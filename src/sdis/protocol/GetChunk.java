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
     * Enhancement boolean
     */
    private boolean enhanced;

    /**
     * TCP port
     */
    private int port;

    /**
     * Constructor of GetChunk
     *
     * @param chunk chunk to be backed up
     */
    public GetChunk(final Chunk chunk) {
        this(chunk, false, -1);
    }

    /**
     * Constructor of GetChunk
     *
     * @param chunk    chunk to be backed up
     * @param enhanced true to use the enhanced protocol, false otherwise
     * @param port     port of the TCP server
     */
    public GetChunk(final Chunk chunk, final boolean enhanced, final int port) {
        this.chunk = chunk;
        this.enhanced = enhanced;
        this.port = port;
    }

    /**
     * Run method of the get chunk
     */
    @Override
    public void run() {
        // Send get chunk message
        byte[] message = getMessage();
        BackupService.getInstance().getChannelsHandler().sendMessage(message, ChannelType.MC);

        //System.out.println("Retrieving a chunk!");

        boolean finished = false;
        int currentAttempt = 1;
        while (!finished) {
            if (currentAttempt > 5)
                break;
            int index = -1;
            if (BackupService.getInstance().getChannelsHandler().waitingForChunks.containsKey(chunk.getFileID()))
                index = BackupService.getInstance().getChannelsHandler().waitingForChunks.get(chunk.getFileID()).indexOf(chunk.getChunkNo());

            if (index != -1) {
                try {
                    //System.out.println("Waiting for stored chunk confirmations...");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                currentAttempt++;
                BackupService.getInstance().getChannelsHandler().sendMessage(message, ChannelType.MC);
            } else finished = true;
        }
    }

    /**
     * Get the get chunk protocol message
     *
     * @return get chunk protocol message
     */
    @Override
    public byte[] getMessage() {
        String header =
                BackupProtocol.GETCHUNK_MESSAGE + " "
                        + (enhanced ? BackupProtocol.VERSION_ENHANCEMENT : BackupProtocol.VERSION) + " "
                        + BackupService.getInstance().getServerId() + " "
                        + chunk.getFileID() + " "
                        + chunk.getChunkNo()
                        + (enhanced ? " " + port : "")
                        + BackupProtocol.CRLF
                        + BackupProtocol.CRLF;
        return header.getBytes();
    }
}
