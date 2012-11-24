// Kyle Geib - Program 4 - CSS434 Fall 2012 - Dr Fukuda - November 13rd 2012

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
		readers.add(clientIP);	// Add creating client's IP to the readers list
		
		if (mode.equals("w")) {	
			owner = clientIP;	// If writing, then this creator is the owner
			fileState = FileState.Write_Shared;	// Write
		} else {
			fileState = FileState.Read_Shared;	// Read
		}
		
		data = new FileContents(new byte[0]); // Start with size-of-zero array
	}
	
	
	/**For use with Vector's contains() method among others.
	 * @param fileName
	 * @return
	 */
	public boolean equals(String fileName) {
		// TODO - If this doesn't work. May need to make a FileContainers Object before comparison.
		return this.fileName.equals(fileName);
	}
}
