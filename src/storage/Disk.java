package storage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Disk
 */
public class Disk {

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
     * @param capacity capacity of the disk
     */
    public Disk(int capacity) {
        this.capacityBytes = capacity;
        this.usedBytes = 0;
        this.files = new HashMap<>();
    }

    /**
     * Get the capscity of the disk
     * @return capacity of the disk
     */
    public synchronized int getCapacity() {
        return capacityBytes;
    }

    /**
     * Get the used bytes of the disk
     * @return used bytes of the disk
     */
    public synchronized int getUsedBytes() {
        return usedBytes;
    }

    /**
     * Get the free bytes of the disk
     * @return free bytes of the disk
     */
    public synchronized int getFreeBytes() {
        return capacityBytes - usedBytes;
    }

    public synchronized void saveFile(long bytes) {
        if (bytes > getFreeBytes())
            System.out.println("Not enough space in disk!");
        else
            usedBytes += bytes;
    }

    public synchronized void removeFile(long bytes) {
        if (bytes > getUsedBytes())
            System.out.println("Removing more bytes than the ones being used!");
        else
            usedBytes -= bytes;
    }

    public synchronized void addCapacity(int bytes) {
        capacityBytes += bytes;
    }

    public synchronized void setCapacity(int bytes) {
        capacityBytes = bytes;
    }
}
