package sdis.protocol;

/**
 * Backup Protocol Definitions
 */
public class BackupProtocol {

    /**
     *
     *      PROTOCOL INFORMATION
     *
     */

    /**
     * Version of the protocol
     */
    public final static int VERSION = 1;

    /**
     * End character
     */
    public final static String CRLF = "\n\r";

    /**
     *
     *          HEADER INDEXES
     *
     */

    /**
     * Message type field
     */
    public static final int MESSAGE_TYPE_INDEX = 0;

    /**
     * Version field
     */
    public static final int VERSION_INDEX = 1;

    /**
     * File Id field
     */
    public static final int FILE_ID_INDEX = 2;

    /**
     * Chunk number field
     */
    public static final int CHUNK_NO_INDEX = 3;

    /**
     * Replication degree field
     */
    public static final int REPLICATION_DEG_INDEX = 4;

    /**
     *
     *          MESSAGES
     *
     */

    /**
     * Put chunk message type
     */
    public static final String PUTCHUNK_MESSAGE = "PUTCHUNK";

    /**
     * Stored message type
     */
    public static final String STORED_MESSAGE = "STORED";

    /**
     * Get chunk message type
     */
    public static final String GETCHUNK_MESSAGE = "GETCHUNK";

    /**
     * Chunk message type
     */
    public static final String CHUNK_MESSAGE = "CHUNK";

    /**
     * Delete message type
     */
    public static final String DELETE_MESSAGE = "DELETE";

    /**
     * Removed message type
     */
    public static final String REMOVED_MESSAGE = "REMOVED";
}
