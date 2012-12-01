// Kyle Geib - Program 4 - CSS434 Fall 2012 - Dr Fukuda - December 13th 2012

package main;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;

/**DFS Client
 * @author Kyle Geib
 */
public class DFSClient extends UnicastRemoteObject implements ClientInterface, Runnable {

	// Thread object for using Runnable for the user prompts.
	Thread thread;
	
	/**Enum to track the different states of this DFSClient's file.*/
	private enum ClientState {
		Invalid, Read_Shared, Write_Owned, Release_Ownership
	}
	
	/**Enum to track the access mode of the current file: read or write.*/
	private enum AccessMode {
		Invalid, Read, Write
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
		accessMode = AccessMode.Invalid;
		
		fileContents = null;
		fileName = "";
		hasOwnership = false;
		
		cleanFile();	// Clean out and/or initialize the local file cache.
		
		// Creating a new thread for the user prompt function
		thread = new Thread(this, "Prompt Thread");
		thread.start();
	}
	
	
	// Invalidate, an RMI method
	public boolean invalidate() {
		
		System.out.println("RMI: invalidate()");
		clientState = ClientState.Invalid;	// Change state to invalid.
		
		try {
			// Use chmod to set access to none (000).
			Runtime runtime = Runtime.getRuntime( );   
			runtime.exec("chmod 000 ./tmp/" + accountName + ".txt");
		} catch (IOException e) {e.printStackTrace();}
		
		return true;
	}
	
	
	// Writeback, an RMI method
	public boolean writeback() {
		System.out.println("RMI: writeback()");
		if (clientState == ClientState.Write_Owned) {
			clientState = ClientState.Release_Ownership;
			return true;	// Change state. Return successful result.
		} else
			return false;	// Undefined result.
	}
	
	
	// Resume, an RMI method.
	public void resume(String fileName) {
		System.out.println("RMI: resume()");
		// Checks the name to make sure this is a file the client still wants.
		if (this.fileName.equals(fileName) && pullFile(fileName, "w")) {	
			
			clientState = ClientState.Write_Owned;	// Next state
			
			writeToFile(fileContents.get());	// Write to file.
			
			// Use chmod to set to write mode (600).
			chmod("600");
		}
	}
	
	
	/**Performs the user prompting cycle in the console.
	 * @throws IOException 
	 */
	private void userPrompt() throws IOException {

		// The prompting loop
		while (true) {
			
			if (clientState == ClientState.Release_Ownership) {
				
				System.out.println("++++ Server requests ownership release of "
						+ fileName + " ++++");
				
				if (pushFile()) {
					// Use chmod to set to read mode (400).
					chmod("400");
					clientState = ClientState.Read_Shared;
					accessMode = AccessMode.Read;
				} else {
					System.err.println("Unable to upload file!");
					break;	// This is bad.
				}
			}
			
			
			System.out.print("Current file: ");
			
			if (clientState == ClientState.Invalid) {
				System.out.println("None");
			} else {
				System.out.println(fileName);
			}
			
			System.out.println("Current Client State: " + clientState);
			System.out.println("Current Access Mode: " + accessMode);
			System.out.println("Current Owner?: " + hasOwnership);
			
			// Command line input
			Console c = System.console();
			boolean acceptable = false;
			String fileTarget = "";
			String mode = "";
			
			while (!acceptable) {
				fileTarget = c.readLine("File name: ");
				mode = c.readLine("How(r/w): ");
				
				// Set to lower-case, functions use lower-case r and w
				mode.toLowerCase();
				
				// World-class input checking.
				if ( fileTarget.length() != 0 && 
						(mode.equals("r") || mode.equals("w")) )
					acceptable = true;
				else 
					System.out.println("Try putting a valid command this time...");
			}
			
			switch (clientState) {	// State switch...
			case Invalid:								// No file
				if (mode.equals("r")) {					// Read mode
					if (pullFile(fileTarget, mode)) {	// Success
						clientState = ClientState.Read_Shared;
						
						writeToFile(fileContents.get());	// Write to file.
						
						// Use chmod to set to read mode (400).
						chmod("400");
						
					} else {							// Failure
						System.out.println("--Download failed, try again!");
						clientState = ClientState.Invalid;
					}
				} else {								// Write mode
					if (pullFile(fileTarget, mode)) {	// Success
						clientState = ClientState.Write_Owned;	// Next state
						
						writeToFile(fileContents.get());	// Write to file.
						
						// Use chmod to set to write mode (600).
						chmod("600");
						
					} else {							// Failure
						System.out.println("--Download failed, try again!");
						clientState = ClientState.Invalid;
					}
				}
				break;
				
			case Read_Shared:							// Reading a file
				if (mode.equals("r")) {					// Read mode
					if (fileName.equals(fileTarget)) {	// Same file
						// "Do nothing"
						System.out.println("Using local file...");
						
					} else {							// Replace file
						if (pullFile(fileTarget, mode)) {	// Success
							clientState = ClientState.Read_Shared;
							
							writeToFile(fileContents.get());	// Write to file.
							
							// Use chmod to set to read mode (400).
							chmod("400");
							
						} else {							// Failure
							System.out.println("--Download failed, try again!");
							clientState = ClientState.Invalid;
						}
					}
				} else {								// Write mode
					/* Whether trying to write to the same file or a new one,
					 * the actions taken are the same. */
					
					if (pullFile(fileTarget, mode)) {	// Success
						clientState = ClientState.Write_Owned;	// Next state
						
						writeToFile(fileContents.get());	// Write to file.
						
						// Use chmod to set to write mode (600).
						chmod("600");
						
					} else {							// Failure
						System.out.println("--Download failed, try again!");
						clientState = ClientState.Invalid;
					}
				}
				break;
				
			case Write_Owned:							// Writing a file
				if (fileName.equals(fileTarget)) {		// Same file
					// "Do nothing"
					System.out.println("Using local file...");
					
				} else {
					if (pushFile()) {
						accessMode = AccessMode.Invalid;
						clientState = ClientState.Invalid;
					} else {
						System.err.println("Unable to upload file!");
						break;	// This is bad.
					}
					
					if (mode.equals("r")) {					// Read mode
						
						if (pullFile(fileTarget, mode)) {	// Success
							clientState = ClientState.Read_Shared;
							
							writeToFile(fileContents.get());	// Write to file.
							
							// Use chmod to set to read mode (400).
							chmod("400");
							
						} else {							// Failure
							System.out.println("--Download failed, try again!");
							clientState = ClientState.Invalid;
						}
					
					} else {								// Write mode
	
						if (pullFile(fileTarget, mode)) {	// Success
							clientState = ClientState.Write_Owned;
							
							writeToFile(fileContents.get());	// Write to file.
							
							// Use chmod to set to write mode (600).
							chmod("600");
							
						} else {							// Failure
							System.out.println("--Download failed, try again!");
							clientState = ClientState.Invalid;
						}
					}
				}
				break;
				
			case Release_Ownership:						// Need to release
			
				System.out.println("++++ Server requests ownership release of "
						+ fileName + " ++++");
				System.out.println("---- Sorry, but your last request for " 
						+ fileTarget + " has been ignored. ----");
				
				if (pushFile()) {
					// Use chmod to set to read mode (400).
					chmod("400");
					clientState = ClientState.Read_Shared;
					accessMode = AccessMode.Read;
				} else {
					System.err.println("Unable to upload file! This is bad!");
					break;	// This is bad.
				}
			
				break;
			}
			
			// Now the user reads or makes changes to their file.
			System.out.println(
					"\nPlease use emacs with your file at this time...\n");
			c.readLine("When finished close emacs and " +
					"hit enter to continue.\n");
		}
		
	}
	
	
	/**Pull performs a download on a requested file from the server.
	 * NOTE: Does not update the clientState.
	 * @param fileName
	 * @param mode "r" for read. "w" for write.
	 * @return Success of the download. False means no change: likely suspended
	 */
	private boolean pullFile(String fileName, String mode) {
		System.out.println("pullFile("+fileName + ", " + mode + ")");
		try {
			ServerInterface server = (ServerInterface) 
					Naming.lookup("rmi://" + serverIP + 
							":" + port + "/dfsserver");
			
			FileContents returnFile = server.download(clientIP, fileName, mode); 
			
			// This will be the new file...
			this.fileName = fileName;
			
			if (returnFile == null) {	// Failure
				hasOwnership = false;
				accessMode = AccessMode.Invalid;
				return false;
				
			} else {					// Success
				
				System.out.println("return: " + returnFile.get() );
				
				fileContents = returnFile;	// Update fileContents
				
				if (mode.equals("r")) {	// Read mode
					hasOwnership = false;
					accessMode = AccessMode.Read;
					
				} else {				// Write mode
					hasOwnership = true;
					accessMode = AccessMode.Write;
				}
				
				return true;
			}
		} catch (Exception e) {e.printStackTrace(); return false;}
	}
	
	
	/**Push performs an upload of the client's current file.
	 * NOTE: Performs no other tasks other than to upload fileContents!!!
	 * @return Success of the upload operation.
	 */
	private boolean pushFile() {
		System.out.println("pushFile()");
		
		// Update the contents with the text file's data.
		fileContents = new FileContents(readFromFile());
		
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
		System.out.println("writeToFile()");
		cleanFile(); // Clean out the file that may exist or create a new one
		
		try {
			
			chmod("600");
			
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
		System.out.println("readFromFile()");
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

	
	/**chmod command function
	 * @param code Numerical code for permissions
	 * @return Success of the operation
	 */
	private boolean chmod(String code) {
		try {
			Runtime runtime = Runtime.getRuntime( );   
			runtime.exec("chmod " + code + " ./tmp/" + accountName + ".txt");
		} catch (IOException e) {e.printStackTrace(); return false;}
		return true;
	}
	
	
	/**run Creates a new thread of DFSClient that runs the userPrompt function.
	 */
	public void run() {
		try {
			userPrompt();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
