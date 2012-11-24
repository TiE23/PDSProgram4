// Kyle Geib - Program 4 - CSS434 Fall 2012 - Dr Fukuda - November 13rd 2012

package main;

import java.io.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

/**DFS Client
 * @author Kyle Geib
 */
public class DFSClient extends UnicastRemoteObject implements ClientInterface {

	/**Enumerator to track the different states of this DFSClient's file.*/
	private enum ClientState {
		Invalid, Read_Shared, Write_Owned, Release_Ownership
	}
	
	private String accountName;			// The client account's name.
	private String clientIP;			// This client's IP name.
	private String serverIP;			// The DFSServer's IP name.	
	
	private ClientState clientState; 	// The current state of this client.
	private FileContents fileContents;	// This client's file contents object.
	private String fileName;			// Name of the file.
	private boolean hasOwnership;		// Is this file owned by this client?
	
	
	/**Constructor for DFSClient
	 * @param accountName The user's name.
	 * @param clientIP The client's own IP name. (ex: "uw1-320-09")
	 * @param serverIP DFSServer's IP. (ex: "uw1-320-16")
	 */
	public DFSClient(String accountName, String clientIP, 
			String serverIP) throws RemoteException {
		
		this.accountName = accountName;
		this.clientIP = clientIP;
		this.serverIP = serverIP;
		
		clientState = ClientState.Invalid;
		fileContents = null;
		fileName = "";
		hasOwnership = false;
		
		// TODO - Work in the user-prompt system.
		//userPrompt();
		
		
		// TODO
		/* Attempting a server.download() call will need to handle 
		 * null returns... which means that it has been suspended and 
		 * will be fulfilled later.
		 * */
	}
	
	
	// Invalidate, an RMI method
	public boolean invalidate() {
		// TODO - I wonder if it'll stay this simple...
		clientState = ClientState.Invalid;
		return true;
	}
	
	
	// Writeback, an RMI method
	public boolean writeback() {
		// TODO
		return false;
	}
	
	
	/**Saves the inputed byte array to "tmp/accountname.txt"
	 * @param data The byte array to write to file
	 * @return Success of the method.
	 */
	private boolean writeToFile(byte[] data) {
		
		cleanFile(); // Clean out the file that may exist or create a new one
		
		try {
			FileOutputStream fos = 
					new FileOutputStream("tmp/" + accountName + ".txt");
			fos.write(data);
			fos.close();
			
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	/**Reads the file "tmp/accountname.txt" and puts it into a raw byte array
	 * @return data in the form of a byte array. Null if there is no file!
	 */
	private byte[] readFromFile() {
		File file = new File("tmp/" + accountName + ".txt");
		
		if (!file.exists())
			return null;	// File doesn't exist!

		try {
			FileInputStream fis = new FileInputStream(file);
			byte[] data = new byte[(int) file.length()];
			
			fis.read(data);	// Read file contents into byte array.
			fis.close();
			
			return data;	// Return the byte array.
			
		} catch (IOException e) { 
			e.printStackTrace(); return null; 
		}
	}
	
	
	/**Function deletes the cached file (if it exists) and re-initializes it.
	 */
	private void cleanFile() {
		
		File dir = new File("tmp");
		File file = new File("tmp/" + accountName + ".txt");
		
		if (file.exists()) // File already exists: delete it.
			file.delete();
		
		try {
			dir.mkdir();			// Create the tmp directory.
			file.createNewFile();	// Re-initialize the file.
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	
	/**Performs the user prompting cycle in the console.
	 */
	private void userPrompt() {
		// TODO - Write the user prompt loop method and handle the user account stuff
	}
	
	
	/**Main function handles the command line invocation.
	 * @param args
	 */
	public static void main(String[] args) {
		if ( args.length != 4 ) {
		    System.err.println("usage: java DFSClient " +
		    		"[YourName] [YourIP] [ServerIP] [port#]");
		    System.exit( -1 );
		}
		try {
			DFSClient client = new DFSClient( args[0], args[1], args[2] );
		    Naming.rebind("rmi://localhost:" + args[3] + "/dfsclient", client);
		} catch ( Exception e ) {
		    e.printStackTrace( );
		    System.exit( 1 );
		}
	}
}
