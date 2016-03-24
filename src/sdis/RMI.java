package sdis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Pedro Castro on 18/03/2016.
 */
public interface RMI extends Remote{

    String test()throws RemoteException;

    /**
     * remote function to backup the given file
     * @param file the file to be backed up
     * @param repDegree the degree of replication for this file
     */
    int backup(String filename, int repDegree) throws RemoteException, IOException;

    void restore(String filename) throws RemoteException, FileNotFoundException,InterruptedException,IOException;

    void delete(String filename) throws RemoteException;

    void reclaim(String filename) throws RemoteException;

}
