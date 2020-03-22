package mbiscosi.wq.server;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface ConnectionInterface extends Remote{
/*
 * Interfaccia utilizzata per gestire la registrazione di un nuovo utente, tramite RMI
 */
	
	/*
	 * REGISTRAZIONE
	 * 
	 * metodo che sfrutta la RMI, per effettuare la registrazione di un nuovo utente.
	 * 
	 * Returns: ritorna il messaggio di andata a buon fine o meno della registrazione.
	 * 
	 * Exceptions: RemoteException, lanciata in caso in cui il servizio non sia attivo al momento
	 */
	public String registrazione(String username, String password) throws RemoteException;
}
