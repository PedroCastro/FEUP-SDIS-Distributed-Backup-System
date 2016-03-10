package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

/**
 * Multicast Connection
 */
public class MulticastChannel {

    /**
     * Maximum size per packet
     */
    private static final int MAX_SIZE_PACKET = 64000;

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
     * Multicast socket
     */
    private final MulticastSocket multiCastSocket;

    /**
     * Buffer where the data received will be written to
     */
    private final byte[] buffer;

    /**
     * Data packet to received the data from the channel
     */
    private final DatagramPacket dataPacket;

    /**
     * Constructor of MulticastChannel
     * @param type type of the multicast channel
     * @param address address of the multicast channel
     * @param port port of the multicast channel
     * @throws IOException error when creating multicast socket
     */
    public MulticastChannel(final ChannelType type, final InetAddress address, final int port) throws IOException {
        this.type = type;
        this.address = address;
        this.port = port;

        // Join the multicast channel
        this.multiCastSocket = new MulticastSocket(port);
        this.multiCastSocket.joinGroup(address);

        // Cached items
        this.buffer = new byte[MAX_SIZE_PACKET];
        this.dataPacket = new DatagramPacket(buffer, buffer.length);
    }

    /**
     * Get the type of the channel
     * @return type of the channel
     */
    public ChannelType getType() {
        return type;
    }

    /**
     * Get the address of the channel
     * @return address of the channel
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Get the port of the channel
     * @return port of the channel
     */
    public int getPort() {
        return port;
    }

    /**
     * Read a packet from the multicast channel
     * @return read a packet from the channel
     */
    public byte[] read() {
        Arrays.fill(buffer, (byte) 0); // Clear buffer
        try {
            multiCastSocket.setSoTimeout(1000); // Wait one second to read data
            multiCastSocket.receive(dataPacket);
            return dataPacket.getData();
        } catch (SocketTimeoutException e) {
            return null;
        } catch (IOException e) {
            System.out.println(type + ": Error while reading. " + e.getMessage());
            return null;
        }
    }

    /**
     * Write a message to the multicast channel
     * @param message message to be written
     * @return true if successfull, false otherwise
     */
    public boolean write(final byte[] message) {
        try {
            multiCastSocket.send(new DatagramPacket(message, message.length, address, port));
            return true;
        } catch (IOException e) {
            System.out.println(type + ": Error while writing. " + e.getMessage());
            return false;
        }
    }

    /**
     * Close the multicast socket channel
     */
    public void close() {
        try {
            multiCastSocket.leaveGroup(address);
        } catch (IOException e) {
            System.out.println(type + ": Error while leaving group. " + e.getMessage());
        }
        multiCastSocket.close();
    }
}