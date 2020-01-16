package mbiscosi.wq.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import mbiscosi.wq.server.ConnectionInterface;

public class OperationsImpl implements Operations{
	
	private static int DEFAULT_PORT = 13200;
	private static int BUFFER_SIZE = 2048; 
	
	private SocketAddress address;
	private SocketChannel client;
	private static ByteBuffer buffer;
	private static int connesso = 0;
	private String msg = null;
	
	
	
	public OperationsImpl(SocketAddress address, SocketChannel client) {
		this.address = address;
		this.client = client;
		buffer = ByteBuffer.allocate(BUFFER_SIZE);
	}
	
	
	
	
	
	public String login(String username, String password) {	
		if(connesso == 1)
			return "Utente già connesso";
		
		try {
			return connessione(username, password);
		} catch (IOException e) {
			System.out.println("Server al momento non raggiungibile, riprovare più tardi");
			MainClassClient.terminazione = true;
		}
		
		return null;
	}
	
	
	
	
	
	public String registrazione(String username, String password) throws RemoteException, NotBoundException{
		if(connesso == 1)
			return "Logout necessario per registrare un nuovo account";
		
		Registry reg = LocateRegistry.getRegistry();
		ConnectionInterface registra = (ConnectionInterface) reg.lookup("registrazione");
		
		return registra.registrazione(username, password);
	}
	
	
	
	
	
	
	private String connessione(String username, String password) throws IOException{
		//Configuro l'indirizzo e la porta per le richieste TCP fatte dal client al server
		address = new InetSocketAddress("localhost", DEFAULT_PORT);
		
		client = SocketChannel.open(address);
		
		int response = richiestaTCP("login " + username + " " + password);
		
		
		if(response == -1) {
			try {
				client.close();
			} catch (IOException e) {}
			MainClassClient.terminazione = true;
		}

		else if(response == 0) {
			msg = "Username o password sbagliati";
			try {
				client.close();
			} catch (IOException e) {}
		}
		
		
		else if(response == -2) {
			msg = "Login al momento non disponibile";
			try {
				client.close();
			} catch (IOException e) {}
		}

		
		else if(response == 1) {
			connesso = 1;
			msg = "Login avvenuto con successo";
		}
		
		
		return msg;
	}
	
	


	@Override
	public String logout(String username) {
		if(connesso == 0) {
			return "Prima di effettuare il logout si prega di effettuare il login";
		}
		
		
		int response = richiestaTCP("logout " + username);
		
		
		switch(response) {
		case 1:
			msg = "Logout avvenuto con successo";
			connesso = 0;
			break;
		
		
		case 0:
			msg = "Utente non esistente";
			connesso = 0;
			break;
			
		
		case -1:
			connesso = 0;
			break;
			
			
		case -2: 
			msg = "Logout al momento non disponibile";
			connesso = 0;
			break;
		}
		
		try {
			client.close();
		} catch (IOException e) {}
		
		MainClassClient.terminazione = true;
			
		
		return msg;
	}





	@Override
	public String aggiungiAmico(String username, String userAmico) {
		if(connesso == 0) {
			return "Prima di effettuare tale operazione si prega di effettuare il login";
		}
		
		
		int response = richiestaTCP("aggiungiAmico " + username + " " + userAmico);
		
		
		switch(response) {
		case 1:
			msg = "Amico aggiunto con successo";
			break;
		
		
		case 0:
			msg = "Utente amico non esistente";
			break;
			
		
		case -1:
			break;
			
			
		case -2: 
			msg = "Amico già nella lista degli amici";
			break;
		}
		
		
		return msg;
	}





	@Override
	public String listaAmici(String username) {
		if(connesso == 0) {
			return "Prima di effettuare tale operazione si prega di effettuare il login";
		}
		
		msg = "Nessun amico...";
		
		String jsonResponse = richiestaTCPJson("listaAmici " + username, 0);
		
		return jsonResponse;
	}





	@Override
	public String sfida(String username, String userAmico) {
		// TODO Auto-generated method stub
		return null;
	}





	@Override
	public String mostraPunteggio(String username) {
		if(connesso == 0) {
			return "Prima di effettuare tale operazione si prega di effettuare il login";
		}
		
		
		int response = richiestaTCP("mostraPunteggio " + username);
		
		msg = Integer.toString(response);
		
		return msg;
	}





