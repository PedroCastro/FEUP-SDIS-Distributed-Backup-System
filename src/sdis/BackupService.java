package sdis;

import sdis.network.ChannelType;
import sdis.network.ChannelsHandler;
import sdis.network.MulticastChannel;
import sdis.network.TCPChannel;
import sdis.protocol.BackupChunk;
import sdis.protocol.DeleteFile;
import sdis.protocol.GetChunk;
import sdis.storage.Chunk;
import sdis.storage.Disk;
import sdis.storage.FileChunker;

import java.io.*;
import java.net.InetAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackupService implements RMI {

    /**
     * Default capacity of the disk
     */
    private final static int DEFAULT_DISK_CAPACITY = 100000000; // 95MB
    /**
     * Instance of the backup service
     */
    private static BackupService instance;
    /**
     * Flag to tell if the service is running or not
     */
    public final AtomicBoolean isRunning;
    /**
     * Disk of the backup service
     */
    private final Disk disk;
    /**
     * Channels handler
     */
    private final ChannelsHandler channelsHandler;
    /**
     * Semaphore for restoring chunks
     */
    public Semaphore sem = new Semaphore(1);
    /**
     * Identification of the server
     */
    private String serverId = "default";
    /**
     * File name of the disk
     */
    private String DISK_FILENAME = "disk " + serverId + ".iso";

    /**
     * Constructor of sdis.BackupService
     *
     * @param serverId identification of the server instance
     */
    private BackupService(final String serverId) throws RemoteException {
        this.isRunning = new AtomicBoolean(false);
        this.serverId = serverId;
        this.DISK_FILENAME = serverId + "_disk" + ".iso";
        this.disk = loadDisk();
        saveDisk();
        this.channelsHandler = new ChannelsHandler(serverId);

        // Print disk information
        disk.printInfo();
    }

    /**
     * Instance of the backup service
     *
     * @return backup service instance
     */
    public static BackupService getInstance() {
        return instance;
    }

    /**
     * Main method of the sdis.BackupService
     *
     * @param args arguments sent to the console
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 7) {
            System.out.println("Please execute the backup service using the following format:");
            System.out.println("java -jar BackupService <serverId> <mc_address> <mc_port> <mdb_address> <mdb_port> <mdr_address> <mdr_port>");
            return;
        }

        instance = new BackupService(args[0]);

        instance.createRMI();

        // Create multicast channels
        instance.channelsHandler.addChannel(new MulticastChannel(ChannelType.MC, InetAddress.getByName(args[1]), Integer.parseInt(args[2])));
        instance.channelsHandler.addChannel(new MulticastChannel(ChannelType.MDB, InetAddress.getByName(args[3]), Integer.parseInt(args[4])));
        instance.channelsHandler.addChannel(new MulticastChannel(ChannelType.MDR, InetAddress.getByName(args[5]), Integer.parseInt(args[6])));
        instance.channelsHandler.addChannel(new TCPChannel(ChannelType.TDR));

        // Start the service
        instance.startService();


        instance.reclaimEnh(instance.disk.getUsedBytes());
        try {
            Thread.sleep(100000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        instance.stopService();
    }

    /**
     * Get the server identification
     *
     * @return server identification
     */
    public String getServerId() {
        return serverId;
    }

    /**
     * Get the disk of the backup service
     *
     * @return disk of the backup service
     */
    public Disk getDisk() {
        return disk;
    }

    /**
     * Get the channels handler
     *
     * @return channels handler
     */
    public ChannelsHandler getChannelsHandler() {
        return channelsHandler;
    }

    /**
     * Load the disk
     *
     * @return loaded disk
     */
    public Disk loadDisk() {
        final File diskFile = new File(DISK_FILENAME);

        // Disk already exists
        if (diskFile.exists() && !diskFile.isDirectory()) {
            try {
                return (Disk) new ObjectInputStream(new FileInputStream(diskFile)).readObject();
            } catch (ClassNotFoundException | IOException e) {
                System.out.println("Failed to load the disk! " + e.getMessage());
                return null;
            }
        }
        // Disk does not exist
        else {
            System.out.println("Starting an empty disk with capacity of " + DEFAULT_DISK_CAPACITY + " bytes!");
            return new Disk(DEFAULT_DISK_CAPACITY);
        }
    }

    /**
     * Save the disk to the HDD
     */
    public void saveDisk() {
        try {
            new ObjectOutputStream(new FileOutputStream(DISK_FILENAME)).writeObject(disk);
        } catch (IOException e) {
            System.out.println("Failed to save the disk! " + e.getMessage());
        }
    }

    /**
     * Start the backup service
     */
    public void startService() {
        isRunning.set(true);

        channelsHandler.start();

        System.out.println("Backup service is now running.");
    }

    /**
     * Stop the backup service
     */
    public void stopService() {
        isRunning.set(false);

        channelsHandler.stop();

        System.out.println("Backup service is now stopped.");
    }

    /**
     * Creates the RMI service
     */
    private void createRMI() {

        try {
            RMI rmiService = (RMI) UnicastRemoteObject.exportObject(this, 0);

            try {
                LocateRegistry.createRegistry(1099);
            } catch (ExportException e) {
                System.out.println("Registetry is running");
            }
            Registry registry = LocateRegistry.getRegistry(1099);
            try {
                registry.bind(this.getServerId(), rmiService);
            } catch (AlreadyBoundException e) {
                try {
                    registry.unbind(this.getServerId());
                } catch (NotBoundException e1) {
                    System.out.println("Registry not bound");
                    return;
                }

                //rebind this server
                registry.bind(this.getServerId(), rmiService);
            }

        } catch (RemoteException | AlreadyBoundException e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
            return;
        }
    }

    /**
     * Remote function to backup the given file
     *
     * @param filename  the name of the file to be backed up
     * @param repDegree the degree of replication for this file
     * @throws IOException
     */
    @Override
    public int backup(String filename, int repDegree) throws IOException {
        File file = new File(filename);

        if (!file.exists()) {
            return -1;
        }

        String id = FileChunker.getFileChecksum(file);

        this.disk.addFilename(filename, id);

        //
        int part = 0;

        byte[] chunk = new byte[FileChunker.getMaxSizeChunk()];
        FileInputStream f = new FileInputStream(file);
        BufferedInputStream inputStream = new BufferedInputStream(f);

        int size;
        while ((size = inputStream.read(chunk)) > 0) {
            byte[] currChunk = Arrays.copyOfRange(chunk, 0, size);
            Chunk newChunk = new Chunk(id, part++, currChunk, repDegree);
            Thread thread = new Thread(new BackupChunk(newChunk));
            thread.start();
        }
        inputStream.close();
        f.close();

        this.getDisk().addNumberOfChunks(id, part);


        return 0;

    }

    /**
     * Remote function to restore file
     *
     * @param filename filename to be restored
     * @throws InterruptedException
     * @throws IOException
     */
    @Override
    public int restore(String filename) throws InterruptedException, IOException {

        String id = this.getDisk().getId(filename);

        if (id == null)
            return -1;

        int numberOfChunks = this.getDisk().getNumberOfChunks(id);

        ArrayList<Integer> array = new ArrayList<>();

        for (int i = 0; i < numberOfChunks; i++)
            array.add(i);

        getChannelsHandler().waitingForChunks.put(id, array);

        //locking semaphore to wait for all chunks to be restored
        sem.acquire();

        for (int i = 0; i < numberOfChunks; i++) {
            Chunk newChunk = new Chunk(id, i, (new byte[0]), 0);
            Thread thread = new Thread(new GetChunk(newChunk));
            thread.start();
        }

        //waits to aquire the sem ->ends the restore of all files
        sem.acquire();
        sem.release();

        File file = new File(id);

        if (file.canWrite())
            file.renameTo(new File(filename));

        return 0;
    }

    /**
     * Remote function to delete a file
     *
     * @param filename filename of the file
     * @throws RemoteException
     */
    @Override
    public int delete(String filename) throws RemoteException {

        String id = this.getDisk().getId(filename);

        if (id == null)
            return -1;

        (new DeleteFile(id)).run();

        this.getDisk().removeFilename(filename);

        return 0;
    }

    /**
     * Remote function to reclaim given file
     *
     * @param space space to reclaim
     * @throws RemoteException
     */
    @Override
    public int reclaim(int space) throws RemoteException {
        return disk.freeSpace(space, false) ? 0 : -1;
    }

    /**
     * Remote function to backup the given file enhanced
     *
     * @param filename  the name of the file to be backed up
     * @param repDegree the degree of replication for this file
     * @throws IOException
     */
    @Override
    public int backupEnh(String filename, int repDegree) throws IOException {
        File file = new File(filename);

        if (!file.exists()) {
            return -1;
        }

        String id = FileChunker.getFileChecksum(file);

        this.disk.addFilename(filename, id);

        //
        int part = 0;

        byte[] chunk = new byte[FileChunker.getMaxSizeChunk()];
        FileInputStream f = new FileInputStream(file);
        BufferedInputStream inputStream = new BufferedInputStream(f);

        int size;
        while ((size = inputStream.read(chunk)) > 0) {
            byte[] currChunk = Arrays.copyOfRange(chunk, 0, size);
            Chunk newChunk = new Chunk(id, part++, currChunk, repDegree);
            Thread thread = new Thread(new BackupChunk(newChunk, true));
            thread.start();
        }
        inputStream.close();
        f.close();

        this.getDisk().addNumberOfChunks(id, part);


        return 0;

    }

    /**
     * Enhanced remote function to reclaim given file
     *
     * @param space space to reclaim
     * @throws RemoteException
     */
    @Override
    public int reclaimEnh(int space) throws RemoteException {
        new Thread(() -> disk.freeSpace(space, true)).start();
        return 0;
    }

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
    @Override
    public int restoreEnh(String filename) throws RemoteException, FileNotFoundException, InterruptedException, IOException {
        String id = this.getDisk().getId(filename);

        if (id == null)
            return -1;

        int numberOfChunks = this.getDisk().getNumberOfChunks(id);

        ArrayList<Integer> array = new ArrayList<>();

        for (int i = 0; i < numberOfChunks; i++)
            array.add(i);

        getChannelsHandler().waitingForChunks.put(id, array);

        //locking semaphore to wait for all chunks to be restored
        sem.acquire();

        for (int i = 0; i < numberOfChunks; i++) {
            Chunk newChunk = new Chunk(id, i, (new byte[0]), 0);
            Thread thread = new Thread(new GetChunk(newChunk, true, getChannelsHandler().getChannelByType(ChannelType.TDR).getPort())); // TODO replace with PORT
            thread.start();
        }

        //waits to aquire the sem ->ends the restore of all files
        sem.acquire();
        sem.release();

        File file = new File(id);

        if (file.canWrite())
            file.renameTo(new File(filename));

        return 0;
    }
}