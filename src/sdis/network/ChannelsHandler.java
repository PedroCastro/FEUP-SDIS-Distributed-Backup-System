package sdis.network;

import sdis.BackupService;
import sdis.protocol.BackupProtocol;
import sdis.protocol.BackupRemovedChunk;
import sdis.protocol.RestoreChunk;
import sdis.protocol.StoredChunk;
import sdis.storage.Chunk;
import sdis.storage.ChunkState;
import sdis.utils.Utilities;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for all the multicast channels
 */
public class ChannelsHandler {

    /**
     * Map with the chunks we are waiting for being restored
     * <FileId, ChunkNo>
     */
    public final Map<String, ArrayList<Integer>> waitingForChunks;

    /**
     * Map with all the channels and correspondent thread
     */
    private final Map<Channel, Thread> channels;
    /**
     * Map to track the mirrors of the chunks of a file being sent
     * <FileId, <ChunkNo, ChunkState>>
     */
    private final Map<String, Map<Integer, ChunkState>> mirrorDevices;
    /**
     * Map with chunks that will be restored
     * <FileId, <ChunkNo, RestoreThread>>
     */
    private final Map<String, Map<Integer, RestoreChunk>> chunksForRestore;
    /**
     * Chunks to backup again because they were removed and the count dropped below the desired
     * level of replication.
     * <FileId, <ChunkNo, BackupRemovedChunk>>
     */
    private final Map<String, Map<Integer, BackupRemovedChunk>> chunksBackupAgain;
    /**
     * The id of the server of this channels handler
     */
    private final String serverId;
    /**
     * Stored messages received
     */
    public Map<String, Map<Integer, Integer>> storedMessagesReceived;
    /**
     * Map with number of putchunks for chunk
     */
    private Map<String, Map<Integer, Integer>> storedListened;

    /**
     * Constructor of ChannelsHandler
     */
    public ChannelsHandler(final String serverId) {
        this.channels = new HashMap<>();
        this.mirrorDevices = new HashMap<>();
        this.storedMessagesReceived = new HashMap<>();
        this.waitingForChunks = new HashMap<>();
        this.chunksForRestore = new HashMap<>();
        this.chunksBackupAgain = new HashMap<>();
        this.storedListened = new HashMap<>();
        this.serverId = serverId;
    }

    /**
     * Start the channels handler
     */
    public void start() {
        // Listen to all channels
        listenChannel(getChannelByType(ChannelType.MC));
        listenChannel(getChannelByType(ChannelType.MDB));
        listenChannel(getChannelByType(ChannelType.MDR));
        listenChannel(getChannelByType(ChannelType.TDR));
    }

    /**
     * Stop the channels handler
     */
    public void stop() {
        // Close all multicast channels
        for (final Map.Entry<Channel, Thread> entry : channels.entrySet()) {
            // Wait for thread to finish
            try {
                entry.getValue().join();
            } catch (InterruptedException ignored) {
            }

            // Close safely the channel
            final Channel channel = entry.getKey();
            System.out.println(channel.getType() + " has been closed.");
            channel.close();
        }
    }

    /**
     * Add a multicast channel
     *
     * @param channel channel to be added
     */
    public void addChannel(final Channel channel) {
        channels.put(channel, null);
    }

    /**
     * Remove a multicast channel
     *
     * @param channel channel to be removed
     */
    public void removeChannel(final Channel channel) {
        channels.remove(channel);
    }

    /**
     * Get a channel by its type
     *
     * @param type type of the channel
     * @return channel with that type
     */
    private Channel getChannelByType(final ChannelType type) {
        for (final Channel channel : channels.keySet())
            if (channel.getType() == type)
                return channel;
        return null;
    }

    /**
     * Start listening a channel
     *
     * @param channel channel to listen to
     */
    private void listenChannel(final Channel channel) {
        final Thread channelThread = new Thread() {
            @Override
            public void run() {
                System.out.println(channel.getType() + " is listening.");

                while (BackupService.getInstance().isRunning.get()) {
                    final DatagramPacket data = (DatagramPacket) channel.read();
                    if (data == null)
                        continue;

                    //System.out.println("Received " + data.getLength() + " bytes.");

                    // Handle the received message
                    new Thread(() -> handleMessage(data.getData(), data.getLength(), data.getAddress(), channel.getType())).start();
                    //handleMessage(data, channel.getType());
                }
            }
        };
        channelThread.start();
        channels.put(channel, channelThread);
    }

