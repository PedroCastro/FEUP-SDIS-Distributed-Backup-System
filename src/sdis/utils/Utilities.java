package sdis.utils;

import sdis.protocol.BackupProtocol;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

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

    /**
     * Extract the header of a message
     *
     * @param message message to be extract the header
     * @return extracted header
     */
    public static String[] extractHeader(final byte[] message) {
        ByteArrayInputStream stream = new ByteArrayInputStream(message);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        try {
            return reader.readLine().split(" ");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Extract the body of a message
     *
     * @param message message to get its body extracted
     * @return extracted body
     */
    public static byte[] extractBody(final byte[] message) {
        ByteArrayInputStream stream = new ByteArrayInputStream(message);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        String line = null;
        int headerLinesLengthSum = 0;
        int numLines = 0;

        do {
            try {
                line = reader.readLine();

                headerLinesLengthSum += line.length();

                numLines++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (line != null && !line.isEmpty());

        int bodyStartIndex = headerLinesLengthSum + numLines * BackupProtocol.CRLF.getBytes().length;

        return Arrays.copyOfRange(message, bodyStartIndex, message.length);
    }
}
