import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class workerThread extends Thread{
	
	private ComFunctions com;
	private long delay;
	private DatagramSocket sendSocket;
	private DatagramPacket delayedPacket;
	
	public workerThread(DatagramPacket delayedPacket, DatagramSocket sendSocket, long delay) {
		this.delayedPacket = delayedPacket;
		this.sendSocket = sendSocket;
		this.delay = delay;
		
		com = new ComFunctions();
		sendSocket = com.startSocket();
	}
	
	public void run() {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println("System unable to perform delay");
			e.printStackTrace();
		}
		
		com.sendPacket(delayedPacket, sendSocket);
	}
}
