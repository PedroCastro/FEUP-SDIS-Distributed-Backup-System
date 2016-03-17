package sdis.protocol;

import sdis.BackupService;
import sdis.storage.Chunk;
import sdis.utils.Utilities;

/**
 * Backup chunk protocol
 */
public class BackupChunk implements Runnable {

    /**
     * Chunk to be backed up
     */
    private Chunk chunk;

    /**
     * Constructor of BackupChunk
     * @param chunk chunk to be backed up
     */
    public BackupChunk(final Chunk chunk) {
        this.chunk = chunk;
    }

    /**
     * Run method of the backup chunk
     */
    @Override
    public void run() {
        byte[] message = getMessage();
    }

    /**
     * Get the backup chunk protocol message
     * @return backup chunk protocol message
     */
    public byte[] getMessage() {
        String header = "PUTCHUNK "
                + BackupService.VERSION + " "
                + BackupService.getInstance().getServerId() + " "
                + chunk.getFileID() + " "
                + chunk.getChunkNo() + " "
                + chunk.getState().getMinReplicationDegree()
                + BackupService.CLRF
                + BackupService.CLRF;
        return Utilities.concatBytes(header.getBytes(), chunk.getData());
    }
}
