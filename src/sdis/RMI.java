package sdis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote Method Interface
 */
public interface RMI extends Remote {

    /**
     * Remote function to backup the given file
     *
     * @param filename  the name of the file to be backed up
     * @param repDegree the degree of replication for this file
     */
    int backup(String filename, int repDegree) throws IOException;

    /**
     * Remote function to restore file
     *
     * @param filename filename to be restored
     * @throws InterruptedException
     * @throws IOException
     */
    int restore(String filename) throws InterruptedException, IOException;

    /**
     * Remote function to delete a file
     *
     * @param filename filename of the file
     * @throws RemoteException
     */
    int delete(String filename) throws RemoteException;

    /**
     * Remote function to reclaim given file
     *
     * @param space space to reclaim
     * @throws RemoteException
     */
    int reclaim(int space) throws RemoteException;

    /**
     * Remote function to backup the given file enhanced
     *
     * @param filename  the name of the file to be backed up
     * @param repDegree the degree of replication for this file
     * @throws IOException
     */
    int backupEnh(String filename, int repDegree) throws IOException;

    /**
     * Enhanced remote function to reclaim given file
     *
     * @param space space to reclaim
     * @throws RemoteException
     */
    int reclaimEnh(int space) throws RemoteException;

    /**
     * Enhanced version of restore function
     *
     * @param filename filename of file to be restored
     * @return -1 if errors have occurred
     * @throws RemoteException
     * @throws FileNotFoundException
     * @throws InterruptedException
     * @throws IOException
     */
    int restoreEnh(String filename) throws InterruptedException, IOException;
}
