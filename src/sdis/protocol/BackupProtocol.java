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
     * Version of the enhaced protocol
     */
    int VERSION_ENHANCEMENT = 2;

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
     * Sender of the message index
     */
    int SENDER_INDEX = 2;

    /**
     * File Id field
     */
    int FILE_ID_INDEX = 3;

    /**
     * Chunk number field
     */
    int CHUNK_NUMBER_INDEX = 4;

    /**
     * Replication degree field
     */
    int REPLICATION_DEG_INDEX = 5;

    /**
     * TCP port field
     */
    int TCP_PORT = 5;

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
     * Deleted Chunk Message Type
     */
    String DELETED_MESSAGE = "DELETEDCHUNK";

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
     *
     * @return protocol message
     */
    byte[] getMessage();
}