// Kyle Geib - Program 4 - CSS434 Fall 2012 - Dr Fukuda - November 13rd 2012

package main;

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
	
	private ClientState clientState; 	// The current state of this client.
	private String serverIP;			// The DFSServer's IP name.
	private FileContents fileContents;	// This client's file contents object.
	private String accountName;			// The client account's name.
	
	/**Constructor for DFSClient
	 * @param serverIP DFSServer's IP
	 */
	public DFSClient(String serverIP) throws RemoteException {
		this.serverIP = serverIP;
		this.clientState = ClientState.Invalid;
	}
	
	// Invalidate, an RMI method
	public boolean invalidate() {
		clientState = ClientState.Invalid;
		return true;
	}
	
	// Writeback, an RMI method
	public boolean writeback() {
		return false;
	}
	
	/**Saves the inputed byte array to "tmp/accountname.txt"
	 * @param data The byte array to write to file
	 */
	private void writeToFile(byte[] data) {
		// TODO - Look up the semantics for .txt files and other data stream stuff
	}
	
	/**Reads the file "tmp/accountname.txt" and puts it into a raw byte array
	 * @return data in the form of a byte array
	 */
	private byte[] readFromFile() {
		
		// TODO - Look up the semantics for reading from .txt files to byte arrays
		return null;
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
		if ( args.length != 2 ) {
		    System.err.println( "usage: java DFSClient ServerIP port#" );
		    System.exit( -1 );
		}
		try {
			DFSClient client = new DFSClient( args[0] );
		    Naming.rebind( "rmi://localhost:" + args[1] + "/dfsclient", client );
		} catch ( Exception e ) {
		    e.printStackTrace( );
		    System.exit( 1 );
		}
	}
}