    /**
     * Send a message to a channel
     *
     * @param message message to be sent
     * @param channel channel of the message to be sent
     * @return true if message was sent, false otherwise
     */
    public boolean sendMessage(final byte[] message, ChannelType channel) {
        Channel messageChannel = getChannelByType(channel);
        if (channel == null)
            return false;
        return messageChannel.write(message);
    }

    /**
     *
     *      MESSAGES HANDLERS
     *
     */

    /**
     * Handle a received message
     *
     * @param data    data that was received
     * @param length  length of the data received
     * @param address address of the sender
     * @param channel channel that got the message
     */
    private synchronized void handleMessage(final byte[] data, final int length, final InetAddress address, ChannelType channel) {
        String[] header = Utilities.extractHeader(data);
        if (header == null || header.length <= 0)
            return;

        if (header[BackupProtocol.SENDER_INDEX].equals(this.serverId))
            return;


        // Multicast Control Channel
        if (channel == ChannelType.MC) {
            switch (header[BackupProtocol.MESSAGE_TYPE_INDEX]) {
                case BackupProtocol.STORED_MESSAGE:
                    handleStoredChunk(header[BackupProtocol.FILE_ID_INDEX],
                            Integer.parseInt(header[BackupProtocol.CHUNK_NUMBER_INDEX]),
                            header[BackupProtocol.SENDER_INDEX]);
                    break;
                case BackupProtocol.GETCHUNK_MESSAGE:
                    if (header[BackupProtocol.VERSION_INDEX].equals(Integer.toString(BackupProtocol.VERSION_ENHANCEMENT)))
                        handleGetChunk(header[BackupProtocol.FILE_ID_INDEX],
                                Integer.parseInt(header[BackupProtocol.CHUNK_NUMBER_INDEX]),
                                address,
                                Integer.parseInt(header[BackupProtocol.TCP_PORT]));
                    else
                        handleGetChunk(header[BackupProtocol.FILE_ID_INDEX],
                                Integer.parseInt(header[BackupProtocol.CHUNK_NUMBER_INDEX]),
                                address,
                                -1);
                    break;
                case BackupProtocol.DELETE_MESSAGE:
                    handleDeleteFile(header[BackupProtocol.FILE_ID_INDEX]);
                    break;
                case BackupProtocol.REMOVED_MESSAGE:
                    handleRemovedChunk(header[BackupProtocol.FILE_ID_INDEX],
                            Integer.parseInt(header[BackupProtocol.CHUNK_NUMBER_INDEX]),
                            header[BackupProtocol.SENDER_INDEX]);
                    break;
            }
        }
        // Multicast Data Backup Channel
        else if (channel == ChannelType.MDB) {
            switch (header[BackupProtocol.MESSAGE_TYPE_INDEX]) {
                case BackupProtocol.PUTCHUNK_MESSAGE:
                    byte[] body = Utilities.extractBody(data, length);
                    if (header[BackupProtocol.VERSION_INDEX].equals(Integer.toString(BackupProtocol.VERSION_ENHANCEMENT)))
                        handlePutChunkEnh(header[BackupProtocol.FILE_ID_INDEX],
                                Integer.parseInt(header[BackupProtocol.CHUNK_NUMBER_INDEX]),
                                Integer.parseInt(header[BackupProtocol.REPLICATION_DEG_INDEX]), body);
                    else
                        handlePutChunk(header[BackupProtocol.FILE_ID_INDEX],
                                Integer.parseInt(header[BackupProtocol.CHUNK_NUMBER_INDEX]),
                                Integer.parseInt(header[BackupProtocol.REPLICATION_DEG_INDEX]), body);
                    break;
            }
        }
        // Multicast Data Restore Channel
        else if (channel == ChannelType.MDR) {
            switch (header[BackupProtocol.MESSAGE_TYPE_INDEX]) {
                case BackupProtocol.CHUNK_MESSAGE:
                    byte[] body = Utilities.extractBody(data, length);
                    handleRestoreChunk(header[BackupProtocol.FILE_ID_INDEX],
                            Integer.parseInt(header[BackupProtocol.CHUNK_NUMBER_INDEX]),
                            body);
                    break;
            }
        }
    }

