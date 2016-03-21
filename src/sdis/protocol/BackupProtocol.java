package sdis.protocol;

/**
 * Backup Protocol Definitions
 */
public interface BackupProtocol {

    /**
     *
     *      PROTOCOL INFORMATION
     *
     */

    /**
     * Version of the protocol
     */
    int VERSION = 1;

    /**
     * End character
     */
    String CRLF = "\n\r";

    /**
     *
     *          HEADER INDEXES
     *
     */

    /**
     * Message type field
     */
    int MESSAGE_TYPE_INDEX = 0;

    /**
     * Version field
     */
    int VERSION_INDEX = 1;

    /**
     * File Id field
     */
    int FILE_ID_INDEX = 2;

    /**
     * Chunk number field
     */
    int CHUNK_NUMBER_INDEX = 3;

    /**
     * Replication degree field
     */
    int REPLICATION_DEG_INDEX = 4;

    /**
     *
     *          MESSAGES
     *
     */

    /**
     * Put chunk message type
     */
    String PUTCHUNK_MESSAGE = "PUTCHUNK";

    /**
     * Stored message type
     */
    String STORED_MESSAGE = "STORED";

    /**
     * Get chunk message type
     */
    String GETCHUNK_MESSAGE = "GETCHUNK";

    /**
     * Chunk message type
     */
    String CHUNK_MESSAGE = "CHUNK";

    /**
     * Delete message type
     */
    String DELETE_MESSAGE = "DELETE";

    /**
     * Removed message type
     */
    String REMOVED_MESSAGE = "REMOVED";

    /**
     * Get the protocol message
     * @return protocol message
     */
    byte[] getMessage();
}