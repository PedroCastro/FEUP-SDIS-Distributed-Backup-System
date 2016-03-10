import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * Created by Pedro Castro on 10/03/2016.
 */
public class FileChunker {

    /**
     * Max Size of Chunk
     */
    private static final int MAX_SIZE_CHUNK = 64000;


    public static ArrayList<Chunk> chunkFile(File file) throws NoSuchAlgorithmException, IOException {

        int part = 0;

        ArrayList<Chunk> chunkList = new ArrayList<>();

        String id = getFileChecksum(file);

        byte[] chunk = new byte[MAX_SIZE_CHUNK];

        BufferedInputStream inputStream = new BufferedInputStream (new FileInputStream(file));

        //TODO ultimo chunk tem que ter tamanho 0 se o file tiver tamanho certo
        //int temp;

        while((inputStream.read(chunk)) > 0){
            Chunk newChunk = new Chunk(id,part++,chunk);
            chunkList.add(newChunk);
        }
        return chunkList;
    }
    public static String getFileChecksum(File file) throws NoSuchAlgorithmException, IOException {

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
    private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < arrayBytes.length; i++) {
            stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return stringBuffer.toString();
    }
}
