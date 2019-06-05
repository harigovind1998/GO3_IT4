import java.net.DatagramPacket;
import java.net.DatagramSocket;
public class delaySimulator extends Thread{
	private ComFunctions com;
	private DatagramPacket packet;
	private DatagramSocket socket;
	private long delay;
	public delaySimulator(DatagramPacket packet, long delay, DatagramSocket socket, int destPort) {
		com = new ComFunctions();
		this.packet = com.createPacket(packet, destPort);
		this.delay = delay;
		this.socket = socket;
	}
	
	public void run() {
		try{
			//thread sleeps for delay milliseconds then sends the packet to target socket
			Thread.sleep(delay);
			com.sendPacket(packet, socket);
			if(IntermediateHost.mode==1) {
				System.out.println(com.verboseMode("Sent delayed Packet", packet));
			}
		}
		catch(InterruptedException e){
			System.out.println(e);
		}
	}
}
