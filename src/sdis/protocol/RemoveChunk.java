package sdis.protocol;

import sdis.BackupService;
import sdis.network.ChannelType;
import sdis.storage.Chunk;

/**
 * Remove chunk protocol
 */
public class RemoveChunk implements BackupProtocol, Runnable {

    /**
     * Chunk that was removed
     */
    private final Chunk chunk;

    /**
     * Constructor of RemoveChunk
     *
     * @param chunk chunk that was removed
     */
    public RemoveChunk(final Chunk chunk) {
        this.chunk = chunk;
    }

    /**
     * Run method of the remove chunk
     */
    @Override
    public void run() {
        // Send remove chunk message
        byte[] message = getMessage();
        BackupService.getInstance().getChannelsHandler().sendMessage(message, ChannelType.MC);

        System.out.println("Removed a chunk from the backup system!");
    }

    /**
     * Get the removed chunk protocol message
     *
     * @return removed chunk protocol message
     */
    @Override
    public byte[] getMessage() {
        String header =
                BackupProtocol.REMOVED_MESSAGE + " "
                        + BackupProtocol.VERSION + " "
                        + BackupService.getInstance().getServerId() + " "
                        + chunk.getFileID() + " "
                        + chunk.getChunkNo()
                        + BackupProtocol.CRLF
                        + BackupProtocol.CRLF;
        return header.getBytes();
    }
}
