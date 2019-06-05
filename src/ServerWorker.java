import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.Arrays;

public class ServerWorker extends Thread {
	
	private final int BLOCK_SIZE = 516;
	private DatagramPacket initialPacket, RecievedResponse, SendingResponse;
	private int interHostPort;
	private String fileName;
	private DatagramSocket SendRecieveSocket; 
	private ComFunctions com;
	private int job, mode;

	/**
	 * Gets the name of the file that is being written into or read from
	 */
	private void getFileName() {
		byte[] data = initialPacket.getData();
		int[] secondZero = {3,0,0};
		int track = 1;
		for(int i = 3; i<data.length ; i ++) {
			if(data[i] == 0) {
				secondZero[track] = i;
				track++;
				if (track == 3) {
					break;
				}
			}
		}
		byte[] file = Arrays.copyOfRange(data, 2 , secondZero[1]);
		this.fileName = new String(file);
	}
	
	/**
	 * Decodes the incoming packet to get the necessary information, namely the file name and weather the its a read or write request
	 */
	private boolean decodePacket() {
		if (com.checkRequestFormat(initialPacket.getData())) {
			job = initialPacket.getData()[1]; //format of the message has been checked so second bit will determine if the request is a read or write
			interHostPort = initialPacket.getPort();
			getFileName();
			return true;
		}else {
			DatagramPacket errorPacket = com.createPacket(com.generateErrMessage(new byte[] {0, 4}, ""),initialPacket.getPort());
			if(mode==1) {
				System.out.println(com.verboseMode("Error Packet Recieved as a req:", errorPacket));
			}
			DatagramSocket errorSocket = com.startSocket();
			com.sendPacket(errorPacket, errorSocket);
			errorSocket.close();
			return  false;
		}
		
	}
	
	
	/**
	 * Sends the contents over to the client
	 */
	private void readServe() {

		byte [] fileByteReadArray = com.readFileIntoArray("./Server/" + fileName);
		int blockNum = 1;
		//Keeps looping until it is the entire file has been sent over
		int tries = 0;
		mainLoop:
			while(true){
				byte[] msg = com.generateDataPacket(com.intToByte(blockNum), com.getBlock(blockNum, fileByteReadArray));
				RecievedResponse = com.createPacket(100);
				SendingResponse = com.createPacket(msg, interHostPort);
				//Loop that is in charge of sending/resending the data packet
				outterSend:
					while(true) {
						com.sendPacket(SendingResponse, SendRecieveSocket); //Send message
						if(mode == 1) {
							System.out.println(com.verboseMode("Sent Packet:", SendingResponse));
						}
						//Loop that ensures that the incoming AckPackets are correct, if it isn't, it will loop back to listening until the correct Ack is recieved or the the socket receive times out
						innerSend:
							while(true) {
								try {
									SendRecieveSocket.receive(RecievedResponse);
								} catch (Exception e) {
									tries ++;
									if(tries < 4) {
										if(mode == 1) {
											System.out.println(com.verboseMode("Preparing to resend packet:", SendingResponse));
										}
										break innerSend;
									}else {
										System.out.println("Connection Lost, terminating client\n");
										break mainLoop;
									}
								}
								
								tries = 0;
								if(mode == 1) {
									System.out.println(com.verboseMode("Recieved Packet:", RecievedResponse));
								}
								
								if(RecievedResponse.getPort() != interHostPort) {
									msg = com.generateErrMessage(new byte[] {0,5}, "");
									SendingResponse= com.createPacket(msg, RecievedResponse.getPort());

									if(mode == 1) {
										System.out.println(com.verboseMode("Preparing to send Packet to second Server:", SendingResponse));
									}
									break innerSend;
								}

								msg = com.parseForError(RecievedResponse);

								if(msg != null) {
									if(msg[3]==4) {
										SendingResponse= com.createPacket(msg,interHostPort);
										com.sendPacket(SendingResponse, SendRecieveSocket);
										if(mode == 1) {
											System.out.println(com.verboseMode("Sent Packet:", SendingResponse));
											System.out.println("Terminating connection.\n");
										}
										break mainLoop;
									}
								}

								

								if(com.getPacketType(RecievedResponse)== 4) {
									if(com.CheckAck(RecievedResponse, blockNum)){
										if(SendingResponse.getLength() < 512 && SendingResponse.getData()[1] == 3 ){ //Checks to see if the file has come to an end
											System.out.println("End of File reached!\n");
											break mainLoop;
										}
										
										blockNum ++ ;
										msg = com.generateDataPacket(com.intToByte(blockNum), com.getBlock(blockNum, fileByteReadArray));
										SendingResponse = com.createPacket(msg, interHostPort);
										
										break innerSend;
									}else if (blockNum > ByteBuffer.wrap(new byte[] {RecievedResponse.getData()[2],RecievedResponse.getData()[3]}).getShort()){
										System.out.println("Duplicate Block received, continue waiting...\n");
									}else {
										msg  = com.generateErrMessage(new byte[] {0,4},"");
										SendingResponse = com.createPacket(msg, interHostPort);
										com.sendPacket(SendingResponse, SendRecieveSocket);
										if(mode == 1) {
											System.out.println("Block received out of order, Terminating...");
											System.out.println(com.verboseMode("Sent Packet:", SendingResponse));
											System.out.println("Ending Connection.");
										}
										break mainLoop;
									}
								}else if (com.getPacketType(RecievedResponse)==5){
									if(RecievedResponse.getData()[3]==4){
										System.out.println("Error Code 4 received. Terminating connection\n");
										break mainLoop;
									}else if(RecievedResponse.getData()[3]==5) {
										System.out.println("Connection with original server lost. Terminating connection");
										break mainLoop;
									}
								}
							}


					}
			}
	}
	
