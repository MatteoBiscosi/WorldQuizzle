package mbiscosi.wq.server;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;

public class ProcessRequest {
	
	private String request;
	private String response;
	private ServerService server;
	private SelectableChannel channel;
	private SelectionKey key;
	
	
	public ProcessRequest(String request, ServerService server, SelectableChannel channel, SelectionKey key) {
		this.request = request;
		this.server = server;
		this.channel = channel;
		this.key = key;
	}
	
	
	
	public String process() {
		String[] splitting;
		
		try {
			splitting = request.split(" ");
		} catch(NullPointerException e) {
			return "Errore nel login";
		}
		
		
		switch(splitting[0]) {
		case "login":
			login(splitting[1], splitting[2]);
			break;

			
		case "logout":
			logout(splitting[1]);
			break;
			
			
		case "aggiungiAmico":
			aggiungiAmico(splitting[1], splitting[2]);
			break;
			
			
		case "listaAmici":
			listaAmici(splitting[1]);
			break;
		
			
		case "mostraPunteggio":
			mostraPunteggio(splitting[1]);
			break;
			
			
		case "mostraClassifica":
			mostraClassifica(splitting[1]);
			break;	
		}
		
		

		return response;
	}
	
	
	
	
	
	
	
	/*
	 * OPERAZIONE DI LOGIN:
	 * Abbiamo i seguenti possibili casi: 
	 * - Username o password sbagliati (ritorno un messaggio di risposta adeguato)
	 * - Utente già connesso (ritorno un messaggio di risposta adeguato e non gli permetto il login)
	 * - Username e password corretti (ritorno un messaggio affermativo e imposto la var. "connesso" della hashmap ad 1)
	 */
	
	public void login(String username, String password) {
		UserInfo tmp;
		
		
		if((tmp = server.getConnessioni().get(username)) != null) {
			if(tmp.getPassword().equals(password)) {
				if(tmp.getConnesso() == 1) {
					response = "-2";
					return;
				}
				tmp.setConnesso(1);
				tmp.setSocketChannel(channel);
				response = "1";
				return;
			}
		}
		
		
		response = "0";
	}
	
	
	
	
	/*
	 * OPERAZIONE DI LOGOUT:
	 * abbiamo i seguenti possibili casi:
	 * - username esiste ed è connesso (lo disconnetto, settando il socket channel a null e connesso a 0)
	 * - username non esiste (ritorno un messaggio di errore adeguato)
	 * - username esiste ma utente non connesso (ritorno messaggio di errore adeguato)
	 */
	public void logout(String username) {
		UserInfo tmp;
		
		
		if((tmp = server.getConnessioni().get(username)) != null) {
			
			if(tmp.getConnesso() == 1) {
				tmp.setConnesso(0);
				tmp.setSocketChannel(null);
				response = "1";
				return;
			}
			

			response = "-2";
			return;
		}
		
		response = "0";
	}
	
	
	
	
	
	/*
	 * OPERAZIONE DI AGGIUNTA AMICO:
	 * abbiamo i seguenti possibili casi:
	 * - username ha gia come amico userAmico (ritorno messaggio di errore adeguato)
	 * - username non esiste (ritorno un messaggio di errore adeguato)
	 * - username esiste ma utente non connesso (ritorno messaggio di errore adeguato)
	 * - username e userAmico esistono e l'aggiunta va a buon fine (ritorno messaggio di successo)
	 * - userAmico non esiste (ritorno messaggio di errore adeguato)
	 */
	public void aggiungiAmico(String username, String userAmico) {
		ArrayList<String> tmp;
		
		tmp = server.getUtenti().get(username);
		
		if(!server.getUtenti().containsKey(userAmico))
			response = "0";
		
		else if(tmp.contains(userAmico))
			response = "-2";
		
		else {
			response = "1";
			server.getJson().scriviJSON(username, null, userAmico);
			tmp.add(userAmico);
		}
	}
	
	
	
	
	
	/*
	 * OPERAZIONE DI AGGIUNTA AMICO:
	 * abbiamo i seguenti possibili casi:
	 * - username ha gia come amico userAmico (ritorno messaggio di errore adeguato)
	 * - username non esiste (ritorno un messaggio di errore adeguato)
	 * - username esiste ma utente non connesso (ritorno messaggio di errore adeguato)
	 * - username e userAmico esistono e l'aggiunta va a buon fine (ritorno messaggio di successo)
	 * - userAmico non esiste (ritorno messaggio di errore adeguato)
	 */
	public void listaAmici(String username) {
		ArrayList<String> tmp;
		
		tmp = server.getUtenti().get(username);
		
		response = server.getJson().scriviJSONAmici(tmp, 0);
		
		response = response.length() + "\r\n" + response;
	}
	
	
	
	
	
	
	/*
	 * OPERAZIONE DI MOSTRA PUNTEGGIO:
	 * restituisco il punteggio dell'utente username. L'esistenza dell'username è garantita dal
	 * fatto che ha potuto fare il login
	 */
	public void mostraPunteggio(String username) {
		UserInfo tmp = server.getConnessioni().get(username);
		
		response = Integer.toString(tmp.getPunteggio());
	}
	
	
	
	
	/*
	 * OPERAZIONE DI MOSTRA CLASSIFICA:
	 * restituisco la classifica tra gli utenti amici di username e username stesso. L'esistenza dell'username 
	 * è garantita dal fatto che ha potuto fare il login
	 */
	public void mostraClassifica(String username) {
		ArrayList<UtilityUserInfo> punteggioAmici;
		ArrayList<String> amici;
		
		amici = server.getUtenti().get(username);
		
		punteggioAmici = new ArrayList<UtilityUserInfo>(amici.size() + 1);
		
		for(String userAmico : amici) {
			punteggioAmici.add(new UtilityUserInfo(userAmico, server.getConnessioni().get(userAmico).getPunteggio()));
		}
		
		punteggioAmici.add(new UtilityUserInfo(username, server.getConnessioni().get(username).getPunteggio()));
		
		/*for(UtilityUserInfo prova : punteggioAmici) {
			System.out.println(prova.getUsername() + prova.getPunteggio());
		}*/
		
		response = server.getJson().scriviJSONAmici(punteggioAmici, 1);
		
		response = response.length() + "\r\n" + response;
	}
}
