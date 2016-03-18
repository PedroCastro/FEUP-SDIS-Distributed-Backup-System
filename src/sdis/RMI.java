package sdis;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Pedro Castro on 18/03/2016.
 */
public interface RMI extends Remote{

    String test()throws RemoteException;
    //TODO por aqui as funcoes do send putchunks e getchunks

}
