package sdis;

import sdis.network.ChannelType;
import sdis.network.MulticastChannel;
import sdis.storage.Disk;

import java.io.*;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackupService {

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

        // Create multicast channels
        instance.addChannel(new MulticastChannel(ChannelType.MC, InetAddress.getByName(args[1]), Integer.parseInt(args[2])));
        instance.addChannel(new MulticastChannel(ChannelType.MDB, InetAddress.getByName(args[3]), Integer.parseInt(args[4])));
        instance.addChannel(new MulticastChannel(ChannelType.MDR, InetAddress.getByName(args[5]), Integer.parseInt(args[6])));

        // Start the service
        instance.startService();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        instance.stopService();
    }

    /**
     * Flag to tell if the service is running or not
     */
    private final AtomicBoolean isRunning;

    /**
     * Identification of the server
     */
    private final String serverId;

    /**
     * Disk of the backup service
     */
    private final Disk disk;

    /**
     * Map with all the multicast channels and correspondent thread
     */
    private final Map<MulticastChannel, Thread> multicastChannels;

    /**
     * Constructor of sdis.BackupService
     *
     * @param serverId identification of the server instance
     */
    private BackupService(final String serverId) {
        this.isRunning = new AtomicBoolean(false);
        this.serverId = serverId;
        this.multicastChannels = new HashMap<>();
        this.disk = loadDisk(); saveDisk();

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
     * Add a multicast channel
     *
     * @param channel channel to be added
     */
    public void addChannel(final MulticastChannel channel) {
        multicastChannels.put(channel, null);
    }

    /**
     * Remove a multicast channel
     *
     * @param channel channel to be removed
     */
    public void removeChannel(final MulticastChannel channel) {
        multicastChannels.remove(channel);
    }

    /**
     * Get a multicast channel by its type
     *
     * @param type type of the channel
     * @return channel with that type
     */
    public MulticastChannel getChannelByType(final ChannelType type) {
        for (final MulticastChannel channel : multicastChannels.keySet())
            if (channel.getType() == type)
                return channel;
        return null;
    }

    /**
     * Start the backup service
     */
    public void startService() {
        isRunning.set(true);

        // Listen to all channels
        listenChannel(getChannelByType(ChannelType.MC));
        listenChannel(getChannelByType(ChannelType.MDB));
        listenChannel(getChannelByType(ChannelType.MDR));

        System.out.println("Backup service is now running.");
    }

    /**
     * Start listening the a multicast channel
     *
     * @param channel channel to listen to
     */
    private void listenChannel(final MulticastChannel channel) {
        final Thread mcChannelThread = new Thread() {
            @Override
            public void run() {
                System.out.println(channel.getType() + " is listening.");

                byte[] data;
                while (isRunning.get()) {
                    data = channel.read();
                    if (data == null)
                        continue;

                    System.out.println("Received " + data.length + " bytes.");
                }
            }
        };
        mcChannelThread.start();
        multicastChannels.put(channel, mcChannelThread);
    }

    /**
     * Stop the backup service
     */
    public void stopService() {
        isRunning.set(false);

        // Close all multicast channels
        for (final Map.Entry<MulticastChannel, Thread> entry : multicastChannels.entrySet()) {
            // Wait for thread to finish
            try {
                entry.getValue().join();
            } catch (InterruptedException ignored) {
            }

            // Close safely the channel
            final MulticastChannel channel = entry.getKey();
            System.out.println(channel.getType() + " has been closed.");
            channel.close();
        }

        System.out.println("Backup service is now stopped.");
    }
}