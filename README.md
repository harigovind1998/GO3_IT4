# TFTP

Iteration 4
*** PLEASE SET THE PREMISSION OF ./Client/AccessViolation and ./Server/AccessViolation folder to have read only permission (I alreay set it but it might change from computer to computer) ***


## Getting Started
These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites
Java (using preferably Eclipse IDE)

## Running
	1. Open 3 different consoles in eclipse.
	   (To switch between read and write, Comment out line 242 and uncomment line 244 in Client.java (this will do a
	   concurrent read and write). And vise versa)
	2. Run the Server; Type 1 for verbose and 0 for silent in the console; type "exit" whenever in the console to end the
	   server
	3. Run the Intermediate Host (YOU CAN BY PASS THIS BY NOT USING IT AND SELECTING 1 WHEN CLIENT ASKS "ERROR SIMULATION MODE"); Type 1 for verbose and 0 for silent in the console and select the approprioate error you
	   want to simulate, 0 if you dont want to simulate any errors (0 for Normal Operation; 1 for Losing Packet; 2 for
	   Delaying Packet; 3 for Duplicating Packet; 4 for Damaging Packet; 5 to damage WRQ/RRQ packets, 6 to damage ACK/DATA Packets; 7 to lose last ACK PACKET; 8 to delete files in the middle of a transfer; 9 to change file access in the middle of transfer) READ NoteOnErrorSimulation.txt for delailed info on error packet 1,2,3 and 6
	4. Run the Client; Type 1 for verbose and 0 for silent in the console
	5. Switch each Console Window to show the Client, Intermediate Host, and Server Individually
	6. View results in External Window (i.e. JFrame)
	7. If you want to rerun it, ensure that only the original files are used

### Testing concurrent File transfers
	1. This one needs a little modification from you. Delete all the files that were created from the previous file
	   transfer tests (./Client should only have writeTest.txt and ./Server should only have readText.txt).
	2. Start the server, intermediateHost following instructions under Running
	3. Start the client but do not choose the verbose yet
	4. Modify the interHostPort in the intermediateHost.java and client.java to another number that not 23 BUT the new port
	   chosen must both be the same and cannot be 23 or 69
	5. Comment out line 242 and uncomment line 244 in Client.java (this will do a concurrent read and write)
	6. Start the modified intermediateHost with the changed port and choose verbose or non verbose
	7. Start the modified client
	8. Get both the client instances to be on seperate consoles and select verbose or non verbose for both

## Files
	1. Client.java; Contains the code that controls the client portal 
	2. ComFunctions.java; Contains common functions that are shared between the client, server and the
	   IntermediateHost (i.e. error simulator)
	3. delaySimulator.jav; used to delay a target packet my n milliseonds, used in error simulation
	4. IntermediateHost.java; Error Simulator, for now it just passed the messages between the serverWorkerThread/Server
	   and Client
	5. Server.java; Handles all the in initial incoming requests, then passes the job on to a sperate thread
 	6. ServerExitListener.java; thread that listens for exit command in the console
 	7. ServerWorker.java; Handles the reads and writes
	8. intermediateHostRandomPort.java: Simulates random port for the Server
	9. ./Client/writeTest.txt; sample file used for WRQ
 	10. ./Server/readTest.txt; sample file used for RRQ
	11. ./Client/sherlock.txt; sample file that can we used for WRQ that is extremely large 
    12. ./Client/big.txt; sample file that can we used for WRQ that is large 
    13. ./Client/five.txt; sample file that can we used for WRQ that is exactly 512 bytes long
    14. ./Client/AccessViolation/writeTest.txt; file used to test access violation into a folder in server with read only permission
    15. ./Server/AccessViolation/readTest.txt; file used to test access violation into a folder in server with write only permission
## Testing
	1. Run the server, select verbose or not  
	2. **optional** if you want to use the error Simulator, run IntermediateHost and select the error youd like to simulate
	3. Run the client select 1 if you would like to bypass the intermediate host and 1 if you want to simulat errors (if you want to, ensure that the intermediate host is already running)
	4. input writeTest.txt if you are testing write and readTest.txt if you are testing read 
	5. after every simulation delete the file that was just tansfered ./Client should only have writeTest.txt and ./Server should only have readTest.txt
	6. you must restart the intermediateHost and client after every test

## Built With
	* Java
	* Eclipse

## Authors
	* Hariprasadh Ravichandran Govindasamy
	* Tarun Kalikivaya
	* Yohannes Kussia
	* Steven Zhou
