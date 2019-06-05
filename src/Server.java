import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Scanner;


public class Server {
	private final int REQUEST_SIZE = 100;
	DatagramSocket recieveSocket, errorSocket;
	DatagramPacket recievePacket, errorPacket;
	ComFunctions com;
	int mode;
	public static boolean serverOn = true;
	/**
	 * loops and keeps serving all the incoming requests
	 */
	public void serve() {
		while(serverOn) {
			recievePacket = com.recievePacket(recieveSocket, REQUEST_SIZE); 
			//if the received packet is valid, passes the message onto a worker thread that takes care of all request until it is complete 
			if(mode == 1) {
				System.out.println(com.verboseMode("Main server Recieved", recievePacket));
			}
			ServerWorker worker = new ServerWorker(Integer.toString((recievePacket.getPort())), recievePacket,mode);
			worker.start();
		}
	}
	
	/**
	 * Initializes the server at the start
	 */
	public Server() {
		// TODO Auto-generated constructor stub
		Scanner sc = new Scanner(System.in);
		System.out.println("Select Mode : Quiet [0], Verbose [1]");
		mode = sc.nextInt();
		com = new ComFunctions();
		recieveSocket = com.startSocket(69);
		ServerExitListener exitListener = new ServerExitListener("Exit listener");
		exitListener.start();
	}

	
	public static void main(String[] args) {
		Server server = new Server();
		server.serve();
	}
}

