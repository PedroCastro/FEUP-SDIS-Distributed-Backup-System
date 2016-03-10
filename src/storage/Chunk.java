package storage;

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
     * Constructor of Chunk
     * @param fileID identification of the file
     * @param chunkNo number of the chunk
     * @param data data in the chunk
     */
    public Chunk(String fileID, int chunkNo, byte[] data) {
        this.fileID = fileID;
        this.chunkNo = chunkNo;
        this.data = data;
    }

    /**
     * Get the file identification of the chunk
     * @return file identification of the chunk
     */
    public String getFileID() {
        return fileID;
    }

    /**
     * Get the number of the chunk
     * @return number of the chunk
     */
    public int getChunkNo() {
        return chunkNo;
    }

    /**
     * Get the data in the chunk
     * @return data in the chunk
     */
    public byte[] getData() {
        return data;
    }
}
