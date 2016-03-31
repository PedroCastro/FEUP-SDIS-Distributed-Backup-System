package sdis.storage;

/**
 * Chunk Object
 */
public class Chunk {

    /**
     * Identification of the file
     */
    private final String fileID;

    /**
     * Number of the chunk
     */
    private final int chunkNo;

    /**
     * Data in the chunk
     */
    private final byte[] data;

    /**
     * State of the chunk
     */
    private final ChunkState state;

    /**
     * Constructor of Chunk
     *
     * @param fileID  identification of the file
     * @param chunkNo number of the chunk
     * @param data    data in the chunk
     * @param state   state of the chunk
     */
    public Chunk(final String fileID, final int chunkNo, final byte[] data, final ChunkState state) {
        this(fileID, chunkNo, data, state.getMinReplicationDegree(), state.getReplicationDegree());
        this.state.mirrorDevices = state.mirrorDevices;
    }

    /**
     * Constructor of Chunk
     *
     * @param fileID               identification of the file
     * @param chunkNo              number of the chunk
     * @param data                 data in the chunk
     * @param minReplicationDegree minimum degree of replication
     */
    public Chunk(final String fileID, final int chunkNo, final byte[] data, final int minReplicationDegree) {
        this(fileID, chunkNo, data, minReplicationDegree, 0);
    }

    /**
     * Constructor of Chunk
     *
     * @param fileID               identification of the file
     * @param chunkNo              number of the chunk
     * @param data                 data in the chunk
     * @param minReplicationDegree minimum degree of replication
     * @param replicationDegree    replication degree of the chunk
     */
    public Chunk(final String fileID, final int chunkNo, final byte[] data, final int minReplicationDegree, final int replicationDegree) {
        this.fileID = fileID;
        this.chunkNo = chunkNo;
        this.data = data;
        this.state = new ChunkState(minReplicationDegree, replicationDegree);
    }

    /**
     * Get the file identification of the chunk
     *
     * @return file identification of the chunk
     */
    public String getFileID() {
        return fileID;
    }

    /**
     * Get the number of the chunk
     *
     * @return number of the chunk
     */
    public int getChunkNo() {
        return chunkNo;
    }

    /**
     * Get the data in the chunk
     *
     * @return data in the chunk
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Get the chunk state
     *
     * @return state of the chunk
     */
    public ChunkState getState() {
        return state;
    }
}