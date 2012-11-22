// Kyle Geib - Program 4 - CSS434 Fall 2012 - Dr Fukuda - November 13rd 2012

package main;

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;

// TODO - Fix this bullshit
import main.FileContainer.FileState;


/**DFS Server
 * @author Kyle Geib
 */
public class DFSServer extends UnicastRemoteObject implements ServerInterface {
	
	private Vector<FileContainer> cache;
	
	/**Constructor for DFSServer - Initializes the cache vector.
	 * @throws RemoteException
	 */
	public DFSServer() throws RemoteException {
		cache = new Vector<FileContainer>();
	}
	
	// Download
	public FileContents download(String clientIP, 
			String fileName, String mode) {
		
		// If the cache does not contain a file by that name, create a new one
		if (!cache.contains(fileName)) {
			
			// Create a new File with fileName, the requesting client, and mode
			FileContainer newFile = 
					new FileContainer(fileName, clientIP, mode);
			
			cache.add(newFile);		// Add it to the cache
			return newFile.data;	// return the FileContents
			
		} else if (mode.equals('r')) {	// File exists, requests read
			
			// TODO - The file does exist. A whole other can of worms.
			FileContainer file = cache.get(cache.indexOf(fileName));
			//found.
		} else {	// File exists, requests write
			FileContainer file = cache.get(cache.indexOf(fileName));
		}
		
		return null;
	}
	
	// Upload
	public boolean upload(String clientIP, String fileName, String mode) {
		if (!cache.contains(fileName))
			return false;
		
		FileContainer file = cache.get(cache.indexOf(fileName));
		
		if (file.fileState == FileState.Not_Shared || !file.owner.equals(clientIP))
			return false;
		
		if (file.fileState == FileState.Read_Shared) {
			file.fileState = FileState.Not_Shared;
			return false;
		}
		
		return true;
	}
	
	/**Main function. Starts the RMI services of the DFSServer.
	 * @param args
	 */
	public static void main( String args[] ) {
		if ( args.length != 1 ) {
		    System.err.println( "usage: java DFSServer port#" );
		    System.exit( -1 );
		}
		try {
			DFSServer server = new DFSServer( );
		    Naming.rebind( "rmi://localhost:" + args[0] + "/dfsserver", server );
		} catch ( Exception e ) {
		    e.printStackTrace( );
		    System.exit( 1 );
		}
    }
}