	/**
	 * Handles the write request
	 */
	private void writeServe(){
		File yourFile = new File("./Server/" + fileName);
		//If the specified file doesn't exit, it will create it
		try {
			yourFile.createNewFile();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		byte[] msg = null;
		int blockNum = 0;
		byte[] incomingBlock = new byte[2];
		int last;
		RecievedResponse = com.createPacket(BLOCK_SIZE);
		SendingResponse = com.createPacket(com.generateAckMessage(com.intToByte(blockNum)), interHostPort);
		
		blockNum++;
		int tries = 0;
		writeLoop:
		while (true) {//Loop to write the entire file
			mainLoop:
				while(true) { //Loop that sends and resends AckPackets accordingly
					com.sendPacket(SendingResponse, SendRecieveSocket);
					if(mode == 1) {
						System.out.println(com.verboseMode("Sent Packet:", SendingResponse));
					}
					innerLoop:
						while(true) { //Loop that listens for the incoming packet, if the packet is incorrect it keeps listing until the correct one is received 
							try {
								SendRecieveSocket.receive(RecievedResponse);
							}catch (Exception e) {
//								// TODO: handle exception
//								if(mode == 1) {
//									System.out.println(com.verboseMode("Preparing to resend packet:", SendingResponse));
//									break mainLoop;										
//								}
								
								tries ++;
								if(tries < 4) {
									if(mode == 1) {
										System.out.println(com.verboseMode("Preparing to resend packet:", SendingResponse));
									}
									break mainLoop;
								}else {
									System.out.println("Connection Lost, terminating client\n");
									break writeLoop;
								}
							}
							tries  = 0;
							if(mode == 1) {
								System.out.println(com.verboseMode("Recieved Packet:", RecievedResponse));
							}
							
							if(RecievedResponse.getPort() != interHostPort) {
								msg = com.generateErrMessage(new byte[] {0,5}, "");
								SendingResponse = com.createPacket(msg, RecievedResponse.getPort());
								if(mode==1) {
									System.out.println("Packet source TID incorrect");
								}
								break mainLoop;
							}
							
							msg = com.parseForError(RecievedResponse);
							
							if(msg!= null) {
								if(msg[3]==4) { //if the Packet received was the wrong format, send the error to the client and close connection
									SendingResponse = com.createPacket(msg,interHostPort);
									com.sendPacket(SendingResponse, SendRecieveSocket);
									if(mode==1) {
										System.out.println(com.verboseMode("Sending Packet:", SendingResponse));
									}
									break writeLoop;
								}
							}
							
							//Checks to see if the Data Packet received is the correct packet, if it isn't waits for next incoming packet
							incomingBlock[0] = RecievedResponse.getData()[2];
							incomingBlock[1] = RecievedResponse.getData()[3];
							if((blockNum == ByteBuffer.wrap(incomingBlock).getShort()) && com.getPacketType(RecievedResponse) == 3) {
								com.writeArrayIntoFile(com.parseBlockData(RecievedResponse), Paths.get("./Server/" + fileName));
								last = RecievedResponse.getData()[RecievedResponse.getLength() -1];
								msg = com.generateAckMessage(com.intToByte(blockNum));
								SendingResponse = com.createPacket(msg, interHostPort);
								if(RecievedResponse.getLength()<512){ //Checks for if the Data Packet is the last packet
									com.sendPacket(SendingResponse, SendRecieveSocket);
									if(mode == 1) {
										System.out.println(com.verboseMode("Sent", SendingResponse));
									}
									System.out.println("End of file reached");
									break writeLoop; //End of file receive so breaks out of all loops
								}
								++blockNum;
								break innerLoop; //The correct data Packet was received so it leaves the inner loop
							}else if((blockNum< ByteBuffer.wrap(incomingBlock).getShort()) && com.getPacketType(RecievedResponse) == 3) { // Missed a block
								msg  = com.generateErrMessage(new byte[] {0,4},"");
								SendingResponse = com.createPacket(msg, interHostPort);
								com.sendPacket(SendingResponse, SendRecieveSocket);
								if(mode == 1) {
									System.out.println(com.verboseMode("Sent Packet:", SendingResponse));
									System.out.println("Ending Connection.");
								}
								break writeLoop;
							}else if (com.getPacketType(RecievedResponse)==3){
								if(mode==1) {
									System.out.println("Received duplicate Data packet");
								}
							}else if(com.getPacketType(RecievedResponse) == 5) { //If it an error packet, do things accordingly 
								if(RecievedResponse.getData()[2]==0 && RecievedResponse.getData()[3]==5) {
									System.out.println("Terminating due to receiving error packet 5");
									break writeLoop;
								}if(RecievedResponse.getData()[2]==0 && RecievedResponse.getData()[3]==4) {
									System.out.println("Terminating due to receiving error packet  4");
									break writeLoop;
								}
							}else if(!(com.getPacketType(RecievedResponse)==5)) {
								msg  = com.generateErrMessage(new byte[] {0,4},"");
								SendingResponse = com.createPacket(msg, interHostPort);
								com.sendPacket(SendingResponse, SendRecieveSocket);
								if(mode == 1) {
									System.out.println(com.verboseMode("Sent Packet:", SendingResponse));
								}
								break writeLoop;
							}
						}
					break mainLoop; //block was written in to file so the server can send the ack packet and start listening for n+1 packet
					
				}
			
			
		}
	}
	
	/**
	 * decodes and then performs the necessary task
	 */
	public void run() {
		if(decodePacket()) {
			if(job == 1) {
				readServe();
			}else if (job ==2) {
				writeServe();
			}
		}
	}
	
	public ServerWorker(String name, DatagramPacket packet, int mode) {
		// TODO Auto-generated constructor stub
		super(name);
		com = new ComFunctions();
		SendRecieveSocket = com.startSocket();
		try {
			SendRecieveSocket.setSoTimeout(1000);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		initialPacket = packet;
		this.mode = mode;
	}
	
}
