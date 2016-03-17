package sdis.utils;

/**
 * Utilities class
 */
public class Utilities {
    /**
     * Concatenate two byte arrays
     * @param a first byte array
     * @param b second byte array
     * @return concatenated byte array
     */
    public static byte[] concatBytes(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;

        byte[] c = new byte[aLen + bLen];

        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }
}
