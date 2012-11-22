// Kyle Geib - Program 4 - CSS434 Fall 2012 - Dr Fukuda - November 13rd 2012

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;

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
			// Create a new File with fileName
			FileContainer newFile = 
					new FileContainer(fileName, clientIP, mode);
			
			cache.add(newFile);			// Add it to the cache
			return newFile.getData();	// return the FileContents
		} else {
			// TODO - The file does exist. A whole other can of worms.
			
		}
		
		return null;
	}
	
	// Upload
	public boolean upload(String clientIP, String fileName, String mode) {
		return false;
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