	@Override
	public String mostraClassifica(String username) {
		if(connesso == 0) {
			return "Prima di effettuare tale operazione si prega di effettuare il login";
		}
		
		msg = "Nessun amico...";
		
		String jsonResponse = richiestaTCPJson("mostraClassifica " + username, 1);
		
		return jsonResponse;
	}
	
	
	
	
	

	private int richiestaTCP(String request) {
		//invio la stringa al server
		
		
		buffer.put(request.getBytes());
		buffer.flip();
		
		
		while(buffer.hasRemaining()) {
			try {
				client.write(buffer);
			//Se viene lanciata l'eccezione è perchè il server è terminato,
			//per cui termino anche il client
			} catch (IOException e) {
				msg = "Connessione terminata dal server, chiusura del client in corso...";	
				return -1;
			}
		}
		
		
		buffer.clear();
		byte[] tmp;
		//int bytes = 0;
		int counter;
		//attendo la risposta e la confronto con la precedente
		try {
			counter = client.read(buffer);
			/*while((counter = client.read(buffer)) != 0) {
				
				bytes += counter;
				if(bytes == -1) {
					System.out.println("Server terminato, chiusura del client...\r\n");
					client.close();
					return -1;
				}
				
				System.out.println(bytes);
			}*/
			System.out.println();
		} catch (IOException e) {
			msg = "Server terminato, chiusura del client...";
			return -1;
		}
		
		tmp = new byte[counter];
		buffer.flip();
		
		buffer.get(tmp, 0, counter);
		int response = Integer.parseInt(new String(tmp));
		buffer.clear();
		
		return response;
	}
	
	
	
	
	private String richiestaTCPJson(String request, int classifica) {
		buffer.put(request.getBytes());
		buffer.flip();
		
		
		while(buffer.hasRemaining()) {
			try {
				client.write(buffer);
			//Se viene lanciata l'eccezione è perchè il server è terminato,
			//per cui termino anche il client
			} catch (IOException e) {
				msg = "Connessione terminata dal server, chiusura del client in corso...";	
				return null;
			}
		}
		
		
		buffer.clear();
		
		String response = null;
		byte[] tmp;
		int counter;
		int dimension = 0;
		int textDim = 0;
		//attendo la risposta e la confronto con la precedente
		try {
			do {	
				counter = client.read(buffer);
				
				tmp = new byte[counter];
				buffer.flip();
				
				buffer.get(tmp, 0, counter);
				if(response == null) {
					response = new String(tmp);
					String[] splitting = response.split("\r\n");
					dimension = Integer.parseInt(splitting[0]);
					textDim = splitting[1].length();
					response = splitting[1];
				}
				
				else {
					response = response.concat(new String(tmp));
					textDim += response.length();
				}
				buffer.clear();	
				
			} while(textDim != dimension);
			
		} catch (IOException e) {
			msg = "Server terminato, chiusura del client...";
			return null;
		}
		
		
		parseJson(response, classifica);
		
		return msg;
	}
	
	
	
	private void parseJson(String response, int classifica) {
		
		
		JSONParser jsonParser = new JSONParser();
		Object obj;
		String arrayAmici = null;
		try {
			obj = jsonParser.parse(response);
			JSONObject tmp = (JSONObject) obj;
			
			JSONArray utenti = (JSONArray) tmp.get("ListaAmici");
			
			
			Iterator<JSONObject> internIt = utenti.iterator();
			int i = 0;
			
			if(classifica == 0) {
				while(internIt.hasNext()) {
					
					String userAmico = (String) internIt.next().get("UserAmico");
	
					if(i == 0) {
						i++;
						msg = userAmico;	
					}
					
					else 
						msg = msg.concat(", " + userAmico);
					
				}
			}
			
			else {
				while(internIt.hasNext()) {
					JSONObject tmpObj = (JSONObject) internIt.next();
					
					String userAmico = (String) tmpObj.get("UserAmico");
					long punteggio = (long) tmpObj.get("Punteggio");
	
					if(i == 0) {
						i++;
						msg = userAmico + " -> " + punteggio;	
					}
					
					else 
						msg = msg.concat(", " + userAmico + " -> " + punteggio);
					
				}
			}
			
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}
