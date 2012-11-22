// Kyle Geib - Program 4 - CSS434 Fall 2012 - Dr Fukuda - November 13rd 2012

import java.io.*;
import java.util.*;

/**Class holds the byte[] data of the file.
 * @author Kyle Geib
 */
public class FileContents implements Serializable {
	private byte[] contents;	// file contents
	
	/**Create a new FileContents object with byte array.
	 * @param contents
	 */
	public FileContents(byte[] contents) {
		this.contents = contents;
	}
	
	/**Prints contents of file to console.
	 * @throws IOException
	 */
	public void print() throws IOException {
		System.out.println("FileContents = " + contents);
	}
	
	/**Returns byte array of contents.
	 * @return
	 */
	public byte[] get() {
		return contents;
	}
}
