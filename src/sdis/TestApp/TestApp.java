package sdis.TestApp;

import sdis.RMI;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Created by Pedro Castro on 18/03/2016.
 */
public class TestApp {
    /**
     * Main method of the TestApp
     *
     * @param args arguments sent to the console
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Please execute the backup service using the following format:");
            System.out.println("java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2> ");
            return;
        }
        RMI rmi;
        try {
            Registry registry = LocateRegistry.getRegistry(null);

            rmi = (RMI) registry.lookup(args[0]);
        }
        catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
            return;
        }

        switch(args[1]){
            case "BACKUP":
                if (args.length < 4) {
                    System.out.println("Please execute the backup service using the following format:");
                    System.out.println("java TestApp <peer_ap> BACKUP <file_path> <rep_degree> ");
                    return;
                }
                String response = rmi.test();
                System.out.println(response);
                //TODO chunck's and SENDPUTCHUNKS
                break;
            case "RESTORE":
                if (args.length < 4) {
                    System.out.println("Please execute the backup service using the following format:");
                    System.out.println("java TestApp <peer_ap> BACKUP <file_path> <rep_degree> ");
                    return;
                }
                //TODO Send getChunks, and rechunk
                break;
            case "DELETE":
                if (args.length < 4) {
                    System.out.println("Please execute the backup service using the following format:");
                    System.out.println("java TestApp <peer_ap> BACKUP <file_path> <rep_degree> ");
                    return;
                }
                break;
            case "RECLAIM":
                break;
        }

    }
}
