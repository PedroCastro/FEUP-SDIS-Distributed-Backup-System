package sdis.storage;

import sdis.BackupService;
import sdis.protocol.RemoveChunk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
     * Map with all files and chunks saved as well as their state
     * <FileId, <ChunkNo, ChunkState>>
     */
    private ConcurrentHashMap<String, ConcurrentHashMap <Integer, ChunkState>> files;

    /**
     *  HashMap to be able to retrieve the id from the filename
     */
    public Map<String,String> filenames;

    /**
     *  HashMap to be able to retrieve the number of chunks of a file
     */
    public Map<String,Integer> filesizes;

    /**
     * Constructor of Disk
     *
     * @param capacity capacity of the disk
     */
    public Disk(int capacity) {
        this.capacityBytes = capacity;
        this.usedBytes = 0;
        this.files = new ConcurrentHashMap <>();
        this.filenames = new HashMap<>();
        this.filesizes = new HashMap<>();
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
     *
     * @param bytes number of bytes to be added to capacity
     */
    public synchronized void addCapacity(final int bytes) {
        capacityBytes += bytes;

        // Save the disk
        this.saveDisk();
    }

    /**
     * Set the capacity of the disk
     *
     * @param bytes number of bytes of the capacity
     */
    public synchronized void setCapacity(final int bytes) {
        capacityBytes = bytes;

        // Save the disk
        this.saveDisk();
    }

    /**
     * Get a chunk from the disk
     *
     * @param fileHash    file hash of the chunk
     * @param chunkNumber chunk number
     * @return chunk with that file hash and chunk number
     */
    public synchronized Chunk getChunk(final String fileHash, final int chunkNumber) {
        if (!hasChunk(fileHash, chunkNumber))
            return null;

        // Chunk state
        ChunkState state = files.get(fileHash).get(chunkNumber);

        // Get chunk from the disk
        File chunkFile = new File(BackupService.getInstance().getServerId().toString()+"data" + File.separator + fileHash + File.separator + chunkNumber + ".bin");
        byte[] data = new byte[(int) chunkFile.length()];
        try {
            new FileInputStream(chunkFile).read(data);
        } catch (Exception e) {
            System.out.println("Error while fetching chunk from the disk! " + e.getMessage());
            return null;
        }

        return new Chunk(fileHash, chunkNumber, data, state);
    }

    /**
     * Get the chunk state of a chunk
     *
     * @param fileHash    file hash of the chunk
     * @param chunkNumber number of the chunk
     * @return chunk state of the chunk
     */
    public synchronized ChunkState getChunkState(final String fileHash, int chunkNumber) {
        if (!files.containsKey(fileHash))
            return null;
        if (!files.get(fileHash).containsKey(chunkNumber))
            return null;
        return files.get(fileHash).get(chunkNumber);
    }

    /**
     * Check if a chunk is on the disk
     *
     * @param fileHash    hash of the file to check
     * @param chunkNumber number of the chunk to check
     * @return true if chunk file exists, false otherwise
     */
    public synchronized boolean hasChunk(final String fileHash, final int chunkNumber) {
        if (!files.containsKey(fileHash))
            return false;
        return files.get(fileHash).containsKey(chunkNumber);
    }

    /**
     * Save a chunk to the disk
     * @param chunk to be added
     */
    public synchronized boolean saveChunk(final Chunk chunk) {
        if (chunk == null)
            return false;

        // Check free space
        if (chunk.getData().length > getFreeBytes()) {
            System.out.println("Not enough space in disk!");
            return false;
        }

        // Save chunk in the disk
        try {

            File dir = new File(BackupService.getInstance().getServerId().toString()+"data" + File.separator + chunk.getFileID());

            File chunkFile = new File(dir.toString() + File.separator + chunk.getChunkNo() + ".bin");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            FileOutputStream f = new FileOutputStream(chunkFile);
            BufferedOutputStream chunkFileOutputStream = new BufferedOutputStream(f);
            chunkFileOutputStream.write(chunk.getData());
            chunkFileOutputStream.close();
            f.close();
        } catch (IOException e) {
            System.out.println("Failed to save chunk to the disk! " + e.getMessage());
            return false;
        }

        // Added used space
        usedBytes += chunk.getData().length;

        // Save in chunk's map
        if (!files.containsKey(chunk.getFileID()))
            files.put(chunk.getFileID(), new ConcurrentHashMap<>());

        files.get(chunk.getFileID()).put(chunk.getChunkNo(), chunk.getState());

        // Save the disk
        this.saveDisk();

        return true;
    }

    /**
     * Delete all the chunks of a file
     *
     * @param fileHash file hash to delete all the chunks
     * @return true if successfull, false otherwise
     */
    public synchronized boolean removeChunks(final String fileHash) {
        if (!files.containsKey(fileHash))
            return true;

        // Remove all chunks
        final Map<Integer, ChunkState> chunks = files.get(fileHash);
        for (Map.Entry<Integer, ChunkState> entry : new HashMap<>(chunks).entrySet()) {
            if (!removeChunk(fileHash, entry.getKey()))
                return false;
        }

        return true;
    }

    /**
     * Remove a chunk from the disk
     *
     * @param fileHash    file hash of the chunk
     * @param chunkNumber chunk number to be removed
     * @return true if successfull, false otherwise
     */
    public synchronized boolean removeChunk(final String fileHash, final int chunkNumber) {
        return removeChunk(getChunk(fileHash, chunkNumber));
    }

    /**
     * Remove a chunk from the disk
     *
     * @param chunk chunk to be removed
     * @return true if chunk was removed, false otherwise
     */
    public synchronized boolean removeChunk(final Chunk chunk) {
        if (chunk == null)
            return false;
        // Check disk space
        if (chunk.getData().length > getUsedBytes()) {
            System.out.println("Removing more bytes than the ones being used!");
            return false;
        }

        // Check if chunk exists
        if (!hasChunk(chunk.getFileID(), chunk.getChunkNo()))
            return false;

        File chunkFile = new File(BackupService.getInstance().getServerId().toString()+"data" + File.separator + chunk.getFileID()+ File.separator + chunk.getChunkNo() + ".bin");
        System.gc();

        if (!chunkFile.delete())
                return false;

        // Delete file folder if no more chunks are stored
        File fileFolder = chunkFile.getParentFile();
        if (fileFolder.list().length <= 0)
            if (!fileFolder.delete())
                return false;

        // Add free space
        usedBytes -= chunk.getData().length;

        // Remove in chunk's map
        files.get(chunk.getFileID()).remove(chunk.getChunkNo());

        //Remove file from file's map is no more chunks exist
        if(files.get(chunk.getFileID()).isEmpty())
            files.remove(chunk.getFileID());

        // Save the disk
        this.saveDisk();

        return true;
    }

    /**
     * Update the state of a chunk
     *
     * @param chunk chunk to be updated
     */
    public synchronized void updateChunkState(final Chunk chunk) {
        files.get(chunk.getFileID()).put(chunk.getChunkNo(), chunk.getState());

        // Save the disk
        this.saveDisk();
    }

    /**
     * Add the id of a file to hashtable with its filename as key
     * @param filename of the file
     * @param id of the file
     */
    public synchronized void addFilename(String filename, String id){
        filenames.put(filename,id);
        this.saveDisk();
    }

    /**
     * Returns the id of the file with the given filename
     * @param filename of the file
     * @return id of the file
     */
    public synchronized String getId(String filename){
        return filenames.get(filename);
    }

    /**
     * Add the number of chunks of a given file
     * @param id of the file
     * @param size - number of chunks of the file
     */
    public synchronized void addNumberOfChunks(String id, int size)
    {
        filesizes.put(id,size);
        this.saveDisk();
    }

    /**
     * Returns the number of chunks of a file
     * @param id of the file
     * @return number of chunks of the file
     */
    public synchronized int getNumberOfChunks(String id){
        return filesizes.get(id);
    }

    /**
     * Saves the current Disk to Disk File
     */
    public synchronized void saveDisk(){
        BackupService.getInstance().saveDisk();
    }

    /**
     * Frees the given space in the disk
     * @param space to be free
     * @return true if the given space was made free, false if not
     */
    public synchronized boolean freeSpace(int space){
        if(space > usedBytes)
            return false;

        int minFreeSpace = usedBytes - space;

        while(usedBytes > minFreeSpace)
        {
            for(ConcurrentHashMap .Entry<String, ConcurrentHashMap <Integer, ChunkState>> filesEntry : files.entrySet())//iterate through files
                for(ConcurrentHashMap .Entry<Integer, ChunkState> chunksEntry : filesEntry.getValue().entrySet())//iterate chunkStates
                {
                    Chunk chunk = getChunk(filesEntry.getKey(),chunksEntry.getKey());
                    ChunkState state = chunksEntry.getValue();
                    if(state.getReplicationDegree()>state.getMinReplicationDegree())
                        removeChunk(chunk);

                    (new RemoveChunk(chunk)).run();
                }
        }

        return true;
    }

    public synchronized void printInfo(){
        System.out.println("Disk - f:" + this.getFreeBytes() + "b / u:" + this.getUsedBytes() + "b / c:" + this.getCapacity() + "b");
    }
}