    /**
     * Handle the stored chunk
     *
     * @param fileId      file id of the chunk
     * @param chunkNumber number of the chunk
     * @param deviceId    device that has mirrored the chunk
     */
    private synchronized void handleStoredChunk(final String fileId, final int chunkNumber, final String deviceId) {
        // Add stored confirmation in case it is listening to confirmations
        addStoredConfirmation(fileId, chunkNumber, deviceId);

        if (storedMessagesReceived.containsKey(fileId))
            if (storedMessagesReceived.get(fileId).containsKey(chunkNumber))
                storedMessagesReceived.get(fileId).put(chunkNumber, storedMessagesReceived.get(fileId).get(chunkNumber) + 1);

        if (!BackupService.getInstance().getDisk().hasChunk(fileId, chunkNumber)) {
            if (!this.storedListened.containsKey(fileId))
                this.storedListened.put(fileId, new HashMap<>());
            if (this.storedListened.get(fileId).containsKey(chunkNumber))
                this.storedListened.get(fileId).put(chunkNumber, this.storedListened.get(fileId).get(chunkNumber) + 1);
            else this.storedListened.get(fileId).put(chunkNumber, 1);
        }


        // Update replication degree if that is the case
        Chunk chunk = BackupService.getInstance().getDisk().getChunk(fileId, chunkNumber);
        if (chunk != null) {
            ChunkState chunkState = chunk.getState();
            System.out.println("antes" + chunkState.getReplicationDegree() + "/" + chunk.getChunkNo());
            chunkState.increaseReplicas(Integer.parseInt(deviceId));
            System.out.println("depois" + chunkState.getReplicationDegree() + "/" + chunk.getChunkNo());
            BackupService.getInstance().getDisk().updateChunkState(chunk);
        }
    }

    /**
     * Handle the putchunk message, with the enhacement
     *
     * @param fileId               file id of the chunk
     * @param chunkNumber          number of the chunk
     * @param minReplicationDegree minimum replication degree of the chunk
     * @param data                 data of the chunk
     */
    private synchronized void handlePutChunkEnh(final String fileId, final int chunkNumber, final int minReplicationDegree, final byte[] data) {
        // A peer must never store the chunks of its own files.
        if (isListeningStoredConfirmations(fileId, chunkNumber)) {
            return;
        }

        if (BackupService.getInstance().getDisk().filenames.containsValue(fileId))
            return;

        // Check if we were waiting the backup the chunk we are receiving
        if (chunksBackupAgain.containsKey(fileId)) {
            Map<Integer, BackupRemovedChunk> chunksToBackupAgain = chunksBackupAgain.get(fileId);
            if (chunksToBackupAgain.containsKey(chunkNumber)) {
                chunksToBackupAgain.get(chunkNumber).cancel();
                return;
            }
        }
        try {
            if (!this.storedListened.containsKey(fileId))
                this.storedListened.put(fileId, new HashMap<>());
            if (!this.storedListened.get(fileId).containsKey(chunkNumber))
                this.storedListened.get(fileId).put(chunkNumber, 0);
            Thread.sleep((int) (Math.random() * 400));
        } catch (InterruptedException ignore) {
        }
        if (this.storedListened.get(fileId).get(chunkNumber) < minReplicationDegree) {
            this.storedListened.get(fileId).remove(chunkNumber);
            if (this.storedListened.get(fileId).isEmpty())
                this.storedListened.remove(fileId);
            handlePutChunk(fileId, chunkNumber, minReplicationDegree, data);
        } else {
            this.storedListened.get(fileId).remove(chunkNumber);
            if (this.storedListened.get(fileId).isEmpty())
                this.storedListened.remove(fileId);
        }
    }

