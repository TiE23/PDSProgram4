// Kyle Geib - Program 4 - CSS434 Fall 2012 - Dr Fukuda - December 13th 2012
// ServerInterface.java

package main;

import java.rmi.*;

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
    		String fileName, String mode) throws RemoteException;
    
    /**Upload is called from a DFSClient and has a file sent to the Server.
     * @param clientIP
     * @param fileName
     * @param data
     * @return Whether or not the action was successful.
     */
    public boolean upload(String clientIP, 
    		String fileName, FileContents data) throws RemoteException;
}
