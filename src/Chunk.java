/**
 * Created by Pedro Castro on 10/03/2016.
 */
public class Chunk {


        String fileID;

        int chunkNo;

        byte[] data;


    public Chunk(String fileID,int chunkNo,byte[] data){
        this.fileID = fileID;
        this.chunkNo = chunkNo;
        this.data = data;
    }

}
