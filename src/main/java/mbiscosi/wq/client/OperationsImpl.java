package mbiscosi.wq.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

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
	
	private AtomicBoolean sfidaTerminata = new AtomicBoolean(true);
	private int udpPort;
	
	
	
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
		
		for(udpPort = 1200; udpPort < 65353; udpPort++) {
			try {
				Thread thread = new Thread(new UdpHandler(InetAddress.getLocalHost(), udpPort, username));
				thread.start();
				break;
			} catch(SocketException e) {}
		}
		
		//System.out.println(udpPort);
		
		int response = Integer.parseInt(richiestaTCP("login " + username + " " + password + " " + udpPort));
		
		
		if(response == -1) {
			try {
				client.close();
			} catch (IOException e) {}
			MainClassClient.terminazione = true;
			UdpHandler.shutdown();
		}

		else if(response == 0) {
			msg = "Username o password sbagliati";
			try {
				client.close();
			} catch (IOException e) {}
			
			UdpHandler.shutdown();
		}
		
		
		else if(response == -2) {
			msg = "Login al momento non disponibile";
			try {
				client.close();
			} catch (IOException e) {}
			
			UdpHandler.shutdown();
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
		
		
		int response = Integer.parseInt(richiestaTCP("logout " + username));
		
		
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
		
		
		int response = Integer.parseInt(richiestaTCP("aggiungiAmico " + username + " " + userAmico));
		
		
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
		if(connesso == 0) {
			return "Prima di effettuare tale operazione si prega di effettuare il login";
		}
		
		System.out.println("Sfida in attesa di essere accettata...");
		
		MainClassClient.setTimer(System.currentTimeMillis());
		
		int response = richiestaTCPNonBlock("sfida " + username + " " + userAmico);
		
		switch(response) {
		case 2:
			msg = "Sfida rifiutata";
			break;
		
		case 1:
			System.out.println("Sfida accettata, preparazione della sfida in corso...");
			cicloSfida(0, username, userAmico);
			return null;
		
		
		case 0:
			msg = "Utente amico già in sfida";
			break;
			
		
		case -1:
			msg = "Utente amico non presente nella lista degli amici";
			break;
			
			
		case -2: 
			msg = "Amico al momento non connesso";
			break;
			
			
		case -3: 
			msg = "Errore nella richiesta di sfida, riprovare più tardi";
			break;
		}
		
		
		
		return msg;
	}
	
	
	
	
	@Override
	public String accettaSfida(String response, String username, String userAmico) {
		if(connesso == 0) {
			return "Prima di effettuare tale operazione si prega di effettuare il login";
		}
		
		
		int codResponse = Integer.parseInt(richiestaTCP("accettaSfida " + response + " " + username + " " + userAmico));
		
		switch(codResponse) {
		case 2:
			msg = "Sfida rifiutata";
			break;
		
			
		case 1:
			System.out.println("Sfida accetta, preparazione della sfida in corso...");
			cicloSfida(1, username, userAmico);
			MainClassClient.sfida.set(false);
			return null;
		
		
		case 0:
			msg = "Utente amico già in sfida";
			break;
			
		
		case -1:
			msg = "Utente amico non presente nella lista degli amici";
			break;
			
			
		case -2: 
			msg = "Amico al momento non connesso";
			break;
			
			
		case -3: 
			msg = "Errore nella richiesta di sfida, riprovare più tardi";
			break;
		}
		
		
		MainClassClient.sfida.set(false);
		return msg;
	}





	@Override
	public String mostraPunteggio(String username) {
		if(connesso == 0) {
			return "Prima di effettuare tale operazione si prega di effettuare il login";
		}
		
		
		msg = richiestaTCP("mostraPunteggio " + username);
		
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
	
	
	
	
	

	private String richiestaTCP(String request) {
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
				return "-1";
			}
		}
		
		
		buffer.clear();
		
		//attendo la risposta e la confronto con la precedente
		try {
			client.read(buffer);
			
		} catch (IOException e) {
			msg = "Server terminato, chiusura del client...";
			return "-1";
		}
		
		
		buffer.flip();
		
		byte[] tmp = new byte[buffer.limit()];
		buffer.get(tmp);
		
		buffer.clear();
		
		return new String(tmp);
	}
	
	
	
	
	private int richiestaTCPNonBlock(String request) {
		//invio la stringa al server
		
		try {
			client.configureBlocking(false);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		buffer.put(request.getBytes());
		buffer.flip();
		
		//System.out.println(request);
		
		
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
		
		int counter;
		//attendo la risposta e la confronto con la precedente
		try {
			//client.read(buffer);
			while(System.currentTimeMillis() - MainClassClient.getTimer() < 30000) {
				counter = client.read(buffer);
				
				if(counter == -1) {
					System.out.println("Server terminato, chiusura del client...\r\n");
					client.close();
					return -1;
				}
				
				if(counter > 0)
					break;
				
				System.out.println(counter);
			}
			System.out.println();
		} catch (IOException e) {
			msg = "Server terminato, chiusura del client...";
			return -1;
		}
		
		if(System.currentTimeMillis() - MainClassClient.getTimer() > 30000)
			buffer.put("2".toString().getBytes());
		
		buffer.flip();
		
		byte[] tmp = new byte[buffer.limit()];
		buffer.get(tmp);
		int response = Integer.parseInt(new String(tmp));
		buffer.clear();
		
		
		System.out.println(response);
		
		try {
			client.configureBlocking(true);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
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
	
	
	//0 sfidante
	//1 sfidato
	private void cicloSfida(int tipo, String username, String userAmico) {
		
		sfidaTerminata.set(false);
		
		String keyMap;
		
		if(tipo == 0) {
			keyMap = username + userAmico;
		}
		
		else {
			keyMap = userAmico + username;
		}
		
		int response = Integer.parseInt(richiestaTCP("sfidaPronto " + keyMap));
		
		//Servizio di traduzione non disponibile, ritorno un errore e termino la sfida
		if(response == -1) {
			System.out.println("Problema col server di traduzione, sfida al momento non disponibile.\r\nSi prega di riprovare più tardi.");
			sfidaTerminata.set(true);
			return;
		}
		
		int esito = 0;
		
		System.out.println("Via alla sfida di traduzione!");
		System.out.println("Hai " + response * 20 + " secondi per tradurre tutte le parole!");
		
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		Thread thread = new Thread(new Timer(response * 20, tipo, keyMap, username));
		thread.start();
		
		
		for(int i = 0; i < response; i++) {
			if(sfidaTerminata.get())
				break;
			//Qui richiedo la parola da tradurre
			String parola = richiestaTCP("parolaSfida " + i + " " + keyMap + " " + tipo);
			
			if(parola.equals("-1") || parola.equals("0"))
				return;
			
			System.out.println("Parola " + (i + 1) + "/" + response + ": " + parola);
			
			String traduzione = null;
			
			if(sfidaTerminata.get())
				break;
			try {
				traduzione = reader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(!thread.isAlive()) {
				return;
			}
			
			
			if(sfidaTerminata.get())
				break;
			//che parola ho tradotto + la keymap + se sono sfidante o sfidato + traduzione della parola
			String tmp = richiestaTCP("rispostaParola " + i + " " + keyMap + " " + tipo + " " + traduzione);
				
			if(tmp.equals("-1") || tmp.equals("0"))
				return;
		}
				
		
		if(!sfidaTerminata.get()) {
			System.out.println("Sfida terminata!\r\nVerranno visualizzati i risultati a breve.\r\n"
					+ "Si prega di attendere il termine della sfida da parte dell'avversario.");
			sfidaTerminata.set(true);
			thread.interrupt();
			
			String resp = new String();
			do {
				resp = richiestaTCP("termineSfida " + keyMap + " " + tipo + " " + username);
				if(resp.equals("-1")) {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			while(resp.equals("-1"));
			
			System.out.println(resp);
		}
	}
	
	
	
	
	public class Timer implements Runnable{
		private int time;
		private int tipo;
		private String keyMap;
		private String username;
		
		public Timer(int time, int tipo, String keyMap, String username) {
			this.time = time;
			this.tipo = tipo;
			this.keyMap = keyMap;
			this.username = username;
		}

		@Override
		public void run() {
			try {
				Thread.sleep(time*1000);
				
				if(!sfidaTerminata.get()) {
					sfidaTerminata.set(true);
					System.out.println("Tempo scaduto!\r\nVerranno visualizzati i risultati a breve.");
					
					String resp = new String();
					do {
						resp = richiestaTCP("termineSfida " + keyMap + " " + tipo + " " + username);
						if(resp.equals("-1")) {
							try {
								Thread.sleep(3000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					while(resp.equals("-1"));
					
					System.out.println(resp);
					System.out.println("Premere un qualsiasi tasto per tornare alla scelta dei comandi...");
				}
				
			} catch (InterruptedException e) {
				//System.out.println("Thread sveglia interrotto");
				return;
			}
		}
	}
}