    /**
     * Handle the put chunk
     *
     * @param fileId               file id of the chunk
     * @param chunkNumber          number of the chunk
     * @param minReplicationDegree minimum replication degree of the chunk
     * @param data                 data of the chunk
     */
    private synchronized void handlePutChunk(final String fileId, final int chunkNumber, final int minReplicationDegree, final byte[] data) {
        // A peer must never store the chunks of its own files.
        if (isListeningStoredConfirmations(fileId, chunkNumber)) {
            return;
        }

        if (BackupService.getInstance().getDisk().filenames.containsValue(fileId))
            return;


        // Check if we were waiting the backup the chunk we are receiving
        if (chunksBackupAgain.containsKey(fileId)) {
            Map<Integer, BackupRemovedChunk> chunksToBackupAgain = chunksBackupAgain.get(fileId);
            if (chunksToBackupAgain.containsKey(chunkNumber)) {
                chunksToBackupAgain.get(chunkNumber).cancel();
                return;
            }
        }

        // Backup the received chunk
        Chunk chunk = new Chunk(fileId, chunkNumber, data, minReplicationDegree);

        chunk.getState().increaseReplicas(Integer.parseInt(BackupService.getInstance().getServerId()));

        // Check if chunk has been stored already
        if (BackupService.getInstance().getDisk().hasChunk(fileId, chunkNumber)) {
            Thread thread = new Thread(new StoredChunk(chunk));
            thread.start();
            return;
        }

        // Save the chunk to the disk
        if (!BackupService.getInstance().getDisk().saveChunk(chunk))
            return;

        // Send stored message
        Thread thread = new Thread(new StoredChunk(chunk));
        thread.start();
    }

    /**
     * Handle the get chunk. Initiates a restore chunk protocol.
     *
     * @param fileId      file id of the chunk
     * @param chunkNumber number of the chunk
     * @param address     address to send the file
     * @param port        port to send the file (-1 to use non enhanced)
     */
    private synchronized void handleGetChunk(final String fileId, final int chunkNumber, final InetAddress address, final int port) {
        Chunk chunk = BackupService.getInstance().getDisk().getChunk(fileId, chunkNumber);
        if (chunk == null)
            return;


        RestoreChunk restoreChunk = new RestoreChunk(chunk, (port != -1), address, port);

        if (!chunksForRestore.containsKey(chunk.getFileID()))
            chunksForRestore.put(chunk.getFileID(), new HashMap<>());
        chunksForRestore.get(chunk.getFileID()).put(chunk.getChunkNo(), restoreChunk);

        // Send restore chunk
        Thread thread = new Thread(restoreChunk);
        thread.start();
    }

    /**
     * Handle the restore chunk. When receives a restore chunk protocol message.
     *
     * @param fileId      file id of the chunk
     * @param chunkNumber number of the chunk
     * @param data        data of the chunk
     */
    private synchronized void handleRestoreChunk(final String fileId, final int chunkNumber, final byte[] data) {
        // Check if we were waiting to send this chunk for being restored
        if (chunksForRestore.containsKey(fileId)) {
            Map<Integer, RestoreChunk> chunks = chunksForRestore.get(fileId);
            if (chunks.containsKey(chunkNumber)) {
                chunks.get(chunkNumber).cancel();
                return;
            }
        }

        // Check if we were expecting the chunk to come
        if (!waitingForChunks.containsKey(fileId))
            return;

        final List<Integer> chunksWaiting = waitingForChunks.get(fileId);
        if (!chunksWaiting.contains(chunkNumber))
            return;

        ChunkState state = BackupService.getInstance().getDisk().getChunkState(fileId, chunkNumber);
        if (state == null)
            state = new ChunkState(1, 1);
        final Chunk chunk = new Chunk(fileId, chunkNumber, data, state);
        BackupService.getInstance().getDisk().saveToFile(chunk);

        chunksWaiting.remove(Integer.valueOf(chunkNumber));

        if (chunksWaiting.isEmpty()) {
            waitingForChunks.remove(fileId);
            BackupService.getInstance().sem.release();
        }

        //System.out.println("Restored the chunk successfully("+chunkNumber+")!");
    }

    /**
     * Handle the delete file. Deletes all the chunks of a file.
     *
     * @param fileId file id to delete all the chunks
     */
    private synchronized void handleDeleteFile(final String fileId) {
        if (!BackupService.getInstance().getDisk().removeChunks(fileId)) {
            System.out.println("Failed to delete a file from the backup!");
            return;
        }
        System.out.println("Deleted a file from the backup!");
    }

