package sdis.protocol;

import sdis.BackupService;
import sdis.network.ChannelType;
import sdis.storage.Chunk;
import sdis.utils.Utilities;

/**
 * Backup chunk protocol
 */
public class BackupChunk implements Runnable {

    /**
     * Initial waiting time for responses in millis
     */
    private static final int INITIAL_WAITING_TIME = 1000;

    /**
     * Maximum number of attempts to backup the chunk
     */
    private static final int MAX_ATTEMPTS = 5;

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
        int currentWaitingTime = INITIAL_WAITING_TIME;
        int currentAttempt = 1;
        boolean finished = false;

        byte[] message = getMessage();

        while(!finished) {
            // Listen for stored confirmations
            BackupService.getInstance().getChannelsHandler().listenStoredConfirmations(chunk.getFileID(), chunk.getChunkNo());

            // Send backup chunk message
            BackupService.getInstance().getChannelsHandler().sendMessage(message, ChannelType.MDB);

            // Wait confirmations
            try {
                System.out.println("Waiting for stored chunk confirmations...");
                Thread.sleep(currentWaitingTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Check number confirmations
            int numberConfirmations = BackupService.getInstance().getChannelsHandler().getStoredConfirmations(chunk.getFileID(), chunk.getChunkNo());
            if(numberConfirmations < chunk.getState().getMinReplicationDegree()) {
                currentAttempt++;

                if(currentAttempt > MAX_ATTEMPTS) {
                    System.out.println("Could not get the minimum replication degree for the chunk!");
                    finished = true;
                } else {
                    System.out.println("Chunk haven't got the desired replication degree, trying again!");
                    currentWaitingTime *= 2;
                }
            } else {
                System.out.println("Chunk got the minimum replication degree desired!");
                finished = true;
            }

            // Stop listen to stored confirmations
            BackupService.getInstance().getChannelsHandler().stopListenStoredConfirmations(chunk.getFileID(), chunk.getChunkNo());
        }
    }

    /**
     * Get the backup chunk protocol message
     * @return backup chunk protocol message
     */
    public byte[] getMessage() {
        String header =
                BackupProtocol.PUTCHUNK_MESSAGE
                + BackupProtocol.VERSION + " "
                + BackupService.getInstance().getServerId() + " "
                + chunk.getFileID() + " "
                + chunk.getChunkNo() + " "
                + chunk.getState().getMinReplicationDegree()
                + BackupProtocol.CRLF
                + BackupProtocol.CRLF;
        return Utilities.concatBytes(header.getBytes(), chunk.getData());
    }
}
