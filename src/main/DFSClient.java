// Kyle Geib - Program 4 - CSS434 Fall 2012 - Dr Fukuda - November 13rd 2012

package main;

import java.io.*;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

/**DFS Client
 * @author Kyle Geib
 */
public class DFSClient extends UnicastRemoteObject implements ClientInterface {

	/**Enum to track the different states of this DFSClient's file.*/
	private enum ClientState {
		Invalid, Read_Shared, Write_Owned, Release_Ownership
	}
	
	/**Enum to track the access mode of the current file: read or write.*/
	private enum AccessMode {
		Read, Write, Undefined
	}
	
	private String accountName;			// The client account's name.
	private String clientIP;			// This client's IP name.
	private String serverIP;			// The DFSServer's IP name.
	private String port;				// The port used.
	
	private ClientState clientState; 	// The current state of this client.
	private AccessMode accessMode;		// The current file access mode (r/w).
	
	private FileContents fileContents;	// This client's file contents object.
	private String fileName;			// Name of the file.
	private boolean hasOwnership;		// Is this file owned by this client?
	
	
	/**Constructor for DFSClient
	 * @param accountName The user's name.
	 * @param clientIP The client's own IP name. (ex: "uw1-320-09")
	 * @param serverIP DFSServer's IP. (ex: "uw1-320-16")
	 * @param port The port used for communication between client and server.
	 */
	public DFSClient(String accountName, String clientIP, 
			String serverIP, String port) throws RemoteException {
		
		this.accountName = accountName;
		this.clientIP = clientIP;
		this.serverIP = serverIP;
		this.port = port;
		
		clientState = ClientState.Invalid;
		accessMode = AccessMode.Undefined;
		
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
		// Likely to require chmod use.
		clientState = ClientState.Invalid;
		return true;
	}
	
	
	// Writeback, an RMI method
	public boolean writeback() {
		if (clientState == ClientState.Write_Owned) {
			clientState = ClientState.Release_Ownership;
			return true;	// Change state. Return successful result.
		} else
			return false;	// Undefined result.
	}
	
	
	// Resume, an RMI method.
	public void resume(String fileName) {
		/* TODO - fileName is checked against the current file in the case 
		 * that the user was impatient and decided to download something else 
		 * instead of waiting.*/
		
		if (pullFile(fileName, "w")) {	// Successfully gained file.
			
			// Update information (pullFile only updates this.fileContents).
			accessMode = AccessMode.Write;
			clientState = ClientState.Write_Owned;
			this.fileName = fileName;
			hasOwnership = true;
		}
	}
	
	
	/**Performs the user prompting cycle in the console.
	 */
	private void userPrompt() {
		// TODO - Write the user prompt loop method and handle the user account stuff
	}
	
	
	/**Pull performs a download on a requested file from the server.
	 * NOTE: Performs no other tasks other than to update fileContents!!!
	 * @param fileName
	 * @param mode "r" for read. "w" for write.
	 * @return Success of the download. False means no change: likely suspended
	 */
	private boolean pullFile(String fileName, String mode) {
		try {
			ServerInterface server = (ServerInterface) 
					Naming.lookup("rmi://" + serverIP + 
							":" + port + "/dfsserver");
			
			FileContents returnFile = server.download(clientIP, fileName, mode); 
			
			if (returnFile == null)
				return false;
			else {
				fileContents = returnFile;	// Only changes fileContents!
				return true;
			}
		} catch (Exception e) {e.printStackTrace(); return false;}
	}
	
	
	/**Push performs an upload of the client's current file.
	 * @return Success of the upload operation.
	 */
	private boolean pushFile() {
		try {
			ServerInterface server = (ServerInterface) 
					Naming.lookup("rmi://" + serverIP + 
							":" + port + "/dfsserver");
			
			// Upload the contents of this client's file.
			return server.upload(clientIP, fileName, fileContents);
			
		} catch (Exception e) {e.printStackTrace(); return false;}
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
			
		} catch (IOException e) { e.printStackTrace(); return null; }
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
			DFSClient client=new DFSClient(args[0], args[1], args[2], args[3]);
		    Naming.rebind("rmi://localhost:" + args[3] + "/dfsclient", client);
		} catch ( Exception e ) {
		    e.printStackTrace( );
		    System.exit( 1 );
		}
	}
}
