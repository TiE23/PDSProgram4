// Kyle Geib - Program 4 - CSS434 Fall 2012 - Dr Fukuda - November 13rd 2012

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
	
	/**Constructor for DFSClient
	 * @param serverIP DFSServer's IP
	 */
	public DFSClient(String serverIP) throws RemoteException {
		this.serverIP = serverIP;
		this.clientState = ClientState.Invalid;
	}
	
	// Invalidate
	public boolean invalidate() {
		clientState = ClientState.Invalid;
		return true;
	}
	
	// Writeback
	public boolean writeback() {
		return false;
	}
	
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
