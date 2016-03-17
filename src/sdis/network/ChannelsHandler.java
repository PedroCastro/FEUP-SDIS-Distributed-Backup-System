package sdis.network;

import sdis.BackupService;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for all the multicast channels
 */
public class ChannelsHandler {

    /**
     * Map with all the multicast channels and correspondent thread
     */
    private final Map<MulticastChannel, Thread> multicastChannels;

    /**
     * Constructor of ChannelsHandler
     */
    public ChannelsHandler() {
        this.multicastChannels = new HashMap<>();
    }

    /**
     * Start the channels handler
     */
    public void start() {
        // Listen to all channels
        listenChannel(getChannelByType(ChannelType.MC));
        listenChannel(getChannelByType(ChannelType.MDB));
        listenChannel(getChannelByType(ChannelType.MDR));
    }

    /**
     * Stop the channels handler
     */
    public void stop() {
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
                while (BackupService.getInstance().isRunning.get()) {
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
     * Send a message to a channel
     * @param message message to be sent
     * @param type type of the message to be sent
     * @return true if message was sent, false otherwise
     */
    public boolean sendMessage(final byte[] message, ChannelType type) {
        MulticastChannel channel = getChannelByType(type);
        if(channel == null)
            return false;
        return channel.write(message);
    }
}
