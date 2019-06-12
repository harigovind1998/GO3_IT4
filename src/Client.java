import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.lang.Math; 
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.io.FileOutputStream; 
import java.io.OutputStream; 
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Client {
	ComFunctions com;
	DatagramSocket sendReceiveSocket;
	private static JFrame frame = new JFrame();
	private static JTextArea area = new JTextArea();
	private static JScrollPane scroll;
	
	public static  Path f2path = Paths.get("./Client/returnTest2.txt");
	
	private static byte[] rrq = {0,1};
	private static byte[] wrq = {0,2};
	private static int mode, simMode, rest;
	private int interHostPort = 23;
	private  boolean TIDSet = false;
	/**
	 * The Client presents the user with a front end GUI that displays data being passed and received to and from the server. The client can either read a file from
	 * the server, making a local copy in the ./Client/ folder or write a file to the server, this will take a file in ./Client/ transfer it to the client that will
	 * create a copy in ./Server/ 
	 */
	public Client(){
		//If we arent simulating, we can bypass the errror simulator and create a connection directly with the server
		if((int)simMode == 1) {
			interHostPort = 69;
		}
		com = new ComFunctions();
		sendReceiveSocket = com.startSocket(); //Socket that is used to send and receive packets from the server/errorSimulator
		try {
			sendReceiveSocket.setSoTimeout(1000); //Setting a socket timeout for receive, used mostly re-send packets
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		//Making a GUI to display passed information
		frame.setSize(420, 440);
		area.setBounds(10, 10, 380, 380);
		scroll = new JScrollPane(area, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scroll.setSize(400, 400);
		
		frame.getContentPane().add(scroll);
		frame.setLayout(null);
		frame.setVisible(true);
	}
	
	/**
	 * Sends a local file to the server, uses a similar logic as the server read method
	 * @param name
	 * @param format
	 */
	public void writeFile(String name, String format) {
		File f = new File("./Client/"+name);
		if(rest == 0) {
			f.setReadable(false);
			f.setWritable(false);
		}
//		f.setWritable(false);
//		f.setReadable(false);
		if(!(f.exists() && !f.isDirectory())) { 
		    if(mode  == 1) {
		    	area.append("File does not exit, Please try again with correct file name\n");
		    }
		    return;
		}
		byte[] msg =com.generateMessage(wrq, name, format);
		DatagramPacket sendPacket = com.createPacket(516);
		DatagramPacket recievePacket = com.createPacket(100);
		

		sendPacket = com.createPacket(msg, interHostPort); //creating the datagram, specifying the destination port and message
		//byte[] fileAsByteArr = com.readFileIntoArray("./Client/"+name);
		byte[] fileAsByteArr = null;
		try{
			//fileAsByteArr = Files.readAllBytes(Paths.get("./Client/" + name));
			fileAsByteArr = Files.readAllBytes(f.toPath());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			if(mode==1) {
		    	area.append("File does not exist, Please select correct file next time...Exiting...\n");
		    }
		    return;
		} catch (AccessDeniedException e) {
			if(mode==1) {
				area.append("File access violation...Exiting...\n");
			}
			return;
		}catch(IOException e) {
			String temp = com.getStackTrace(e);
			if(temp.toLowerCase().contains("space")) {
				if(mode ==1) {
					area.append("Not enough space on disk...Exiting...\n");
				}
			}
			return;
		}
		
		if(fileAsByteArr.length / 512 > 65535) {
			area.append("File too big to transfer...Exiting...\n");
			return;
		}
		
		int blockNum = 0;
		int tries = 0;
		mainLoop:
			while(true){
				//Loop that is in charge of sending/re-sending the data packet
				outterSend:
					while(true) {
						com.sendPacket(sendPacket, sendReceiveSocket); //Send message
						if(mode == 1) {
							com.verboseMode("Sent Packet:", sendPacket, area);
						}

						//Loop that ensures that the incoming AckPackets are correct, if it isn't, it will loop back to listening until the correct Ack is received or the the socket receive times out
						innerSend:
							while(true) {
								try {
									sendReceiveSocket.receive(recievePacket);
								}catch(IOException e ) {
									tries ++;
									if(tries < 4) {
										if(mode ==1) {
											com.verboseMode("Preparing to resend packet:", sendPacket, area);
										}
										break innerSend;
									}else {
										area.append("Connection Lost, terminating client\n");
										break mainLoop;
									}
								}
								tries = 0;
								if(mode == 1) {
									com.verboseMode("Recieved Packet:", recievePacket,area);
								}

								if((int)simMode == 1 && !TIDSet) {
									interHostPort = recievePacket.getPort();
									TIDSet = true;
								}
								
								if(recievePacket.getPort() != interHostPort) {
									msg = com.generateErrMessage(new byte[] {0,5}, "");
									sendPacket = com.createPacket(msg, recievePacket.getPort());

									if(mode == 1) {
										com.verboseMode("Preparing to send Packet to second Server:", sendPacket, area);
									}
									break innerSend;
								}
								msg = com.parseForError(recievePacket);
								int err = com.checkIncomingError(recievePacket);
								if(msg != null) {
									if(msg[3]==4) {
										sendPacket = com.createPacket(msg,interHostPort);
										com.sendPacket(sendPacket, sendReceiveSocket);
										if(mode == 1) {
											com.verboseMode("Sent:", sendPacket, area);
											area.append("Terminating connection.\n");
										}
										break mainLoop;
									}
								}
								if(com.getPacketType(recievePacket)== 4) {
									if(com.CheckAck(recievePacket, blockNum)){
										if(sendPacket.getLength() < 512 && sendPacket.getData()[1] == 3 ){ //Checks to see if the file has come to an end
											area.append("End of File reached!\n");
											break mainLoop;
										}
										blockNum ++;
										msg = com.generateDataPacket(com.intToByte(blockNum), com.getBlock(blockNum, fileAsByteArr));
										sendPacket = com.createPacket(msg, interHostPort);
										break innerSend;
									}else if (blockNum > ByteBuffer.wrap(new byte[] {recievePacket.getData()[2],recievePacket.getData()[3]}).getInt()){
										area.append("Duplicate Block received, continue waiting...\n");
									}else {
										msg  = com.generateErrMessage(new byte[] {0,4},"");
										sendPacket = com.createPacket(msg, interHostPort);
										com.sendPacket(sendPacket, sendReceiveSocket);
										if(mode == 1) {
											area.append("Block received out of order, Terminating...\n");
											com.verboseMode("Sent Packet:", sendPacket,area);
											area.append("Ending Connection.");
										}
										break mainLoop;
									}
								}else if(err!= -1) { //If it an error packet, do things accordingly
										switch (err){
											case 0:
												if(mode ==1) {
													area.append("Terminating due to receiving error packet 0");
												}
												break mainLoop;
											case 1:
												if(mode ==1) {
													area.append("Terminating due to receiving error packet 1");
												}
												break mainLoop;
											case 2:
												if(mode ==1) {
													area.append("Terminating due to receiving error packet 2");
												}
												break mainLoop;
											case 3:
												if(mode ==1) {
													area.append("Terminating due to receiving error packet 3");
												}
												break mainLoop;
											case 4:
												if(mode ==1) {
													area.append("Terminating due to receiving error packet 4");
												}
												break mainLoop;
											case 5:
												if(mode ==1) {
													area.append("Terminating due to receiving error packet 5");
												}
												break mainLoop;
											case 6:
												if(mode ==1) {
													area.append("Terminating due to receiving error packet 6");
												}
												break mainLoop;
										}
								}
//										else if (com.getPacketType(recievePacket)==5){
//									if(recievePacket.getData()[3]==4){
//										area.append("Error Code 4 received. Terminating connection\n");
//										break mainLoop;
//									}else if(recievePacket.getData()[3]==5) {
//										area.append("Connection with original server lost. Terminating connection");
//										break mainLoop;
//									}
//								}
							}
						break outterSend; //DataPacket sent and the right AckReceived hence continues on to the next packet

					}
			}

	}

	
	
	/**
	 * Reads a remote file from the server, uses a similar as the write method in the server
	 * @param name
	 * @param format
	 */
	public void readFile(String name, String format) {
		byte[] msg = com.generateMessage(rrq, name, format);
		byte[] incomingBlock =  new byte[] {0,0,0,0};
		File yourFile = new File("./Client/" + name);
		
		if(yourFile.exists() && !yourFile.isDirectory()) {
			if(mode == 1) {
				area.append("File already exits, cant over write it, please select another file...Exiting...\n");
			}
		    return;
		}
		
		try {
			yourFile.createNewFile();
		} catch (AccessDeniedException e) {
			e.printStackTrace();
			if(mode == 1) {
				area.append("File cannot be created due to Folder restrictions, please acquire access  and  trya gain...Exiting...\n");
			}
			return;
		}catch (IOException f) {
			if(com.getStackTrace(f).toLowerCase().contains("space")) {// && (f.getMessage().toLowerCase().contains("no")||f.getMessage().toLowerCase().contains("not"))) {
			    if(mode==1) {
			    	area.append("Not enough space in on disk to create new file...Exiting...\n");
			    }
			}else {
				if(mode  == 1) {
					area.append(f.getMessage() + "...Exiting...\n");
				}
			}
			return;
		}
		f2path = Paths.get("./Client/" + name);
		DatagramPacket sendPacket = com.createPacket(msg, interHostPort); //creating the datagram, specifying the destination port and message
		
		DatagramPacket recievePacket =  com.createPacket(516);
		byte[] dataReceived = null;
		int blockNum = 1;
		int tries = 0;
		outerloop:
		while(true) {
			com.sendPacket(sendPacket, sendReceiveSocket);
			if (mode == 1) {
				com.verboseMode("Sent", sendPacket, area);
			}
			
			innerLoop:
			while(true) {
				try {
					sendReceiveSocket.receive(recievePacket);
				}catch(Exception e) {
					tries ++;
					if(tries < 4) {
						if(mode ==1) {
							com.verboseMode("Preparing to resend packet:", sendPacket, area);
						}
						break innerLoop;
					}else {
						area.append("Connection Lost, terminating client\n");
						break outerloop;
					}
				}
				tries = 0;
				if (mode == 1) {
					com.verboseMode("Received Packet:", recievePacket, area);
				}
				if((int)simMode == 1 && !TIDSet) {
					interHostPort = recievePacket.getPort();
					TIDSet = true;
				}
				
				if(recievePacket.getPort() != interHostPort) {
					msg = com.generateErrMessage(new byte[] {0,5}, "");
					sendPacket = com.createPacket(msg, recievePacket.getPort());
					if(mode == 1) {
						com.verboseMode("Preparing to send Packet to second Server:", sendPacket, area);
					}
					break innerLoop;
				}
				
				msg = com.parseForError(recievePacket);
				if(msg != null) {
					if(msg[3]==4) {
						sendPacket = com.createPacket(msg,interHostPort);
						com.sendPacket(sendPacket, sendReceiveSocket);
						if(mode == 1) {
							com.verboseMode("Sent Packet:", sendPacket, area);
							area.append("Terminating connection.\n");
						}
						break outerloop;
					}else if(msg[3]==1) {
						
					}
				}
				
				int err = com.checkIncomingError(recievePacket)
;				//messageReceived = recievePacket.getData();
				//Add check  to see if the packet is a data Packet
				incomingBlock[2] =  recievePacket.getData()[2];
				incomingBlock[3] = recievePacket.getData()[3];
				
				if((blockNum == ByteBuffer.wrap(incomingBlock).getInt()) && com.getPacketType(recievePacket) == 3) {
					dataReceived = com.parseBlockData(recievePacket);		
					//com.writeArrayIntoFile(dataReceived, f2path);
					try {
						if(yourFile.exists() && !yourFile.isDirectory()) {
							Files.write(f2path, dataReceived, StandardOpenOption.APPEND);
						}else {
							throw new FileNotFoundException();
						}
						//Files.write();
					} catch (AccessDeniedException e) {
						e.printStackTrace();
						msg = com.generateErrMessage(new byte[] {0,2}, "File cannot be written into due to file restrictions");
						sendPacket = com.createPacket(msg, interHostPort);
						com.sendPacket(sendPacket, sendReceiveSocket);
						if(mode == 1) {
							System.out.println(com.verboseMode("Sending", sendPacket));
					    	System.out.println("Terminating server");
						}
						return;
					}catch(FileNotFoundException file) {
						msg = com.generateErrMessage(new byte[] {0,1}, "File not found");
						sendPacket = com.createPacket(msg, interHostPort);
						com.sendPacket(sendPacket, sendReceiveSocket);
						if(mode == 1) {
							System.out.println(com.verboseMode("Sending", sendPacket));
					    	System.out.println("Terminating server");
						}
						return;
					}catch (IOException f) {
						if(com.getStackTrace(f).toLowerCase().contains("space")) {// && (f.getMessage().toLowerCase().contains("no")||f.getMessage().toLowerCase().contains("not"))) {
							msg =com.generateErrMessage(new byte[] {0,3},"Not enough space");
							sendPacket = com.createPacket(msg, interHostPort);
							com.sendPacket(sendPacket,sendReceiveSocket);
						    if(mode==1) {
						    	System.out.println(com.verboseMode("Sending", sendPacket));
						    	System.out.println("Terminating server");
						    }
						    return;
						}
					}
					msg = com.generateAckMessage(com.intToByte(blockNum));
					sendPacket = com.createPacket(msg, interHostPort);
					blockNum++;
					
					if(recievePacket.getLength()<512){ //Checks for if the Data Packet is the last packet
						com.sendPacket(sendPacket, sendReceiveSocket);
						if(mode == 1) {
							com.verboseMode("Sent", sendPacket,area);
						}
						area.append("End of file reached\n");
						break outerloop; //End of file receive so breaks out of all loops
					}
					
					break innerLoop; //Right packet has been received
				}else if((blockNum< ByteBuffer.wrap(incomingBlock).getInt()) &&com.getPacketType(recievePacket) == 3) { // Missed a block
					msg  = com.generateErrMessage(new byte[] {0,4},"");
					sendPacket = com.createPacket(msg, interHostPort);
					com.sendPacket(sendPacket, sendReceiveSocket);
					if(mode == 1) {
						com.verboseMode("Sent Packet:", sendPacket,area);
						area.append("Ending Connection.");
					}
					break outerloop;
				}else if (com.getPacketType(recievePacket)==3){
					if(mode==1) {
						area.append("Received duplicate Data packet\n");
					}
				}else if(err!= -1) { //If it an error packet, do things accordingly
					switch (err){
						case 0:
							if(mode ==1) {
								area.append("Terminating due to receiving error packet 0");
							}
							break outerloop;
						case 1:
							if(mode ==1) {
								area.append("Terminating due to receiving error packet 1");
							}
							break outerloop;
						case 2:
							if(mode ==1) {
								area.append("Terminating due to receiving error packet 2");
							}
							break outerloop;
						case 3:
							if(mode ==1) {
								area.append("Terminating due to receiving error packet 3");
							}
							break outerloop;
						case 4:
							if(mode ==1) {
								area.append("Terminating due to receiving error packet 4");
							}
							break outerloop;
						case 5:
							if(mode ==1) {
								area.append("Terminating due to receiving error packet 5");
							}
							break outerloop;
						case 6:
							if(mode ==1) {
								area.append("Terminating due to receiving error packet 6");
							}
							break outerloop;
					}
//					if(recievePacket.getData()[2]==0 && recievePacket.getData()[3]==5) {
//						area.append("Terminating due to receiving error packet 5");
//						break outerloop;
//					}else if(recievePacket.getData()[2]==0 && recievePacket.getData()[3]==4) {
//						area.append("Terminating due to receiving error packet  4");
//						break outerloop;
//					}else if(recievePacket.getData()[2]==0 && recievePacket.getData()[3]==3) {
//						
//					}
				}else if(!(com.getPacketType(recievePacket)==5)) {
					msg  = com.generateErrMessage(new byte[] {0,4},"");
					sendPacket = com.createPacket(msg, interHostPort);
					com.sendPacket(sendPacket, sendReceiveSocket);
					if(mode == 1) {
						com.verboseMode("Sent Packet:", sendPacket,area);
					}
					break outerloop;
				}
			}
			
		}
	}
	
	public static void main(String[] args) {
		
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		        if (JOptionPane.showConfirmDialog(frame, 
		            "Are you sure you want to close this window?", "Close Window?", 
		            JOptionPane.YES_NO_OPTION,
		            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
		            System.exit(0);
		        }
		    }
		});
		
		
		Scanner sc = new Scanner(System.in);
		System.out.println("Select Mode : Quiet [0], Verbose [1]");
		mode = sc.nextInt();
		System.out.println("Error Simulation mode? Yes [0], No [1]");
		simMode= sc.nextInt();
		
		System.out.println("Select Operation: Read [0], Write[1]");
		int rwMode = sc.nextInt();
		
		System.out.println("Type in file name with file extension i.e '.txt'");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String fileName = null;
		try {
			fileName = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		Client client = new Client();
		if(rwMode == 0) {
			sc.close();
			client.readFile(fileName, "Ascii");
		}else if (rwMode == 1) {
			System.out.println("deny file read access at start? deny[0], allow[1]");
			rest = sc.nextInt();
			sc.close();
			client.writeFile(fileName, "Ascii");
			
		}

	}
}
