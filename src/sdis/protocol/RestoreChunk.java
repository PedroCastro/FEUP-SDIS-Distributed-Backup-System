package sdis.protocol;

import sdis.BackupService;
import sdis.network.ChannelType;
import sdis.storage.Chunk;
import sdis.utils.Utilities;

/**
 * Restore chunk protocol
 */
public class RestoreChunk implements BackupProtocol, Runnable {

    /**
     * Chunk to be backed up
     */
    private Chunk chunk;

    /**
     * Constructor of BackupChunk
     *
     * @param chunk chunk to be backed up
     */
    public RestoreChunk(final Chunk chunk) {
        this.chunk = chunk;
    }

    /**
     * Run method of the backup chunk
     */
    @Override
    public void run() {
        try {
            Thread.sleep((int)(Math.random() * 400));
        } catch (InterruptedException ignore) {
        }

        byte[] message = getMessage();
        BackupService.getInstance().getChannelsHandler().sendMessage(message, ChannelType.MDR);

        System.out.println("Sent chunk to be restored");
    }

    /**
     * Get the backup chunk protocol message
     *
     * @return backup chunk protocol message
     */
    @Override
    public byte[] getMessage() {
        String header =
                BackupProtocol.CHUNK_MESSAGE
                        + BackupProtocol.VERSION + " "
                        + BackupService.getInstance().getServerId() + " "
                        + chunk.getFileID() + " "
                        + chunk.getChunkNo()
                        + BackupProtocol.CRLF
                        + BackupProtocol.CRLF;
        return Utilities.concatBytes(header.getBytes(), chunk.getData());
    }
}