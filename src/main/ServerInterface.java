// Kyle Geib - Program 4 - CSS434 Fall 2012 - Dr Fukuda - November 13rd 2012

package main;
import java.rmi.*;
import java.util.*;

/**Interface class for the DFSServer.
 * @author Kyle Geib
 */
public interface ServerInterface extends Remote {
	/**Download is called from a DFSClient and has a file sent to the Client.
	 * @param clientIP
	 * @param fileName
	 * @param mode
	 * @return The contents of the file requested
	 */
    public FileContents download(String clientIP, 
    		String fileName, String mode);
    
    /**Upload is called from a DFSClient and has a file sent to the Server.
     * @param clientIP
     * @param fileName
     * @param mode
     * @return Whether or not the action was successful.
     */
    public boolean upload(String clientIP, String fileName, String mode);
}
