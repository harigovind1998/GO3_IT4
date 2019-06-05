import java.net.DatagramPacket;
import java.net.DatagramSocket;
public class intermediateHostRandomPort extends Thread {
	DatagramPacket serverRecieve;
	DatagramSocket sendSocket;
	int clientPort;
	ComFunctions com;
	int serverPort;
	public intermediateHostRandomPort(DatagramPacket packet, int clientPort, DatagramSocket sendSocket) {
		serverRecieve = packet;
		serverPort = packet.getPort();
		this.clientPort = clientPort;
		this.sendSocket = sendSocket;
		com = new ComFunctions();
	}
	
	public void run() {
		DatagramSocket clientSend = com.startSocket();
		System.out.println("Sending packet to client through random  port");
		DatagramPacket send = com.createPacket(serverRecieve, clientPort);
		com.sendPacket(send, clientSend);
		
		
		DatagramPacket clientRecievePacket = com.recievePacket(clientSend, 516);
		DatagramPacket sendPacket = com.createPacket(clientRecievePacket, serverPort);
		com.sendPacket(sendPacket, sendSocket);
		System.out.println("Sending packet to Server through original Port");
		clientSend.close();
	}

}
