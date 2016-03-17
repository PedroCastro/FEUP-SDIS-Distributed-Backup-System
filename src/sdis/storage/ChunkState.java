package sdis.storage;

import java.io.Serializable;

/**
 * Class to control the state of a chunk like the minimum replication degree
 * and the current replication degree.
 */
public class ChunkState implements Serializable {

    /**
     * Serial version of the ChunkState
     */
    private static final long serialVersionUID = 1317200560983715340L;

    /**
     * Minimum degree of replication
     */
    private final int minReplicationDegree;

    /**
     * Current replication degree
     */
    private int replicationDegree;

    /**
     * Constructor of ChunkState
     * @param minReplicationDegree minimum degree of replication
     * @param replicationDegree current replication degree
     */
    public ChunkState(final int minReplicationDegree, final int replicationDegree) {
        this.minReplicationDegree = minReplicationDegree;
        this.replicationDegree = replicationDegree;
    }

    /**
     * Get the minimum degree of replication
     * @return minimum degree of replication
     */
    public int getMinReplicationDegree() {
        return minReplicationDegree;
    }

    /**
     * Get the current degree of replication
     * @return current degree of replication
     */
    public int getReplicationDegree() {
        return replicationDegree;
    }

    /**
     * Check if the chunk is safe, that is, if the replication degree
     * is higher or equal than the minimum replication degree of the chunk
     * @return true if safe, false otherwise
     */
    public boolean isSafe() {
        return minReplicationDegree <= replicationDegree;
    }

    /**
     * Increase the replicas of the chunk
     */
    public void increaseReplicas() {
        this.replicationDegree++;
    }

    /**
     * Decrease the replicas of the chunk
     */
    public void decreaseReplicas() {
        this.replicationDegree--;
    }
}
