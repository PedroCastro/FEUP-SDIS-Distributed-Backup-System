package sdis.protocol;

import sdis.BackupService;
import sdis.network.ChannelType;
import sdis.storage.Chunk;

/**
 * Get chunk protocol
 */
public class GetChunk implements BackupProtocol, Runnable {

    /**
     * Chunk to be retrieved
     */
    private final Chunk chunk;

    /**
     * Constructor of GetChunk
     * @param chunk chunk to be retrieved
     */
    public GetChunk(final Chunk chunk) {
        this.chunk = chunk;
    }

    /**
     * Run method of the get chunk
     */
    @Override
    public void run() {
        // Send get chunk message
        byte[] message = getMessage();
        BackupService.getInstance().getChannelsHandler().sendMessage(message, ChannelType.MC);

        System.out.println("Retrieving a chunk!");
    }

    /**
     * Get the get chunk protocol message
     * @return get chunk protocol message
     */
    @Override
    public byte[] getMessage() {
        String header =
                BackupProtocol.GETCHUNK_MESSAGE
                + BackupProtocol.VERSION + " "
                + BackupService.getInstance().getServerId() + " "
                + chunk.getFileID() + " "
                + chunk.getChunkNo()
                + BackupProtocol.CRLF
                + BackupProtocol.CRLF;
        return header.getBytes();
    }
}
