package sdis;

import sdis.network.ChannelType;
import sdis.network.ChannelsHandler;
import sdis.network.MulticastChannel;
import sdis.storage.Disk;

import java.io.*;
import java.net.InetAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackupService implements RMI{

    /**
     * Default capacity of the disk
     */
    private final static int DEFAULT_DISK_CAPACITY = 100000000; // 95MB

    /**
     * File name of the disk
     */
    private final static String DISK_FILENAME = "disk.iso";

    /**
     * Instance of the backup service
     */
    private static BackupService instance;

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

        // Start the service
        instance.startService();
        try {
            Thread.sleep(100000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        instance.stopService();
    }

    /**
     * Flag to tell if the service is running or not
     */
    public final AtomicBoolean isRunning;

    /**
     * Identification of the server
     */
    private final String serverId;

    /**
     * Disk of the backup service
     */
    private final Disk disk;

    /**
     * Channels handler
     */
    private final ChannelsHandler channelsHandler;

    /**
     * Constructor of sdis.BackupService
     *
     * @param serverId identification of the server instance
     */
    private BackupService(final String serverId) {
        this.isRunning = new AtomicBoolean(false);
        this.serverId = serverId;
        this.disk = loadDisk(); saveDisk();
        this.channelsHandler = new ChannelsHandler();


        // Print disk information
        System.out.println("Disk - f:" + disk.getFreeBytes() + "b / u:" + disk.getUsedBytes() + "b / c:" + disk.getCapacity() + "b");
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


            LocateRegistry.createRegistry(2001);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(this.getServerId(), rmiService);

        } catch (RemoteException|AlreadyBoundException e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
            return;
        }
    }
    /**
     * rmi testing func
     */
    public String test(){
        return "yey";
    }
}