package sdis.storage;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * File chunker
 */
public class FileChunker {

    /**
     * Max Size of Chunk
     */
    static final int MAX_SIZE_CHUNK = 64000;

    /**
     * Chunk a file
     * @param file file to chunk
     * @return list with all the chunks of the file
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static ArrayList<Chunk> chunkFile(File file) throws NoSuchAlgorithmException, IOException {
        int part = 0;
        ArrayList<Chunk> chunkList = new ArrayList<>();

        String id = getFileChecksum(file);
        if(id.equalsIgnoreCase("error"))
            return null;

        byte[] chunk = new byte[MAX_SIZE_CHUNK];
        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));

        //TODO ultimo chunk tem que ter tamanho 0 se o file tiver tamanho certo

        while ((inputStream.read(chunk)) > 0) {
            Chunk newChunk = new Chunk(id, part++, chunk, 0);
            chunkList.add(newChunk);
        }

        return chunkList;
    }

    /**
     * Get the file checksum in SHA-256
     * @param file file to get the file checksum
     * @return SHA-256 file checksum
     */
    public static String getFileChecksum(File file) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] bytesBuffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
                digest.update(bytesBuffer, 0, bytesRead);
            }

            byte[] hashedBytes = digest.digest();

            return convertByteArrayToHexString(hashedBytes);
        } catch (NoSuchAlgorithmException | IOException error) {
            System.out.println("Error while hashing : " + error.getMessage());
            return "error";
        }
    }

    /**
     * Convert a byte array to a hexadecimal string
     * @param bytes bytes array to be converted
     * @return converted hexadecimal string
     */
    private static String convertByteArrayToHexString(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte aByte : bytes) {
            stringBuilder.append(Integer.toString((aByte & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return stringBuilder.toString();
    }

    /**
     * Method to return the maximum chunk size
     * @return Maximum chunk size
     */
    public static int getMaxSizeChunk() {
        return MAX_SIZE_CHUNK;
    }
}
