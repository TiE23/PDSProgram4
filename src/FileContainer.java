// Kyle Geib - Program 4 - CSS434 Fall 2012 - Dr Fukuda - November 13rd 2012

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
	
	private String fileName;
	private Vector<String> readers;
	private String owner;
	private FileState fileState;
	private FileContents data;
	
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
	
	/**Gets the FileContents of this file.
	 * @return
	 */
	public FileContents getData() {
		return data;
	}
	
	/**Sets the state of this file.
	 * @param fileState
	 */
	public void setState(FileState fileState) {	// NOT SURE IF THIS WILL BE NECESSARY IN THE END!
		this.fileState = fileState;
	}
	
	/**Gets the state of this file.
	 * @return
	 */
	public FileState getState() {
		return fileState;
	}
	
	/**For use with Vector's contains() method among others.
	 * @param fileName
	 * @return
	 */
	public boolean equals(String fileName) {
		return this.fileName.equals(fileName);
	}
}
