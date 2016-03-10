import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

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
    }

    /**
     * Identification of the server
     */
    private String serverId;

    /**
     * List with all the multicast channels
     */
    private List<MulticastChannel> multicastChannels;

    /**
     * Constructor of BackupService
     * @param serverId identification of the server instance
     */
    private BackupService(final String serverId) {
        this.serverId = serverId;
        this.multicastChannels = new ArrayList<>();
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
        multicastChannels.add(channel);
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
        for(MulticastChannel channel : multicastChannels)
            if(channel.getName().equalsIgnoreCase(channelName))
                return channel;
        return null;
    }
}