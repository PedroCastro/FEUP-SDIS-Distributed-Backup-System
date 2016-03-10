import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Multicast Connection
 */
public class MulticastChannel {

    /**
     * List with all multicast channels
     */
    private static List<MulticastChannel> multicastChannels = new ArrayList<>();

    /**
     * Create a multicast channel
     * @param name name of the channel
     * @param address address name of the channel
     * @param port port of the channel
     * @return multicast channel
     */
    public static MulticastChannel createChannel(final String name, final String address, final int port) {
        try {
            return new MulticastChannel(name, InetAddress.getByName(address), port);
        } catch (IOException e) {
            System.out.println(name + ": Error while creating channel. " + e.getMessage());
        }
        return null;
    }

    /**
     * Get a multicast channel by its name
     * @param channelName name of the channel
     * @return channel with that name
     */
    public static MulticastChannel getChannelByName(final String channelName) {
        for(MulticastChannel channel : multicastChannels)
            if(channel.getName().equalsIgnoreCase(channelName))
                return channel;
        return null;
    }

    /**
     * Maximum size per packet
     */
    private static final int MAX_SIZE_PACKET = 64000;

    /**
     * Multicast Channel name
     */
    private final String name;

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
     * @param name name of the multicast channel
     * @param address address of the multicast channel
     * @param port port of the multicast channel
     * @throws IOException error when creating multicast socket
     */
    private MulticastChannel(final String name, final InetAddress address, final int port) throws IOException {
        this.name = name;
        this.address = address;
        this.port = port;

        // Join the multicast channel
        this.multiCastSocket = new MulticastSocket(port);
        this.multiCastSocket.joinGroup(address);

        // Cached items
        this.buffer = new byte[MAX_SIZE_PACKET];
        this.dataPacket = new DatagramPacket(buffer, buffer.length);

        // Add to multicast channels
        multicastChannels.add(this);
    }

    /**
     * Get the name of the channel
     * @return name of the channel
     */
    public String getName() {
        return name;
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
            multiCastSocket.receive(dataPacket);
        } catch (IOException e) {
            System.out.println(name + ": Error while reading. " + e.getMessage());
        }
        return dataPacket.getData();
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
            System.out.println(name + ": Error while writing. " + e.getMessage());
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
            System.out.println(name + ": Error while leaving group. " + e.getMessage());
        }
        multiCastSocket.close();

        // Remove from channels list
        multicastChannels.remove(this);
    }
}