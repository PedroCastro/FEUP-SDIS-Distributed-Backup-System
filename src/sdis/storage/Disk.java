package sdis.storage;

import sdis.BackupService;
import sdis.protocol.ChunkDeleted;
import sdis.protocol.RemoveChunk;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Disk
 */
public class Disk implements Serializable {

    /**
     * Serial version of Disk
     */
    private static final long serialVersionUID = 8516322467343602642L;
    /**
     * HashMap to be able to retrieve the id from the filename
     */
    public Map<String, String> filenames;
    /**
     * Set of ids this peer has started
     */
    public ArrayList<String> idSet;
    /**
     * HashMap to be able to retrieve the number of chunks of a file
     */
    public Map<String, Integer> filesizes;
    Semaphore sem = new Semaphore(1);

    /**
     * Map of all chunks on the system
     */
    private Map<String, Map<Integer, ChunkState>> mirrorDevices;


    /**
     * Used bytes of the disk
     */
    private int usedBytes;
    /**
     * Map with all files and chunks saved as well as their state
     * <FileId, <ChunkNo, ChunkState>>
     */
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, ChunkState>> files;

    /**
     * Constructor of Disk
     *
     */
    public Disk() {
        this.usedBytes = 0;
        this.files = new ConcurrentHashMap<>();
        this.filenames = new HashMap<>();
        this.filesizes = new HashMap<>();
        this.mirrorDevices = new HashMap<>();
        this.idSet = new ArrayList<>();
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
        File chunkFile = new File(BackupService.getInstance().getServerId().toString() + "data" + File.separator + fileHash + File.separator + chunkNumber + ".bin");
        byte[] data = new byte[(int) chunkFile.length()];
        try {
            FileInputStream fis = new FileInputStream(chunkFile);
            fis.read(data);
            fis.close();

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
     * File a chunk to a file
     *
     * @param chunk chunk to be saved
     */
    public synchronized void saveToFile(final Chunk chunk) {
        try {
            RandomAccessFile file = new RandomAccessFile(chunk.getFileID(), "rw");
            file.seek(FileChunker.getMaxSizeChunk() * chunk.getChunkNo());
            file.write(chunk.getData());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save a chunk to the disk
     *
     * @param chunk to be added
     */
    public synchronized boolean saveChunk(final Chunk chunk) {
        if (chunk == null)
            return false;

        // Save chunk in the disk
        try {

            File dir = new File(BackupService.getInstance().getServerId().toString() + "data" + File.separator + chunk.getFileID());

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
        Map<Integer, ChunkState> chunks = files.get(fileHash);
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
        Chunk chunk = getChunk(fileHash, chunkNumber);
        if (chunk != null)
            return removeChunk(getChunk(fileHash, chunkNumber));
        else return true;
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

        File chunkFile = new File(BackupService.getInstance().getServerId().toString() + "data" + File.separator + chunk.getFileID() + File.separator + chunk.getChunkNo() + ".bin");
        System.gc();

        if (!chunkFile.delete())
            return false;

        BackupService.getInstance().getChannelsHandler().decreaseStoredConfirmation(chunk.getFileID(),chunk.getChunkNo(),(new Integer(BackupService.getInstance().getServerId())).toString());

        //BackupService.getInstance().getChannelsHandler().stopListenStoredConfirmations(chunk.getFileID(),chunk.getChunkNo());

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
        if (files.get(chunk.getFileID()).isEmpty())
            files.remove(chunk.getFileID());

        // Save the disk
        this.saveDisk();

        ChunkDeleted chunkDeleted = new ChunkDeleted(chunk);

        chunkDeleted.run();

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
     *
     * @param filename of the file
     * @param id       of the file
     */
    public synchronized void addFilename(String filename, String id) {
        filenames.put(filename, id);
        this.saveDisk();
    }

    /**
     * Remove filename from hashmap
     *
     * @param filename to be removed
     */
    public synchronized void removeFilename(String filename) {
        filenames.remove(filename);
        this.saveDisk();
    }

    /**
     * Returns the id of the file with the given filename
     *
     * @param filename of the file
     * @return id of the file
     */
    public synchronized String getId(String filename) {
        return filenames.get(filename);
    }

    /**
     * Add the number of chunks of a given file
     *
     * @param id   of the file
     * @param size - number of chunks of the file
     */
    public synchronized void addNumberOfChunks(String id, int size) {
        filesizes.put(id, size);
        this.saveDisk();
    }

    /**
     * Returns the number of chunks of a file
     *
     * @param id of the file
     * @return number of chunks of the file
     */
    public synchronized int getNumberOfChunks(String id) {
        return filesizes.get(id);
    }

    /**
     * Saves the current Disk to Disk File
     */
    public synchronized void saveDisk() {
        BackupService.getInstance().saveDisk();
    }

    /**
     * Frees the given space in the disk
     *
     * @param space to be free
     * @return true if the given space was made free, false if not
     */
    public synchronized boolean freeSpace(int space) {
        if (space > usedBytes)
            return false;

        int minFreeSpace = usedBytes - space;

        //So it can run simultaneous
        Thread thread = new Thread() {
            public void run() {
                outerLoop:
                {
                    for (ConcurrentHashMap.Entry<String, ConcurrentHashMap<Integer, ChunkState>> filesEntry : files.entrySet())//iterate through files
                        for (ConcurrentHashMap.Entry<Integer, ChunkState> chunksEntry : filesEntry.getValue().entrySet())//iterate chunkStates
                        {
                            Chunk chunk = getChunk(filesEntry.getKey(), chunksEntry.getKey());
                            ChunkState state = chunksEntry.getValue();
                            if (state.getReplicationDegree() > state.getMinReplicationDegree()) {
                                if (removeChunk(chunk))
                                    (new RemoveChunk(chunk)).run();
                            }
                            if (usedBytes <= minFreeSpace)
                                break outerLoop;
                        }
                    outerLoop2:
                    for (ConcurrentHashMap.Entry<String, ConcurrentHashMap<Integer, ChunkState>> filesEntry : files.entrySet())//iterate through files
                        for (ConcurrentHashMap.Entry<Integer, ChunkState> chunksEntry : filesEntry.getValue().entrySet())//iterate chunkStates
                        {
                            Chunk chunk = getChunk(filesEntry.getKey(), chunksEntry.getKey());
                            if (removeChunk(chunk))
                                (new RemoveChunk(chunk)).run();
                            if (usedBytes <= minFreeSpace)
                                break outerLoop2;
                        }
                }
            }
        };

        thread.start();


        return true;
    }

    public synchronized void printInfo() {
        System.out.println("Disk - f:" + this.getUsedBytes() + "b");
    }

    /**
     * Get the mirrorDevices variable
     * @return mirrorDevices
     */
    public synchronized Map<String, Map<Integer, ChunkState>> getMirrorDevices() {
        return mirrorDevices;
    }

    /**
     * Setes the variable mirrorDevices
     * @param mirrorDevices
     */
    public synchronized void setMirrorDevices(Map<String, Map<Integer, ChunkState>> mirrorDevices){
        this.mirrorDevices = mirrorDevices;
        saveDisk();
    }
}