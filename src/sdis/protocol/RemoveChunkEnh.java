package sdis.protocol;

import sdis.BackupService;
import sdis.network.ChannelType;
import sdis.storage.Chunk;

import java.util.HashMap;

/**
 * Enhanced remove chunk protocol
 */
public class RemoveChunkEnh implements BackupProtocol, Runnable {
    /**
     * Chunk that was removed
     */
    private final Chunk chunk;

    /**
     * Constructor of RemoveChunk
     *
     * @param chunk chunk that was removed
     */
    public RemoveChunkEnh(final Chunk chunk) {
        this.chunk = chunk;
    }

    /**
     * Run method of the remove chunk
     */
    @Override
    public void run() {
        int currentWaitingTime = 500;
        int currentAttempt = 1;
        boolean finished = false;

        byte[] message = getMessage();

        BackupService.getInstance().getChannelsHandler().sendMessage(message, ChannelType.MC);
        outerLoop:
        while (!finished) {

            if (chunk.getState().isSafe()) {
                System.out.println("safe");
                break outerLoop;
            }

            if (currentAttempt > 3) {
                System.out.println("Couldnt remove chunk Bigger - " + chunk.getChunkNo() + "/" + chunk.getState().getReplicationDegree());
                return;
            }

            if (!BackupService.getInstance().getChannelsHandler().putChunkListener.containsKey(chunk.getFileID()))
                BackupService.getInstance().getChannelsHandler().putChunkListener.put(chunk.getFileID(), new HashMap<>());

            BackupService.getInstance().getChannelsHandler().putChunkListener.get(chunk.getFileID()).put(chunk.getChunkNo(), 0);

            //initiate stored too
            if (!BackupService.getInstance().getChannelsHandler().storedMessagesReceived.containsKey(chunk.getFileID()))
                BackupService.getInstance().getChannelsHandler().storedMessagesReceived.put(chunk.getFileID(), new HashMap<>());

            BackupService.getInstance().getChannelsHandler().storedMessagesReceived.get(chunk.getFileID()).put(chunk.getChunkNo(), 0);

            currentAttempt++;
            try {
                Thread.sleep(currentWaitingTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int numberOfPutChunks = 0;
            if (BackupService.getInstance().getChannelsHandler().putChunkListener.get(chunk.getFileID()).containsKey(chunk.getChunkNo()))
                numberOfPutChunks = Integer.valueOf(BackupService.getInstance().getChannelsHandler().putChunkListener.get(chunk.getFileID()).get(chunk.getChunkNo()));
            else return;
            if (numberOfPutChunks > 0) {
                System.out.println("Entra aqui :" + numberOfPutChunks);
                int attempt = 1;
                while (attempt < 3) {
                    try {
                        Thread.sleep(currentWaitingTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (BackupService.getInstance().getChannelsHandler().storedMessagesReceived.get(chunk.getFileID()).containsKey(chunk.getChunkNo())) {
                        if (BackupService.getInstance().getChannelsHandler().storedMessagesReceived.get(chunk.getFileID()).get(chunk.getChunkNo()) > 0) {
                            System.out.println("stored messages : " + BackupService.getInstance().getChannelsHandler().storedMessagesReceived.get(chunk.getFileID()).get(chunk.getChunkNo()));
                            break outerLoop;
                        }
                    } else return;
                    attempt++;
                    BackupService.getInstance().getChannelsHandler().storedMessagesReceived.get(chunk.getFileID()).put(chunk.getChunkNo(), 0);
                }
            }

        }
        System.out.println("Enhaced removal of chunk " + chunk.getFileID() + " - " + chunk.getChunkNo());
        BackupService.getInstance().getChannelsHandler().putChunkListener.get(chunk.getFileID()).remove(chunk.getChunkNo());
        BackupService.getInstance().getChannelsHandler().storedMessagesReceived.get(chunk.getFileID()).remove(chunk.getChunkNo());

        BackupService.getInstance().getDisk().removeChunk(chunk);
    }

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
