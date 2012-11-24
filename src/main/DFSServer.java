// Kyle Geib - Program 4 - CSS434 Fall 2012 - Dr Fukuda - November 13rd 2012

package main;

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.Queue;
import java.util.Vector;
import main.FileContainer.FileState;


/**DFS Server
 * @author Kyle Geib
 */
public class DFSServer extends UnicastRemoteObject implements ServerInterface {
	
	/**Private inner class allows the DFS Server to track a client's most
	 * recent file to further inform file invalidations with FileContainers.
	 * Additionally, it can also help track suspended downloads.*/
	private class ClientContainer {
		public String clientIP;
		public String fileName;

		public ClientContainer(String clientIP, String currentFileName) {
			this.clientIP = clientIP;
			this.fileName = currentFileName;
		}
	}
	
	private Vector<ClientContainer> jobQueueWS;
	private Queue<ClientContainer> jobQueueOC;
	
	private Vector<ClientContainer> clientList;
	private Vector<FileContainer> cache;
	private String port;
	
	
	/**Constructor for DFSServer - Initializes the cache vector.
	 * @throws RemoteException
	 */
	public DFSServer(String port) throws RemoteException {
		cache = new Vector<FileContainer>();
		this.port = port;
	}
	
	
	// RMI Download function.
	public FileContents download(String clientIP, 
			String fileName, String mode) {
		
		// Check to see if this is a recognized clientIP.
		if (0 < vectorCCSearch(clientList, clientIP)) {
			// It isn't, so add the client and its file.
			clientList.add(new ClientContainer(clientIP, fileName));
		} else { // Recognized Client, update its latest file.
			clientList.elementAt(vectorCCSearch(clientList, clientIP)).fileName = fileName;
		}
		
		// Check to see if this is a recognized fileName.
		if (0 < vectorFCSearch(cache, fileName)) {
			
			// Create a new File with fileName, the requesting client, and mode
			FileContainer newFile = 
					new FileContainer(fileName, clientIP, mode);
			
			cache.add(newFile);		// Add it to the cache
			return newFile.data;	// return the FileContents
			
		} else if (mode.equals('r')) {	// File exists, requests READ!
			
			// Get the requested file...
			FileContainer file = cache.elementAt(vectorFCSearch(cache, fileName));
			
			if (file.fileState == FileState.Not_Shared) {
				
				// Add the client to the Readers list.
				file.safeAddReader(clientIP);
				file.fileState = FileState.Read_Shared;	// Next state.
				
				return file.data;
				
			} else if (file.fileState == FileState.Read_Shared) {
				
				// Add the client to the Readers list.
				file.safeAddReader(clientIP);
				// No state change.
				
				return file.data;
				
			} else if (file.fileState == FileState.Write_Shared) {
				
				// Add the client to the Readers list.
				file.safeAddReader(clientIP);
				// No state change.
				
				return file.data;
				
			} else if (file.fileState == FileState.Ownership_Change) {
				
				// Add the client to the Readers list.
				file.safeAddReader(clientIP);
				// No state change.
				
				return file.data;
			}
			
			
		} else if (mode.equals('w')){	// File exists, requests WRITE!
			
			// Get the requested file...
			FileContainer file = cache.elementAt(vectorFCSearch(cache, fileName));
			
			if (file.fileState == FileState.Not_Shared) {
				
				// Add the client to the Readers list.
				file.owner = clientIP;
				file.fileState = FileState.Write_Shared;	// Next state.
				
				return file.data;
				
			} else if (file.fileState == FileState.Read_Shared) {
				
				// Add the client to the Readers list.
				file.owner = clientIP;
				file.fileState = FileState.Write_Shared;	// Next state.
				
				return file.data;
				
			} else if (file.fileState == FileState.Write_Shared) {
				// TODO
				/*Call the current owner's writeback() function, 
				 * and thereafter suspends this download() function.*/
			} else if (file.fileState == FileState.Ownership_Change) {
				// TODO
				/*Immediately suspend this download() function call.*/
			}
		}
		
		return null;
	}
	
	
	// RMI Upload function.
	public boolean upload(String clientIP, String fileName, FileContents data){
		
		if (0 < vectorFCSearch(cache, fileName))
			return false;	// This file isn't available!
		
		FileContainer file = cache.elementAt(vectorFCSearch(cache, fileName));
		
		if (!file.owner.equals(clientIP)) {
			
			return false;	// This client isn't the owner!
		
		} else if (file.fileState == FileState.Not_Shared) {
			
			return false;	// Unacceptable FileState!
		
		} else if (file.fileState == FileState.Read_Shared) {
			
			file.fileState = FileState.Not_Shared;	// Next state.
			return false;	// Unacceptable FileState!
			
		} else if (file.fileState == FileState.Write_Shared) {
			
			try {
				invalidateAll(file);	// Invalidate all readers.
			} catch (Exception e) { e.printStackTrace(); }
			
			file.data = data;	// Update file's contents.
			file.fileState = FileState.Not_Shared;	// Next state.
			
			return true;
			
		} else if (file.fileState == FileState.Ownership_Change) {
			// TODO
			/* Update the entry data with the given fileContents.
			 * Invalidate a copy of each client registered in the readers list.
			 * Resume download() that has been suspended in Write_Shared. In
			 * other words, register the resumed client to the owner field.
			 * Return a FileContents object to this client.
			 * (Just before the return, resume one of the download() calls
			 * that has been suspended in Ownership_Change.*/
			
			try {
				invalidateAll(file);	// Invalidate all readers.
			} catch (Exception e) { e.printStackTrace(); }
			
			file.data = data;	// Update file's contents.
			file.fileState = FileState.Write_Shared;	// Next state.
			
			return true;
		}
		
		return false;
	}
	
	
	/**Goes through reader vector and makes invalidation calls on all of them.
	 * Then removes the contents of the reader vector.
	 * @param file FileContainer that needs to have all readers invalidated.
	 * @throws Exception
	 */
	private void invalidateAll(FileContainer file) throws Exception {
		
		for (int x = 0; x < file.readers.size(); ++x) {
			String clientName = file.readers.elementAt(x);
			try {
				
				/* This statement is ugly. It finds a known client and checks
				 * to see if it's last used file matches the one we're seeking
				 * to invalidate on client machines. */
				if ( clientList.elementAt(
						vectorCCSearch(clientList, clientName)).
						fileName.equals(file.fileName) ) {
					
					ClientInterface client = (ClientInterface)
							Naming.lookup("rmi://" + clientName + 
									":" + port + "/dfsclient");
					
					client.invalidate();	// Invalidate!
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				break;	// The client did not exist.
			}
		}
		
		// With all of the readers now gone
		file.readers.clear();
	}
	
	private boolean suspendJobWS(String clientIP, String fileName) {
		// TODO
		/*This will take a clientIP and its fileName that needs to download
		 * and put it into the WS vector where resumes are specific.*/
		return false;
	}
	
	private boolean suspendJobOC(String clientIP, String fileName) {
		// TODO
		/*This will take a clientIP and its fileName that needs to download
		 * and put it into the OC queue where resumes are simply performed
		 * in the order received.*/
		return false;
	}
	
	private boolean resumeJobWS(String fileName) {
		// TODO
		/*This will take a clientIP and its fileName that needs to download
		 * and resume it with a call to download(clientIP, fileName, "w"). It
		 * tracks the jobs by the file name requested, [so it will need to
		 * repeat search of the jobQueueWS until all jobs for the particluar
		 * file are resumed.]***
		 * *** I don't know if this is true yet! */
		return false;
	}
	
	private boolean resumeJobOC() {
		// TODO
		/*Resumes a waiting OC job. Simply uses the ClientContainer's contents
		 * to resume a download(clientIP, fileName, "w") call.*/
		
		/*
		 * WELL FUCK I JUST MANAGED TO REALIZE THAT WE COMMUNICATE WITH RETURN
		 * VALUES WITH DOWNLOAD, SO CALLING DOWNLOAD LOCALLY WILL DO JACK 
		 * SQUAT. NEED TO RETHINK THIS... MAYBE WE MAKE THE CLIENT RECALL 
		 * DOWNLOAD? NOT SURE. IT MAY NOT BE ABLE TO COMPLETE SUCH A REQUEST
		 * AT RANDOM? WELL, IT CAN COMPLETE A REQUEST AT RANDOM WHILE 
		 * EDITING IN EMACS WITH INVALIDATE AND WRITEBACK, SO MAYBE IT'LL WORK 
		 * OKAY... OKAY, SO WE'LL MAKE A NEW FUNCTION IN THE CLIENTINTERFACE 
		 * CALLED "NOTIFY" OR SOMETHING...
		 * */
		return false;
	}
	
	
	/**Manual search through a vector for a matching fileContainer name.
	 * @param vector The vector to search through.
	 * @param name The target name that we're searching for.
	 * @return The first element location of the result. -1 if missing.
	 */
	private int vectorFCSearch(Vector<FileContainer> vector, String name) {
		for (int x = 0; x < vector.size(); ++x) {
			if (vector.elementAt(x).fileName.equals(name))
				return x;
		}
		return -1;
	}
	
	
	/**Manual search through a vector for a matching client name.
	 * @param vector The vector to search through.
	 * @param name The target name that we're searching for.
	 * @return The first element location of the result. -1 if missing.
	 */
	private int vectorCCSearch(Vector<ClientContainer> vector, String name) {
		for (int x = 0; x < vector.size(); ++x) {
			if (vector.elementAt(x).clientIP.equals(name))
				return x;
		}
		return -1;
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
			DFSServer server = new DFSServer( args[0] );
		    Naming.rebind("rmi://localhost:" + args[0] + "/dfsserver", server);
		} catch ( Exception e ) {
		    e.printStackTrace( );
		    System.exit( 1 );
		}
    }
}
