package sdis.protocol;

import sdis.BackupService;
import sdis.network.ChannelType;
import sdis.storage.Chunk;

/**
 * Stored chunk protocol
 */
public class StoredChunk implements BackupProtocol, Runnable {

    /**
     * Chunk that has been stored
     */
    private final Chunk chunk;

    /**
     * Constructor of StoredChunk
     *
     * @param chunk chunk that has been stored
     */
    public StoredChunk(final Chunk chunk) {
        this.chunk = chunk;
    }

    /**
     * Run method of the stored chunk
     */
    @Override
    public void run() {
        // Wait a random time between 0 and 400 before sending the stored message
        try {
            Thread.sleep((int) (Math.random() * 400));
        } catch (InterruptedException ignore) {
        }

        // Send stored chunk message
        byte[] message = getMessage();
        BackupService.getInstance().getChannelsHandler().sendMessage(message, ChannelType.MC);

        System.out.println("The chunk(" + chunk.getChunkNo() + ") is stored in the disk!");
    }

    /**
     * Get the stored chunk protocol message
     *
     * @return stored chunk protocol message
     */
    @Override
    public byte[] getMessage() {
        String header =
                BackupProtocol.STORED_MESSAGE + " "
                        + BackupProtocol.VERSION + " "
                        + BackupService.getInstance().getServerId() + " "
                        + chunk.getFileID() + " "
                        + chunk.getChunkNo()
                        + BackupProtocol.CRLF
                        + BackupProtocol.CRLF;
        return header.getBytes();
    }
}
