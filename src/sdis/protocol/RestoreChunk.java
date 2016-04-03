package sdis.protocol;

import sdis.BackupService;
import sdis.network.ChannelType;
import sdis.storage.Chunk;
import sdis.utils.Utilities;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Restore chunk protocol
 */
public class RestoreChunk implements BackupProtocol, Runnable {

    /**
     * Chunk to be backed up
     */
    private final Chunk chunk;
    /**
     * Address to send the restore chunk
     */
    private final InetAddress address;
    /**
     * Port to send the chunk (if enhanced)
     */
    private final int port;
    /**
     * Flag to restore or not the chunk
     */
    private final AtomicBoolean restore;
    /**
     * Enhancement boolean
     */
    private boolean enhanced;

    /**
     * Constructor of BackupChunk
     *
     * @param chunk    chunk to be backed up
     * @param enhanced true if enhanced
     * @param port     port to send the chunk
     */
    public RestoreChunk(final Chunk chunk, final boolean enhanced, final InetAddress address, final int port) {
        this.chunk = chunk;
        this.enhanced = enhanced;
        this.address = address;
        this.port = port;
        this.restore = new AtomicBoolean(true);
    }

    /**
     * Run method of the backup chunk
     */
    @Override
    public void run() {
        try {
            Thread.sleep((int) (Math.random() * 400));
        } catch (InterruptedException ignore) {
        }

        if (!restore.get())
            return;

        byte[] message = getMessage();
        BackupService.getInstance().getChannelsHandler().sendMessage(message, ChannelType.MDR);

        // Directly connect to TCP server
        if (enhanced) {
            try {
                Socket clientSocket = new Socket(address, port);
                DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
                // Get the message this time with the body
                enhanced = false;
                message = getMessage();
                outToServer.write(message);
                outToServer.close();
                clientSocket.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Cancel the restore chunk
     */
    public void cancel() {
        this.restore.set(false);
    }

    /**
     * Get the restore chunk protocol message
     *
     * @return restore chunk protocol message
     */
    @Override
    public byte[] getMessage() {
        String header =
                BackupProtocol.CHUNK_MESSAGE + " "
                        + BackupProtocol.VERSION + " "
                        + BackupService.getInstance().getServerId() + " "
                        + chunk.getFileID() + " "
                        + chunk.getChunkNo()
                        + BackupProtocol.CRLF
                        + BackupProtocol.CRLF;
        return (enhanced ? header.getBytes() : Utilities.concatBytes(header.getBytes(), chunk.getData()));
    }
}
