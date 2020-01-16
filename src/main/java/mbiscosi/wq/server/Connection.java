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

	public String registrazione(String username, String password) throws RemoteException {
		if(server.getUtenti().containsKey(username))
			return "Utente già presente, scegliere un altro username";
		
		
		server.getJson().scriviJSON(username, password, null);
		server.getConnessioni().put(username, new UserInfo(password, 0));
		server.getUtenti().put(username, new ArrayList<String>());
		
		
		server.stampaConnessioni();
		server.stampaUtenti();
		
		return "Utente registrato";
	}
	
}
