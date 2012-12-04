// Kyle Geib - Program 4 - CSS434 Fall 2012 - Dr Fukuda - December 13th 2012

package main;

import java.util.Vector;

/**Class holds file caching information for DFSServer.
 * This includes data such as a file name, the IPs of client readers, the
 * owner, the state of the file, and the data (FileContents).
 * @author Kyle Geib
 */
public class FileContainer {
	
	/**Enumerator to track the different states of this FileContainer.*/
	public enum FileState {
		Not_Shared, Read_Shared, Write_Shared, Ownership_Change
	}
	
	public String fileName;
	public Vector<String> readers;
	public String owner;
	public FileState fileState;
	public FileContents data;
	
	
	/**Constructor - Create a new file for the DFSServer cache with only name.
	 * @param fileName
	 */
	public FileContainer(String fileName) {
		this.fileName = fileName;
		readers = new Vector<String>();
		owner = "";
		fileState = FileState.Not_Shared;
		data = new FileContents(new byte[0]); // Start with size-of-zero array
	}
	
	
	/**Constructor - Create a new file for the DFSServer's cache.
	 * @param fileName
	 * @param clientIP
	 * @param mode
	 */
	public FileContainer(String fileName, String clientIP, String mode) {
		this.fileName = fileName;
		readers = new Vector<String>();
		
		if (mode.equals("w")) {	// Write mode.
			
			owner = clientIP;	// If writing, then this creator is the owner
			fileState = FileState.Write_Shared;	// Write
			
		} else {				// Read mode.
			
			safeAddReader(clientIP);	// Add creating client to the readers list
			fileState = FileState.Read_Shared;	// Read	
			
		}
		
		data = new FileContents(new byte[0]); // Start with size-of-zero array
	}
	
	
	/**Add a clientIP as a reader for this file without inserting repeats.
	 * @param clientIP The new client wanting to read this file.
	 * @return 
	 */
	public boolean safeAddReader(String clientIP) {
		if (readers.contains(clientIP)) {
			return false;	// This client is already a reader. Don't add again
		} else {
			readers.add(clientIP);
			return true;	// This client is not a reader. Add to readers.
		}	
	}
	
	
	/**Prints out the contents of the file's readers to the console.
	 */
	public void reportReaders() {
		System.out.println("# of readers: " + readers.size());
		for (int x = 0; x < readers.size(); ++x) {
			System.out.println("    reader = " + readers.elementAt(x));
		}
	}
}
