public class BackupService {

    public static void main(String[] args) {
        int argIndex = -1;

        // Create multicast channels
        MulticastChannel.createChannel("MC", args[++argIndex], Integer.parseInt(args[++argIndex]));
        MulticastChannel.createChannel("MDB", args[++argIndex], Integer.parseInt(args[++argIndex]));
        MulticastChannel.createChannel("MDR", args[++argIndex], Integer.parseInt(args[++argIndex]));
    }
}