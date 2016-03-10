import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackupService {

    /**
     * Instance of the backup service
     */
    private static BackupService instance;

    /**
     * Instance of the backup service
     * @return backup service instance
     */
    public static BackupService getInstance() {
        return instance;
    }

    /**
     * Main method of the BackupService
     * @param args arguments sent to the console
     */
    public static void main(String[] args) throws IOException {
        if(args.length < 7) {
            System.out.println("Please execute the backup service using the following format:");
            System.out.println("java -jar BackupService <serverId> <mc_address> <mc_port> <mdb_address> <mdb_port> <mdr_address> <mdr_port>");
            return;
        }

        instance = new BackupService(args[0]);

        // Create multicast channels
        instance.addChannel(new MulticastChannel("MC", InetAddress.getByName(args[1]), Integer.parseInt(args[2])));
        instance.addChannel(new MulticastChannel("MDB", InetAddress.getByName(args[3]), Integer.parseInt(args[4])));
        instance.addChannel(new MulticastChannel("MDR", InetAddress.getByName(args[5]), Integer.parseInt(args[6])));

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
    private AtomicBoolean isRunning;

    /**
     * Identification of the server
     */
    private String serverId;

    /**
     * Map with all the multicast channels and correspondent thread
     */
    private Map<MulticastChannel, Thread> multicastChannels;

    /**
     * Constructor of BackupService
     * @param serverId identification of the server instance
     */
    private BackupService(final String serverId) {
        this.isRunning = new AtomicBoolean(false);
        this.serverId = serverId;
        this.multicastChannels = new HashMap<>();
    }

    /**
     * Get the server identification
     * @return server identification
     */
    public String getServerId() {
        return serverId;
    }

    /**
     * Add a multicast channel
     * @param channel channel to be added
     */
    public void addChannel(final MulticastChannel channel) {
        multicastChannels.put(channel, null);
    }

    /**
     * Remove a multicast channel
     * @param channel channel to be removed
     */
    public void removeChannel(final MulticastChannel channel) {
        multicastChannels.remove(channel);
    }

    /**
     * Get a multicast channel by its name
     * @param channelName name of the channel
     * @return channel with that name
     */
    public MulticastChannel getChannelByName(final String channelName) {
        for(MulticastChannel channel : multicastChannels.keySet())
            if(channel.getName().equalsIgnoreCase(channelName))
                return channel;
        return null;
    }

    /**
     * Start the backup service
     */
    public void startService() {
        isRunning.set(true);

        // Create a thread per multicast channel
        for(MulticastChannel channel : new HashMap<>(multicastChannels).keySet()) {
            Thread channelThread = new Thread() {
                @Override
                public void run() {
                    System.out.println(channel.getName() + " is listening.");

                    byte[] data;
                    while(isRunning.get()) {
                        data = channel.read();
                        if(data == null)
                            continue;

                        System.out.println("Received " + data.length + " bytes.");
                    }
                }
            };
            channelThread.start();
            multicastChannels.put(channel, channelThread);
        }

        System.out.println("Backup service is now running.");
    }

    /**
     * Stop the backup service
     */
    public void stopService() {
        isRunning.set(false);

        // Close all multicast channels
        for(Map.Entry<MulticastChannel, Thread> entry : multicastChannels.entrySet()) {
            // Wait for thread to finish
            try {
                entry.getValue().join();
            } catch (InterruptedException ignored) {
            }

            // Close safely the channel
            MulticastChannel channel = entry.getKey();
            System.out.println(channel.getName() + " has been closed.");
            channel.close();
        }

        System.out.println("Backup service is now stopped.");
    }
}