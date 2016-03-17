package sdis.protocol;

import sdis.storage.Chunk;

/**
 * Backup chunk protocol
 */
public class BackupChunk implements Runnable {

    /**
     * Version of the protocol
     */
    private static int VERSION = 1;

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

    }
}