    /**
     * Handle the removed chunk
     *
     * @param fileId      file id of the chunk
     * @param chunkNumber number of the chunk
     * @param deviceId    device that has removed the chunk
     */
    private synchronized void handleRemovedChunk(final String fileId, final int chunkNumber, final String deviceId) {
        // Update replication degree if that is the case
        Chunk chunk = BackupService.getInstance().getDisk().getChunk(fileId, chunkNumber);
        if (chunk == null)
            return;

        chunk.getState().decreaseReplicas(Integer.parseInt(deviceId));
        BackupService.getInstance().getDisk().updateChunkState(chunk);

        // Check replication level
        if (chunk.getState().isSafe())
            return;

        System.out.println("fodeu - " + chunk.getState().getReplicationDegree());

        BackupRemovedChunk backupRemovedChunk = new BackupRemovedChunk(chunk);

        final Thread thread = new Thread(backupRemovedChunk);
        if (!chunksBackupAgain.containsKey(chunk.getFileID()))
            chunksBackupAgain.put(chunk.getFileID(), new HashMap<>());
        chunksBackupAgain.get(chunk.getFileID()).put(chunk.getChunkNo(), backupRemovedChunk);
        thread.start();
    }

    /**
     *
     *      STORED CONFIRMATIONS METHODS
     *
     */

    /**
     * Get the number of stored confirmations
     *
     * @param fileId      file id to get those
     * @param chunkNumber chunk number to get those
     * @return number of confirmations for the given chunk
     */
    public synchronized int getStoredConfirmations(final String fileId, final int chunkNumber) {
        if (!mirrorDevices.containsKey(fileId))
            return -1;

        Map<Integer, ChunkState> fileReplicasCount = mirrorDevices.get(fileId);
        if (!fileReplicasCount.containsKey(chunkNumber))
            return -1;

        return fileReplicasCount.get(chunkNumber).getReplicationDegree();
    }

    /**
     * Add a stored chunk confirmation
     *
     * @param fileId      file id to add the confirmation
     * @param chunkNumber chunk number to add the confirmation
     * @param deviceId    device id that has stored the chunk
     */
    private synchronized void addStoredConfirmation(final String fileId, final int chunkNumber, final String deviceId) {
        if (!mirrorDevices.containsKey(fileId))
            return;

        Map<Integer, ChunkState> fileReplicasCount = mirrorDevices.get(fileId);
        if (!fileReplicasCount.containsKey(chunkNumber))
            return;

        fileReplicasCount.get(chunkNumber).increaseReplicas(Integer.parseInt(deviceId));
    }

    /**
     * Listen to stored chunk confirmations
     *
     * @param fileId      file id to listen to those
     * @param chunkNumber chunk number to listen to those
     */
    public synchronized void listenStoredConfirmations(final String fileId, final int chunkNumber) {
        Map<Integer, ChunkState> fileReplicasCount;
        if (mirrorDevices.containsKey(fileId))
            fileReplicasCount = mirrorDevices.get(fileId);
        else
            fileReplicasCount = new HashMap<>();

        fileReplicasCount.put(chunkNumber, new ChunkState(-1, 0));
        mirrorDevices.put(fileId, fileReplicasCount);
    }

    /**
     * Stop listen to stored chunk confirmations
     *
     * @param fileId      file id to listen to those
     * @param chunkNumber chunk number to listen to those
     */
    public synchronized void stopListenStoredConfirmations(final String fileId, final int chunkNumber) {
        if (!mirrorDevices.containsKey(fileId))
            return;

        Map<Integer, ChunkState> fileReplicasCount = mirrorDevices.get(fileId);
        fileReplicasCount.remove(chunkNumber);

        if (fileReplicasCount.size() == 0)
            mirrorDevices.remove(fileId);
        else
            mirrorDevices.put(fileId, fileReplicasCount);
    }

    /**
     * Check if this peer is listening for stored confirmations for a given chunk
     *
     * @param fileId      file id of the chunk
     * @param chunkNumber number of the chunk
     * @return true if is listening, false otherwise
     */
    public synchronized boolean isListeningStoredConfirmations(final String fileId, final int chunkNumber) {
        if (!mirrorDevices.containsKey(fileId))
            return false;

        Map<Integer, ChunkState> fileReplicasCount = mirrorDevices.get(fileId);
        return fileReplicasCount.containsKey(chunkNumber);
    }
}