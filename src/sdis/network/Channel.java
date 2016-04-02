package sdis.network;

import java.net.InetAddress;

/**
 * Network channel interface
 */
public interface Channel {

    /**
     * Maximum size per packet
     */
    int MAX_SIZE_PACKET = 65000;

    /**
     * Get the type of the channel
     *
     * @return type of the channel
     */
    ChannelType getType();

    /**
     * Get the address of the channel
     *
     * @return address of the channel
     */
    InetAddress getAddress();

    /**
     * Get the port of the channel
     *
     * @return port of the channel
     */
    int getPort();

    /**
     * Read a packet from the channel
     *
     * @return read object
     */
    Object read();

    /**
     * Write a message to the channel
     *
     * @param message message to be written
     * @return true if successful, false otherwise
     */
    boolean write(final byte[] message);

    /**
     * Close the socket channel
     */
    void close();
}
