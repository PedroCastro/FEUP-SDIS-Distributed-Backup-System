package sdis.network;

import sdis.BackupService;
import sdis.protocol.BackupProtocol;
import sdis.storage.Chunk;
import sdis.utils.Utilities;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
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
     * Map to track the replicas of the chunks of a file being sent
     */
    private final Map<String, Map<Integer, Integer>> replicas;

    /**
     * Constructor of ChannelsHandler
     */
    public ChannelsHandler() {
        this.multicastChannels = new HashMap<>();
        this.replicas = new HashMap<>();
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
     *
     * @param message message to be sent
     * @param channel channel of the message to be sent
     * @return true if message was sent, false otherwise
     */
    public boolean sendMessage(final byte[] message, ChannelType channel) {
        MulticastChannel messageChannel = getChannelByType(channel);
        if (channel == null)
            return false;
        return messageChannel.write(message);
    }

    /**
     * Handle a received message
     *
     * @param message message that was received
     * @param channel channel that got the message
     */
    private void handleMessage(final byte[] message, ChannelType channel) {
        String[] header = Utilities.extractHeader(message);
        if (header == null || header.length <= 0)
            return;

        if (channel == ChannelType.MC) {
            switch (header[BackupProtocol.MESSAGE_TYPE_INDEX]) {
                case BackupProtocol.STORED_MESSAGE:
                    handleStoredChunk(header[BackupProtocol.FILE_ID_INDEX], Integer.parseInt(header[BackupProtocol.CHUNK_NUMBER_INDEX]));
                    break;
            }
        } else if (channel == ChannelType.MDB) {

        } else if (channel == ChannelType.MDR) {

        }
    }

    /**
     * Handle the stored chunk
     * @param fileId file id of the chunk
     * @param chunkNumber number of the chunk
     */
    private void handleStoredChunk(final String fileId, final int chunkNumber) {
        // Add stored confirmation in case it is listening to confirmations
        addStoredConfirmation(fileId, chunkNumber);

        // Update replication degree if that is the case
        Chunk chunk = BackupService.getInstance().getDisk().getChunk(fileId, chunkNumber);
        if(chunk != null) {
            chunk.getState().increaseReplicas();
            BackupService.getInstance().getDisk().updateChunkState(chunk);
        }
    }

    /**
     * Get the number of stored confirmations
     *
     * @param fileId      file id to get those
     * @param chunkNumber chunk number to get those
     * @return number of confirmations for the given chunk
     */
    public int getStoredConfirmations(final String fileId, final int chunkNumber) {
        if (!replicas.containsKey(fileId))
            return -1;

        Map<Integer, Integer> fileReplicasCount = replicas.get(fileId);
        if (!fileReplicasCount.containsKey(chunkNumber))
            return -1;

        return fileReplicasCount.get(chunkNumber);
    }

    /**
     * Add a stored chunk confirmation
     *
     * @param fileId      file id to add the confirmation
     * @param chunkNumber chunk number to add the confirmation
     */
    private void addStoredConfirmation(final String fileId, final int chunkNumber) {
        if (!replicas.containsKey(fileId))
            return;

        Map<Integer, Integer> fileReplicasCount = replicas.get(fileId);
        if(!fileReplicasCount.containsKey(chunkNumber))
            return;

        fileReplicasCount.put(chunkNumber, fileReplicasCount.get(chunkNumber) + 1);
        replicas.put(fileId, fileReplicasCount);
    }

    /**
     * Listen to stored chunk confirmations
     *
     * @param fileId      file id to listen to those
     * @param chunkNumber chunk number to listen to those
     */
    public void listenStoredConfirmations(final String fileId, final int chunkNumber) {
        Map<Integer, Integer> fileReplicasCount;
        if (replicas.containsKey(fileId))
            fileReplicasCount = replicas.get(fileId);
        else
            fileReplicasCount = new HashMap<>();

        fileReplicasCount.put(chunkNumber, 0);
        replicas.put(fileId, fileReplicasCount);
    }

    /**
     * Stop listen to stored chunk confirmations
     *
     * @param fileId      file id to listen to those
     * @param chunkNumber chunk number to listen to those
     */
    public void stopListenStoredConfirmations(final String fileId, final int chunkNumber) {
        if (!replicas.containsKey(fileId))
            return;

        Map<Integer, Integer> fileReplicasCount = replicas.get(fileId);
        fileReplicasCount.remove(chunkNumber);

        if (fileReplicasCount.size() == 0)
            replicas.remove(fileId);
        else
            replicas.put(fileId, fileReplicasCount);
    }
}