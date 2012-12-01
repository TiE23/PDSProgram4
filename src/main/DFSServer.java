// Kyle Geib - Program 4 - CSS434 Fall 2012 - Dr Fukuda - December 13th 2012

package main;

import java.rmi.*;
import java.rmi.server.*;
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

		public ClientContainer(String clientIP, String fileName) {
			this.clientIP = clientIP;
			this.fileName = fileName;
		}
		
		public void print(String message) {
			System.out.println(message + "--> " + clientIP + ": " + fileName);
		}
	}
	
	private Vector<ClientContainer> jobQueueWS;
	private Vector<ClientContainer> jobQueueOC;
	
	private Vector<ClientContainer> clientList;
	private Vector<FileContainer> cache;
	private String port;
	
	
	/**Constructor for DFSServer - Initializes the cache vector.
	 * @throws RemoteException
	 */
	public DFSServer(String port) throws RemoteException {
		jobQueueWS = new Vector<ClientContainer>();
		jobQueueOC = new Vector<ClientContainer>();
		
		clientList = new Vector<ClientContainer>();
		cache = new Vector<FileContainer>();
		
		this.port = port;
	}
	
	
	// RMI Download function.
	public FileContents download(String clientIP, 
			String fileName, String mode) {
		
		System.out.println("DOWNLOAD: " + clientIP + ", " + fileName + ", " + mode);
		
		// Check to see if this is a recognized clientIP.
		int clientIndex = vectorCCcnSearch(clientList, clientIP);
		if (clientIndex == -1) {
			// It isn't, so add the client and its file.
			System.out.println("Adding client " + clientIP);
			clientList.add(new ClientContainer(clientIP, fileName));
		} else { // Recognized Client, update its latest file.
			System.out.println("Updating client " + clientIP + " with " + fileName);
			clientList.elementAt(clientIndex).fileName = fileName;
		}
		
		// Check to see if this is a recognized fileName.
		int fileIndex = vectorFCSearch(cache, fileName);
		
		if (fileIndex == -1) {	// File doesn't exist!
			System.out.println("File " + fileName + " being added");
			// Create a new File with fileName, the requesting client, and mode
			FileContainer newFile = 
					new FileContainer(fileName, clientIP, mode);
			
			cache.add(newFile);		// Add it to the cache
			return newFile.data;	// return the FileContents
			
		} else {
			if (mode.equals('r')) {	// File exists, requests READ!
		
				System.out.println("Recognized file, read");
				// Get the requested file...
				FileContainer file = cache.elementAt(fileIndex);
				
				switch (file.fileState) { 
				
				case Not_Shared:
					// Add the client to the Readers list.
					file.safeAddReader(clientIP);
					file.fileState = FileState.Read_Shared;	// Next state.
					System.out.println("RNS");
					return file.data;
					
				case Read_Shared:
					// Add the client to the Readers list.
					file.safeAddReader(clientIP);
					// No state change.
					System.out.println("RRS");
					return file.data;
					
				case Write_Shared:
					// Add the client to the Readers list.
					file.safeAddReader(clientIP);
					// No state change.
					System.out.println("RWS");
					return file.data;
					
				case Ownership_Change:
					// Add the client to the Readers list.
					file.safeAddReader(clientIP);
					// No state change.
					System.out.println("ROC");
					return file.data;
				}
			
			
			} else {				// File exists, requests WRITE!
				System.out.println("Recognized file, write");
				// Get the requested file...
				FileContainer file = cache.elementAt(fileIndex);
				
				switch (file.fileState) { 
				
				case Not_Shared:
					// Add the client as the owner.
					file.owner = clientIP;
					file.fileState = FileState.Write_Shared;	// Next state.
					System.out.println("WNS");
					return file.data;
					
				case Read_Shared:
					// Add the client as the owner.
					file.owner = clientIP;
					file.fileState = FileState.Write_Shared;	// Next state.
					System.out.println("WRS");
					return file.data;
					
				case Write_Shared:
					/*Call the current owner's writeback() function, 
					 * and thereafter suspends this download() function.*/
					
					callWriteback(fileName);			// Make writeback call.
					suspendJobWS(clientIP, fileName);	// Suspend download().
					System.out.println("WWS");
					return null;
					
				case Ownership_Change:
					/*Immediately suspend this download() function call.*/
					suspendJobOC(clientIP, fileName);	// Suspend download().
					System.out.println("WOC");
					return null;
				}
			}
		}
		
		return null;
	}
	
	
	// RMI Upload function.
	public boolean upload(String clientIP, String fileName, FileContents data){
		
		System.out.println("UPLOAD: " + clientIP + ", " + fileName);
		
		int fileIndex = vectorFCSearch(cache, fileName);
		if (fileIndex == -1) {
			System.out.println("Upload - unrecognized file");
			return false;	// This file isn't available!
		}
		
		FileContainer file = cache.elementAt(fileIndex);
		
		if (!file.owner.equals(clientIP)) {
			System.out.println("client isn't the owner!");
			return false;	// This client isn't the owner!
		}
			
		switch (file.fileState) { 
		
		case Not_Shared:
			System.out.println("NS");
			return false;	// Unacceptable FileState!
		
		case Read_Shared:
			file.fileState = FileState.Not_Shared;	// Next state.
			System.out.println("RS");
			return false;	// Unacceptable FileState!
			
		case Write_Shared:
			file.fileState = FileState.Not_Shared;	// Next state.
			
			invalidateAll(file);	// Invalidate all readers.
			file.data = data;	// Update file's contents.
			System.out.println("WS");
			return true;
			
		case Ownership_Change:
			file.fileState = FileState.Write_Shared;	// Next state.
			file.data = data;	// Update file's contents.
			
			invalidateAll(file);	// Invalidate all readers.
			
			file.owner = clientIP;	// Update owner.
			
			// Resume download suspended in Write_Shared.
			resumeJobWS(fileName);	
			
			// Resume download suspended in Ownership_Change.
			resumeJobOC(fileName);	
			System.out.println("OC");
			return true;
		}
		return false;
	}
	
	
	/**Goes through reader vector and makes invalidation calls on all of them.
	 * Then removes the contents of the reader vector.
	 * @param file FileContainer that needs to have all readers invalidated.
	 * @throws Exception
	 */
	private void invalidateAll(FileContainer file) {
		System.out.println("invalidateAll(" + file.fileName + ")");
		for (int x = 0; x < file.readers.size(); ++x) {
			String clientName = file.readers.elementAt(x);
			try {
				
				/* This statement is ugly. It finds a known client and checks
				 * to see if it's last used file matches the one we're seeking
				 * to invalidate on client machines. */
				if ( clientList.elementAt(
						vectorCCcnSearch(clientList, clientName)).
						fileName.equals(file.fileName) ) {
					
					try {
						ClientInterface client = (ClientInterface)
									Naming.lookup("rmi://" + clientName +
											":" + port + "/dfsclient");
						System.out.println("Invalidating " + clientName);
						client.invalidate();	// Invalidate!
						
					} catch (Exception e) {e.printStackTrace();} 
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				break;	// The client did not exist.
			}
		}
		
		// With all of the readers now gone
		file.readers.clear();
	}
	
	
	/**Requests a writeback on a file.
	 * @param fileName The file that needs a writeback.
	 * @return If the writeback was successful.
	 */
	private boolean callWriteback(String fileName) {
		System.out.println("callWriteback(" + fileName + ")");
		int fileIndex = vectorFCSearch(cache, fileName);
		if ( fileIndex == -1 )
			return false;
		
		String owner = cache.elementAt(fileIndex).owner;
		
		if (!owner.equals("")) {	// Make sure this file has a listed owner.
			try {
			ClientInterface client = (ClientInterface)
					Naming.lookup("rmi://" + owner +
							":" + port + "/dfsclient");
			
			return client.writeback();	// Call writeback!
			
			} catch (Exception e) {e.printStackTrace(); return false;}
			
		} else						// File has no owner!
			return false;
	}
	
	
	/**Waiting for a writeback, download request jobs are placed into a vector
	 * where the two details needed (client name and file name) will be held
	 * until a file has been updated and thus can be downloaded.
	 * This function is for Write_Shared suspensions. (Therefore, "WS").
	 * @param clientIP
	 * @param fileName
	 * @return
	 */
	private boolean suspendJobWS(String clientIP, String fileName) {
		System.out.println("Suspending WS job for " + clientIP + " using " + fileName);
		
		int index = vectorCCcnSearch(jobQueueWS, clientIP);
		if (index != 1) // Client already has a job waiting on this list
			jobQueueWS.remove(index);	// Remove the job (to replace it)
		
		
		// Add to job queue.
		jobQueueWS.add(new ClientContainer(clientIP, fileName));
		
		return true;
	}
	
	
	/**Waiting for a writeback, download request jobs are placed into a vector
	 * where the two details needed (client name and file name) will be held
	 * until a file has been updated and thus can be downloaded.
	 * This function is for Ownership_Change suspensions. (Therefore, "OC").
	 * @param clientIP
	 * @param fileName
	 * @return
	 */
	private boolean suspendJobOC(String clientIP, String fileName) {
		System.out.println("Suspending OC job for " + clientIP + " using " + fileName);
		
		int index = vectorCCcnSearch(jobQueueOC, clientIP);
		if (index != 1) // Client already has a job waiting on this list
			jobQueueOC.remove(index);	// Remove the job (to replace it)
		
		// Add to job queue.
		jobQueueOC.add(new ClientContainer(clientIP, fileName));
		
		return true;
	}
	
	
	/**Notifies a client that their file download can be fulfilled now.
	 * @param fileName The file to notify its requesters of.
	 * @return
	 */
	private boolean resumeJobWS(String fileName) {

		int index = vectorCCfnSearch(jobQueueWS, fileName);
		
		if (index == -1 ) {
			return false;	// No such job exists! (This is bad!)
			
		} else {
			
			ClientContainer temp = jobQueueWS.get(index);
			
			try {
				ClientInterface client = (ClientInterface)
							Naming.lookup("rmi://" + temp.clientIP +
									":" + port + "/dfsclient");
				
				client.resume(temp.fileName);	// Notify client to try again.
				
			} catch (Exception e) {e.printStackTrace(); return false;} 
			
			jobQueueWS.remove(index);	// Dequeue the job.
			return true;
		}
	}
	
	
	/**Notifies a client to try their write request download again.
	 * As a result they may be the first in line for a request or simply 
	 * one space closer in the line to gaining write control to this file.
	 * @param fileName The file to notify its requesters of.
	 * @return
	 */
	private boolean resumeJobOC(String fileName) {
		
		int index = vectorCCfnSearch(jobQueueOC, fileName);
		
		if (index == -1) {
			return false;	// No such job exists! (This is bad!)
			
		} else {
			
			ClientContainer temp = jobQueueOC.get(index);
			
			try {
				ClientInterface client = (ClientInterface)
							Naming.lookup("rmi://" + temp.clientIP +
									":" + port + "/dfsclient");
				
				client.resume(temp.fileName);	// Notify client to try again.
				
			} catch (Exception e) {e.printStackTrace(); return false;} 
			
			jobQueueOC.remove(index);	// Dequeue the job.
			return true;
		}
	}
	
	
	/**Manual search through a vector for a matching fileContainer name.
	 * @param vector The vector to search through.
	 * @param name The target name that we're searching for.
	 * @return The first element location of the result. -1 if missing.
	 */
	private int vectorFCSearch(Vector<FileContainer> vector, String name) {
		for (int x = 0; x < vector.size(); ++x) {
			/////vector.elementAt(x).print("FCSearch #" + x); 	/////////debug
			if (vector.elementAt(x).fileName.equals(name))
				return x;
		}
		return -1;
	}
	
	
	/**Manual search through a vector for a matching client name.
	 * @param vector The vector to search through.
	 * @param name The target clientName that we're searching for.
	 * @return The first element location of the result. -1 if missing.
	 */
	private int vectorCCcnSearch(Vector<ClientContainer> vector, 
			String clientName) {
		for (int x = 0; x < vector.size(); ++x) {
			/////vector.elementAt(x).print("CCSearch #" + x);	/////////debug
			if (vector.elementAt(x).clientIP.equals(clientName))
				return x;
		}
		return -1;
	}
	
	
	/**Manual search through a vector for a matching file name.
	 * @param vector The vector to search through.
	 * @param name The target file name that we're searching for.
	 * @return The first element location of the result. -1 if missing.
	 */
	private int vectorCCfnSearch(Vector<ClientContainer> vector, 
			String fileName) {
		for (int x = 0; x < vector.size(); ++x) {
			/////vector.elementAt(x).print("FJSearch #" + x);	/////////debug
			if (vector.elementAt(x).fileName.equals(fileName))
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
