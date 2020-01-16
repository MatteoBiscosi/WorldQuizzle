package mbiscosi.wq.server;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface ConnectionInterface extends Remote{
	
	//Metodo per connettersi al server tramite rmi -- Login --
	public String registrazione(String username, String password) throws RemoteException;
}
