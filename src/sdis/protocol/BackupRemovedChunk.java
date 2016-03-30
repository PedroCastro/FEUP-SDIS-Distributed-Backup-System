package sdis.protocol;

import sdis.storage.Chunk;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Backup a removed chunk protocol
 */
public class BackupRemovedChunk implements BackupProtocol, Runnable {

    /**
     * Chunk to be backed up again
     */
    private final Chunk chunk;

    /**
     * Flag to backed or not the chunk
     */
    private final AtomicBoolean backup;

    /**
     * Constructor of BackupRemovedChunk
     *
     * @param chunk chunk to be backed up
     */
    public BackupRemovedChunk(final Chunk chunk) {
        this.chunk = chunk;
        this.backup = new AtomicBoolean(true);
    }

    /**
     * Run method of the backup removed chunk
     */
    @Override
    public void run() {
        try {
            Thread.sleep((int) (Math.random() * 400));
        } catch (InterruptedException ignore) {
        }

        if (!backup.get())
            return;

        System.out.println("Backing up a removed chunk("+chunk.getChunkNo()+") because count dropped below the desired replication!");

        new BackupChunk(chunk,true).run();
    }

    /**
     * Cancel the backup removed chunk
     */
    public void cancel() {
        this.backup.set(false);
    }

    /**
     * Get the backup chunk protocol message
     *
     * @return backup chunk protocol message
     */
    @Override
    public byte[] getMessage() {
        return new byte[0];
    }
}
