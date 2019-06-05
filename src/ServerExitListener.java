import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
public class ServerExitListener extends Thread {
	
	public ServerExitListener(String name) {
		super(name);
	}
	
	/**
	 * Thread that waits for exit to be input into the console, exit is input, turns off the server
	 */
	public void run(){
		
		String cmd = "";
		BufferedReader input = null;
		
		input = new BufferedReader(new InputStreamReader(System.in));
		try {
			cmd = input.readLine();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		if(cmd.equals("exit")) {
			System.out.println("Server shutting down");
			Server.serverOn = false;
		}
	}
}
