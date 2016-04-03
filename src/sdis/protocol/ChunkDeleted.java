package sdis.protocol;

import sdis.BackupService;
import sdis.network.ChannelType;
import sdis.storage.Chunk;

/**
 * Created by Pedro Castro on 02/04/2016.
 */
public class ChunkDeleted implements BackupProtocol, Runnable {

    private Chunk chunk;
    /**
     * Constructor of ChunkDeleted
     *
     * @param chunk chunk that has been stored
     */
    public ChunkDeleted(final Chunk chunk) {
        this.chunk = chunk;
    }

    /**
     * Run method of the deleted chunk
     */
    @Override
    public void run() {
        // Send delete file message
        byte[] message = getMessage();
        BackupService.getInstance().getChannelsHandler().sendMessage(message, ChannelType.MC);
    }
    /**
     * Get the deleted chunk protocol message
     *
     * @return deleted chunk protocol message
     */
    @Override
    public byte[] getMessage() {
        String header =
                BackupProtocol.DELETED_MESSAGE + " "
                        + BackupProtocol.VERSION_ENHANCEMENT + " "
                        + BackupService.getInstance().getServerId() + " "
                        + chunk.getFileID() + " "
                        + chunk.getChunkNo()
                        + BackupProtocol.CRLF
                        + BackupProtocol.CRLF;
        return header.getBytes();
    }
}
