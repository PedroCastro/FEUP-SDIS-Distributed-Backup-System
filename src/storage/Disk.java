package storage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Disk
 */
public class Disk implements Serializable {

    /**
     * Serial version of Disk
     */
    private static final long serialVersionUID = 8516322467343602642L;

    /**
     * Capacity bytes of the disk
     */
    private int capacityBytes;

    /**
     * Used bytes of the disk
     */
    private int usedBytes;

    /**
     * List with all files and chunks saved
     */
    private Map<String, Set<Integer>> files;

    /**
     * Constructor of Disk
     *
     * @param capacity capacity of the disk
     */
    public Disk(int capacity) {
        this.capacityBytes = capacity;
        this.usedBytes = 0;
        this.files = new HashMap<>();
    }

    /**
     * Get the capscity of the disk
     *
     * @return capacity of the disk
     */
    public synchronized int getCapacity() {
        return capacityBytes;
    }

    /**
     * Get the used bytes of the disk
     *
     * @return used bytes of the disk
     */
    public synchronized int getUsedBytes() {
        return usedBytes;
    }

    /**
     * Get the free bytes of the disk
     *
     * @return free bytes of the disk
     */
    public synchronized int getFreeBytes() {
        return capacityBytes - usedBytes;
    }

    /**
     * Add capacity to the disk
     * @param bytes number of bytes to be added to capacity
     */
    public synchronized void addCapacity(int bytes) {
        capacityBytes += bytes;
    }

    /**
     * Set the capacity of the disk
     * @param bytes number of bytes of the capacity
     */
    public synchronized void setCapacity(int bytes) {
        capacityBytes = bytes;
    }

    /**
     * Save a chunk to the disk
     * @param chunk to be added
     */
    public synchronized boolean saveChunk(Chunk chunk) {
        // Check free space
        if (chunk.getData().length > getFreeBytes()) {
            System.out.println("Not enough space in disk!");
            return false;
        }

        // TODO Save chunk in the disk

        // Added used space
        usedBytes += chunk.getData().length;

        // Save in chunk's map
        if(!files.containsKey(chunk.getFileID()))
            files.put(chunk.getFileID(), new HashSet<>());

        files.get(chunk.getFileID()).add(chunk.getChunkNo());
        return true;
    }

    /**
     * Remove a chunk from the disk
     * @param fileHash file hash of the chunk
     * @param chunkNumber chunk number to be removed
     * @return true if successfull, false otherwise
     */
    public synchronized boolean removeChunk(String fileHash, int chunkNumber) {
        // TODO get chunk from the disk
        Chunk chunk = new Chunk(null, 0, null);
        return removeChunk(chunk);
    }

    /**
     * Remove a chunk from the disk
     * @param chunk chunk to be removed
     * @return true if chunk was removed, false otherwise
     */
    public synchronized boolean removeChunk(Chunk chunk) {
        // Check disk space
        if (chunk.getData().length > getUsedBytes()) {
            System.out.println("Removing more bytes than the ones being used!");
            return false;
        }

        // Check if chunk exists
        if(!files.containsKey(chunk.getFileID()))
            return false;

        // TODO Remove chunk in the disk

        // Add free space
        usedBytes -= chunk.getData().length;

        // Remove in chunk's map
        files.get(chunk.getFileID()).remove(chunk.getChunkNo());
        return true;
    }

    /**
     * Check if a chunk is on the disk
     * @param fileHash hash of the file to check
     * @param chunkNumber number of the chunk to check
     * @return true if chunk file exists, false otherwise
     */
    public synchronized boolean hasChunk(String fileHash, int chunkNumber) {
        if(!files.containsKey(fileHash))
            return false;
        return files.get(fileHash).contains(chunkNumber);
    }
}
