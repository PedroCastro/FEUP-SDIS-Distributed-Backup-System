package sdis.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * TCP Connection
 */
public class TCPChannel implements Channel {

    /**
     * Multicast Channel type
     */
    private final ChannelType type;

    /**
     * Address of the multicast socket
     */
    private final InetAddress address;

    /**
     * Address port of the multicast socket
     */
    private final int port;

    /**
     * Server socket
     */
    private final ServerSocket channelSocket;

    /**
     * Buffer where the data received will be written to
     */
    private byte[] buffer;

    /**
     * Constructor of MulticastChannel
     *
     * @param type type of the multicast channel
     * @throws IOException error when creating multicast socket
     */
    public TCPChannel(final ChannelType type) throws IOException {
        this.type = type;

        // Create server socket
        this.channelSocket = new ServerSocket(0);
        this.address = channelSocket.getInetAddress();
        this.port = channelSocket.getLocalPort();
    }

    /**
     * Get the type of the channel
     *
     * @return type of the channel
     */
    public ChannelType getType() {
        return type;
    }

    /**
     * Get the address of the channel
     *
     * @return address of the channel
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Get the port of the channel
     *
     * @return port of the channel
     */
    public int getPort() {
        return port;
    }

    /**
     * Read a packet from the channel
     *
     * @return DatagramPacket
     */
    public Object read() {
        // Cached items
        final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        this.buffer = new byte[1000];
        try {
            channelSocket.setSoTimeout(100); // Wait one second to read data
            Socket socket = channelSocket.accept();
            int bytesRead;
            do {
                bytesRead = socket.getInputStream().read(buffer);
                if (bytesRead != -1)
                    byteArray.write(buffer);
            } while (bytesRead != -1);
            if (byteArray.size() <= 0)
                return null;
            return byteArray.toByteArray();
        } catch (SocketTimeoutException e) {
            return null;
        } catch (IOException e) {
            System.out.println(type + ": Error while reading. " + e.getMessage());
            return null;
        }
    }

    /**
     * Write a message to the channel
     *
     * @param message message to be written
     * @return true if successful, false otherwise
     */
    public boolean write(final byte[] message) {
        return false;
    }

    /**
     * Close the tcp socket channel
     */
    public void close() {
        try {
            channelSocket.close();
        } catch (IOException e) {
            System.out.println(type + ": Error while closing tcp channel. " + e.getMessage());
        }
    }
}