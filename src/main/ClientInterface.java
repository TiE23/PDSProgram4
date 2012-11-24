// Kyle Geib - Program 4 - CSS434 Fall 2012 - Dr Fukuda - November 13rd 2012

package main;
import java.rmi.*;
import java.util.*;

/**Interface class for the DFSClient.
 * @author Kyle Geib
 */
public interface ClientInterface extends Remote {
	/**Invalidates the Client's file.
	 * @return Success of the invalidation of the Client's file.
	 */
	public boolean invalidate();
	
	/**Writeback commands the Client to upload its file to thye Server.
	 * @return Success of the write-back of the Client's file.
	 */
	public boolean writeback();
}
