package mbiscosi.wq.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;



public class Connection extends UnicastRemoteObject implements ConnectionInterface{

	private static final long serialVersionUID = 1L;
	ServerService server;
	
	

	protected Connection(ServerService server) throws RemoteException {
		super();
		this.server = server;
	}

	/*
	 * REGISTRAZIONE
	 * 
	 * metodo che sfrutta la RMI, per effettuare la registrazione di un nuovo utente.
	 * 
	 * Returns: ritorna il messaggio di andata a buon fine o meno della registrazione.
	 * 
	 * Exceptions: RemoteException, lanciata in caso in cui il servizio non sia attivo al momento
	 */
	public String registrazione(String username, String password) throws RemoteException {
		if(server.getUtenti().containsKey(username))
			return "Utente già presente, scegliere un altro username";
		
		server.getJson().getLock().lock();
		server.getJson().scriviJSON(username, password, null, null);
		server.getJson().getLock().unlock();
		server.getConnessioni().put(username, new UserInfo(password, 0));
		server.getUtenti().put(username, new ArrayList<String>());
		
		
		return "Utente registrato";
	}
	
}
