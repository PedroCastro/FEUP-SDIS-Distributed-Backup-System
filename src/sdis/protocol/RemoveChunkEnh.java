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
     * MinimalFreeSpace
     */
    private final int minFreeSpace;

    /**
     * Constructor of RemoveChunk
     *
     * @param chunk chunk that was removed
     */
    public RemoveChunkEnh(final Chunk chunk, final int minFreeSpace) {
        this.chunk = chunk;
        this.minFreeSpace = minFreeSpace;
    }

    /**
     * Run method of the remove chunk
     */
    @Override
    public void run() {



        int waitingForStoredTime = 500;
        int currentAttempt = 1;

        byte[] message = getMessage();

        byte[] putChunkMessage = (new BackupChunk(chunk, false)).getMessage();

        //initiate storing listening
        if (!BackupService.getInstance().getChannelsHandler().storedMessagesReceived.containsKey(chunk.getFileID()))
            BackupService.getInstance().getChannelsHandler().storedMessagesReceived.put(chunk.getFileID(), new HashMap<>());

        BackupService.getInstance().getChannelsHandler().storedMessagesReceived.get(chunk.getFileID()).put(chunk.getChunkNo(), 0);

        BackupService.getInstance().getChannelsHandler().sendMessage(putChunkMessage, ChannelType.MDB);

        while (currentAttempt <= 3) {

            currentAttempt++;

            //wait for stored messages
            try {
                Thread.sleep(waitingForStoredTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int storedListen = 0;
            if(BackupService.getInstance().getChannelsHandler().storedMessagesReceived.get(chunk.getFileID()).containsKey(chunk.getChunkNo()))
                storedListen = BackupService.getInstance().getChannelsHandler().storedMessagesReceived.get(chunk.getFileID()).get(chunk.getChunkNo());


            System.out.println("Stored Listen : " + storedListen + " of " + chunk.getChunkNo());
            if (storedListen >= chunk.getState().getMinReplicationDegree()) {
                System.out.println("Enhaced removal of chunk " + chunk.getFileID() + " - " + chunk.getChunkNo());
                BackupService.getInstance().getDisk().removeChunk(chunk);
                BackupService.getInstance().getChannelsHandler().sendMessage(message, ChannelType.MC);
                BackupService.getInstance().getChannelsHandler().storedMessagesReceived.get(chunk.getFileID()).remove(chunk.getChunkNo());
                return;
            }
        }

        System.out.println("Couldnt remove chunk");
        BackupService.getInstance().getChannelsHandler().storedMessagesReceived.get(chunk.getFileID()).remove(chunk.getChunkNo());


        /*
        BackupService.getInstance().getChannelsHandler().sendMessage(message, ChannelType.MC);
        outerLoop:
        while (true) {

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
                numberOfPutChunks = BackupService.getInstance().getChannelsHandler().putChunkListener.get(chunk.getFileID()).get(chunk.getChunkNo());
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
                    } else {
                        return;
                    }
                    attempt++;
                    BackupService.getInstance().getChannelsHandler().storedMessagesReceived.get(chunk.getFileID()).put(chunk.getChunkNo(), 0);
                }
            }

        }
        System.out.println("Enhaced removal of chunk " + chunk.getFileID() + " - " + chunk.getChunkNo());
        BackupService.getInstance().getChannelsHandler().putChunkListener.get(chunk.getFileID()).remove(chunk.getChunkNo());
        BackupService.getInstance().getChannelsHandler().storedMessagesReceived.get(chunk.getFileID()).remove(chunk.getChunkNo());

        BackupService.getInstance().getDisk().removeChunk(chunk);

        */
    }

    @Override
    public byte[] getMessage() {
        String header =
                BackupProtocol.REMOVED_MESSAGE + " "
                        + BackupProtocol.VERSION_ENHANCEMENT + " "
                        + BackupService.getInstance().getServerId() + " "
                        + chunk.getFileID() + " "
                        + chunk.getChunkNo()
                        + BackupProtocol.CRLF
                        + BackupProtocol.CRLF;
        return header.getBytes();
    }

}
