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
	/*
	 * Classe che implementa l'interfaccia Operations
	 * Qui ci sono implementate tutte le operazioni delle possibili richieste che il client puo'
	 * fare al server.
	 */
	
	
	private static int DEFAULT_PORT = 13200;
	private static int BUFFER_SIZE = 2048; 
	
	private int port;
	private SocketAddress address;
	private SocketChannel client;
	private static ByteBuffer buffer;
	private static int connesso = 0;
	private String msg = null;
	
	private AtomicBoolean sfidaTerminata = new AtomicBoolean(true);
	private int udpPort;
	
	
	
	public OperationsImpl(SocketAddress address, SocketChannel client, int port) {
		this.address = address;
		this.client = client;
		
		//In caso non venga inserita una porta corretta (controllato nella classe MainClassClient),
		//viene assegnata la porta di default
		if(port == -1) 
			this.port = DEFAULT_PORT;
		else
			this.port = port;
		
		buffer = ByteBuffer.allocate(BUFFER_SIZE);
	}
	
	
	
	
	/*
	 * Mostra i vari comandi
	 */
	public void help() {
		System.out.println("usage: COMMAND [ ARGS ...]");
		System.out.println("Commands:");
		System.out.println("  registra_utente <username> <password>   registra l'utente");
		System.out.println("  login <username> <password>   effettua il login");
		System.out.println("  logout   effettua il logout");
		System.out.println("  aggiungi_amico <userAmico>   crea relazione di amicizia con userAmico");
		System.out.println("  lista_amici   mostra la lista dei propri amici");
		System.out.println("  mostra_punteggio   mostra il punteggio dell'utente");
		System.out.println("  mostra_classifica   mostra una classifica degli amici dell'utente (incluso l'utente stesso)");
		System.out.println("  sfida <userAmico>   richiesta di una sfida a userAmico");
	}
	
	
	
	/*
	 * LOGIN
	 * 
	 * Il login è così strutturato:
	 * appena viene richiamata, effettua la connessione al server, dopodichè spedirà
	 * il pacchetto al server contente username e password e controllerà se l'utente
	 * ha inserito credenziali corrette, in caso affermativo lo connette.
	 * 
	 * Returns: la stringa contente la risposta alla richiesta di login da parte del server.
	 * 
	 * Exceptions: in caso di server non raggiungibile, verrà lanciata la IOException e verrà terminato il client.
	 */
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
	
	
	
	
	
	/*
	 * REGISTRAZIONE
	 * 
	 * appena richiamata, effettua la registrazione di un nuovo utente con username
	 * e password passate; l'operazione dovrebbe esser permessa solo se l'utente non ha
	 * effettuato il login. In caso di server non disponibile la registrazione non verrà effettuata.
	 * 
	 * Returns: la stringa contente la risposta alla richiesta di registrazione da parte del server (Se viene richiesta la 
	 * 			registrazione di un utente con username già esistente, la risposta sarà negativa).
	 * 
	 * Exceptions: 
	 */
	public String registrazione(String username, String password) throws RemoteException, NotBoundException{
		if(connesso == 1)
			return "Logout necessario per registrare un nuovo account";
		
		Registry reg = LocateRegistry.getRegistry();
		ConnectionInterface registra = (ConnectionInterface) reg.lookup("registrazione");
		
		return registra.registrazione(username, password);
	}
	
	
	
	
	
	/*
	 * CONNESSIONE
	 * 
	 * operazione non presente nell'interfaccia, in quanto utilizzata internamente dalla classe per richiedere
	 * il login al server e allo stesso tempo avviare un selector UDP (Classe UdpHandler) in attesa su una porta che verrà scelta
	 * casualmente tra le porte disponibili.
	 * 
	 * Returns: La stringa di risposta da parte del server.
	 * 
	 * Exceptions:
	 */
	private String connessione(String username, String password) throws IOException{
		//Configuro l'indirizzo e la porta per le richieste TCP fatte dal client al server
		address = new InetSocketAddress("localhost", port);
		
		client = SocketChannel.open(address);
		
		//Viene scelta una porta UDP casuale >= 1200
		for(udpPort = 1200; udpPort < 65353; udpPort++) {
			try {
				Thread thread = new Thread(new UdpHandler(InetAddress.getLocalHost(), udpPort, username));
				thread.start();
				break;
			} catch(SocketException e) {}
		}
		
		/*
		 * Viene mandata la richiesta al server per effettuare il login; la richiesta e' cosi' strutturata:
		 * - login username password udpPort -
		 */
		int response = Integer.parseInt(richiestaTCP("login " + username + " " + password + " " + udpPort));
		
		//Controllo la risposta
		if(response == 0) {
			//Errore nell'inserimento dei dati
			msg = "Username o password sbagliati";
			try {
				client.close();
			} catch (IOException e) {}
			
			UdpHandler.shutdown();
		}
		
		
		else if(response == -2) {
			//Utente gia' connesso da un altro dispositivo
			msg = "Login al momento non disponibile";
			try {
				client.close();
			} catch (IOException e) {}
			
			UdpHandler.shutdown();
		}

		
		else if(response == 1) {
			//Caso positivo
			connesso = 1;
			msg = "Login avvenuto con successo";
		}
		
		
		return msg;
	}
	
	


	
	/*
	 * LOGOUT
	 * 
	 * effettua il logout dell'utente, solo se l'utente username era effettivamente loggato.
	 * 
	 * Returns: ritorna il messaggio di risposta del server
	 * 
	 * Exceptions: 
	 */
	public String logout(String username) {
		if(connesso == 0) {
			return "Prima di effettuare il logout si prega di effettuare il login";
		}
		
		/*
		 * Formato del messaggio:
		 * - logout username -
		 */
		int response = Integer.parseInt(richiestaTCP("logout " + username));
		
		
		switch(response) {
		case 1:
			//Logout andato a buon fine
			msg = "Logout avvenuto con successo";
			connesso = 0;
			break;
		
		
		case 0:
			//Utente non esistente
			msg = "Utente non esistente";
			connesso = 0;
			break;
			
		case -2: 
			//Utente gia' disconnesso
			msg = "Logout al momento non disponibile";
			connesso = 0;
			break;
		}
		
		try {
			client.close();
		} catch (IOException e) {}
		
		MainClassClient.terminazione = true;
			
		UdpHandler.shutdown();
		return msg;
	}





	
	/*
	 * AGGIUNTA_AMICO
	 * 
	 * aggiunge un utente alla lista degli amici di username, solo se userAmico esiste e non 
	 * è già amico di username.
	 * 
	 * Returns: ritorna la risposta del server
	 * 
	 * Exceptions:
	 */
	public String aggiungiAmico(String username, String userAmico) {
		if(connesso == 0) {
			return "Prima di effettuare tale operazione si prega di effettuare il login";
		}
		
		/*
		 * Richiesta così strutturata:
		 * - aggiungiAmico username userAmico -
		 */
		int response = Integer.parseInt(richiestaTCP("aggiungiAmico " + username + " " + userAmico));
		
		
		switch(response) {
		case 1:
			//Aggiunta avvenuta con successo
			msg = "Amico aggiunto con successo";
			break;
		
		
		case 0:
			//Amico non esistente
			msg = "Utente amico non esistente";
			break;
			
			
		case -2: 
			//Amico già nella lista degli amici
			msg = "Amico già nella lista degli amici";
			break;
		}
		
		
		return msg;
	}





	/*
	 * LISTA_AMICI
	 * 
	 * fornisce la lista degli amici di username, restituendo un array di String,
	 * di cui ogni elemento è un amico.
	 * 
	 * Returns: ritorna la lista degli amici di username, in caso di nessun amico nella lista,
	 * 			ritorna un messaggio di default
	 * 
	 * Exceptions: 
	 */
	public String listaAmici(String username) {
		if(connesso == 0) {
			return "Prima di effettuare tale operazione si prega di effettuare il login";
		}
		
		msg = "Lista amici vuota...";
		
		/*
		 * Richiesta così strutturata
		 * - listaAmici username -
		 */
		String jsonResponse = richiestaTCPJson("listaAmici " + username, 0);
		
		if(jsonResponse != null)
			msg = jsonResponse;
		
		return msg;
	}





	/*
	 * SFIDA
	 * 
	 * effettua una sfida tra i due utenti, username e userAmico.
	 * La sfida verrà gestita con protocollo TCP.
	 * La sfida e' cosi' composta:
	 *  - lo sfidante richiede al server una sfida con lo sfidato;
	 *  - lo sfidato risponde o in modo affermativo o negativo (in caso negativo la sfida viene annullata);
	 *  - in caso positivo viene costruita la sfida ed il server mandera' una alla volta le parole da tradurre
	 *  - il tempo massimo per terminare la sfida e' esattamente il numeroDiParoleDaTradurre * 20 sec (si stima un tempo di 20 sec
	 *  		massimi per poter tradurre ogni parola);
	 *  - in caso di terminazione del timer, le parole non tradotte verranno considerate 0 punti;
	 *  - il punteggio verra' calcolato solamente quando entrambi gli utenti avranno finito di rispondere a tutte le traduzioni;
	 *  - a fine partita verra' aggiornato il punteggio (lato server) e visualizzato il vincitore;
	 *  
	 *  Returns: messaggio di risposta dal server, in caso di accettazione della sfida, viene richiamato il metodo cicloSfida();
	 *  
	 *  Exceptions: 
	 */
	public String sfida(String username, String userAmico) {
		if(connesso == 0) {
			return "Prima di effettuare tale operazione si prega di effettuare il login";
		}
		
		System.out.println("Sfida in attesa di essere accettata...");
		
		//Setto il timer per verificare la richiesta di sfida non superi un timer dovuto (20 sec)
		MainClassClient.setTimer(System.currentTimeMillis());
		
		/*
		 * Invio la richiesta di sfida al server, la richiesta è così strutturata:
		 * - sfida username userAmico -
		 */
		int response = richiestaTCPNonBlock("sfida " + username + " " + userAmico);
		
		
		
		switch(response) {
		case 2:
			//Sfida rifiutata dallo sfidato
			msg = "Sfida rifiutata";
			break;
		
		case 1:
			//Sfida accettata, il ciclo della sfida si svolgera' all'interno del metodo cicloSfida();
			System.out.println("Sfida accettata, preparazione della sfida in corso...");
			cicloSfida(0, username, userAmico);
			return null;
		
		
		case 0:
			//Caso in cui un utente sia gia' in una sfida
			msg = "Utente amico già in sfida";
			break;
			
		
		case -1:
			//Caso in cui l'utente sfidato non sia presente nella lista di amici dell'utente sfidante
			msg = "Utente amico non presente nella lista degli amici";
			break;
			
			
		case -2: 
			//Sfidato non connesso al momento
			msg = "Amico al momento non connesso";
			break;
			
			
		case -3: 
			//Errore lato server
			msg = "Errore nella richiesta di sfida, riprovare più tardi";
			break;
		}
		
		
		
		return msg;
	}
	
	
	
	
	/*
	 * ACCETTAZIONE DELLA SFIDA
	 * 
	 * metodo richiamato in caso di richiesta di una sfida da parte di un altro utente.
	 * In caso di accettazione della sfida, verra' richiamato il metodo cicloSfida().
	 * 
	 * Returns: messaggio di risposta dal server; in caso di accettazione della sfida, viene richiamato il metodo cicloSfida();
	 * 
	 * Exceptions:
	 */
	public String accettaSfida(String response, String username, String userAmico) {
		if(connesso == 0) {
			return "Prima di effettuare tale operazione si prega di effettuare il login";
		}
		
		/*
		 * Messaggio cosi' strutturato:
		 * - accettaSfida risposta nomeSfidato nomeSfidante -
		 */
		int codResponse = Integer.parseInt(richiestaTCP("accettaSfida " + response + " " + username + " " + userAmico));
		
		switch(codResponse) {
		case 2:
			//Sfida rifiutata
			msg = "Sfida rifiutata";
			break;
		
			
		case 1:
			//Sfida accettata
			System.out.println("Sfida accetta, preparazione della sfida in corso...");
			cicloSfida(1, username, userAmico);
			MainClassClient.sfida.set(false);
			return null;
			
		
		case -1:
			//Utente sfidante disconnesso
			msg = "Utente amico disconnesso, sfida annullata";
			break;
		}
		
		
		MainClassClient.sfida.set(false);
		return msg;
	}





	/*
	 * MOSTRA_PUNTEGGIO
	 * 
	 * mostra il punteggio totale dell'utente username
	 * 
	 * Returns: punteggio dell'utente
	 */
	public String mostraPunteggio(String username) {
		if(connesso == 0) {
			return "Prima di effettuare tale operazione si prega di effettuare il login";
		}
		
		/*
		 * Richiesta cosi' strutturata:
		 * - mostraPunteggio username -
		 */
		msg = richiestaTCP("mostraPunteggio " + username);
		
		return msg;
	}





	/*
	 * MOSTRA_CLASSIFICA
	 * 
	 * mostra la classifica tra gli utenti username e la sua lista di amici. 
	 * 
	 * Returns: classifica, in ordine di punteggio decrescente, degli amici di username e username
	 * 
	 * Exceptions: 
	 */
	public String mostraClassifica(String username) {
		if(connesso == 0) {
			return "Prima di effettuare tale operazione si prega di effettuare il login";
		}
		
		msg = "Nessun amico...";
		
		String jsonResponse = richiestaTCPJson("mostraClassifica " + username, 1);
		
		return jsonResponse;
	}
	
	
	
	
	
	
	/*
	 * CICLO DI VITA DELLA SFIDA
	 * 
	 * Metodo utilizzato per il settaggio e l'avvio della sfida.
	 * 
	 * Returns:
	 * 
	 * Exceptions
	 */
	private void cicloSfida(int tipo, String username, String userAmico) {
		/*
		 * Tipo:
		 * 	- 0 sfidante;
		 *  - 1 sfidato;
		 */
		sfidaTerminata.set(false);
		
		//Setto la entry della map presente lato server che identifica univocamente la partita
		String keyMap;
		
		if(tipo == 0) {
			keyMap = username + userAmico;
		}
		
		else {
			keyMap = userAmico + username;
		}
		
		/*
		 * Invio un messaggio al server per indicare che lato client son pronto alla sfida;
		 * - sfidaPronto keyMap -
		 */
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
		
		//Avvio il thread che gestisce il timer per le risposte
		Thread thread = new Thread(new Timer(response * 20, tipo, keyMap, username));
		thread.start();
		
		
		for(int i = 0; i < response; i++) {
			if(sfidaTerminata.get())
				//Timer scaduto
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
			/*
			 * Invio la risposta che l'utente ha fornito alla parola i;
			 * la risposta e' cosi' strutturata:
			 * - rispostaParola numeroParola keyMapEntry sfidanteOSfidato traduzioneFornita -
			 */
			String tmp = richiestaTCP("rispostaParola " + i + " " + keyMap + " " + tipo + " " + traduzione);
				
			if(tmp.equals("-1") || tmp.equals("0"))
				return;
		}
				
		
		//In caso il timer non sia scaduto
		if(!sfidaTerminata.get()) {
			System.out.println("Sfida terminata!\r\nVerranno visualizzati i risultati a breve.\r\n"
					+ "Si prega di attendere il termine della sfida da parte dell'avversario.");
			sfidaTerminata.set(true);
			
			//Interrompo il thread timer
			thread.interrupt();
			
			String resp = new String();
			
			/*
			 * Ogni 3 secondi, invio il messaggio al client che l'utente username ha terminato;
			 * finche' entrambi gli utenti non hanno terminato, non smetto di mandare il messaggio.
			 * Messaggio cosi' strutturato:
			 * - termineSfida keyMapEntry sfidanteOSfidato username - 
			 */
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

	
	
	
	/*
	 * RICHIESTE TCP BLOCKING
	 * 
	 * Metodo privato della classe utilizzato per inviare tramite protocollo TCP Blocking, le richieste al server.
	 * (Utilizza NIO)
	 * 
	 * Returns: ritorna le risposte del server.
	 * 
	 * Exceptions: 
	 */
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
	
	
	
	/*
	 * RICHIESTE TCP BLOCKING
	 * 
	 * Metodo privato della classe utilizzato per inviare tramite protocollo TCP Non-Blocking, le richieste al server.
	 * (Metodo utilizzato per richiedere la sfida ad un altro utente)
	 * (Utilizza NIO)
	 * 
	 * Returns: ritorna le risposte del server.
	 * 
	 * Exceptions: 
	 */
	private int richiestaTCPNonBlock(String request) {
		//invio la stringa al server
		
		try {
			client.configureBlocking(false);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
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
		
		int counter;
		//attendo la risposta e la confronto con la precedente
		try {
			//Utilizzo questo metodo per via del timer
			while(System.currentTimeMillis() - MainClassClient.getTimer() < 30000) {
				counter = client.read(buffer);
				
				if(counter == -1) {
					System.out.println("Server terminato, chiusura del client...\r\n");
					client.close();
					return -1;
				}
				
				if(counter > 0)
					break;
			}
		} catch (IOException e) {
			msg = "Server terminato, chiusura del client...";
			return -1;
		}
		
		buffer.flip();
		
		byte[] tmp = new byte[buffer.limit()];
		buffer.get(tmp);
		int response;
		try {
			response = Integer.parseInt(new String(tmp));
		} catch (RuntimeException ex){
			response = 2;
		}
		buffer.clear();
		
		try {
			client.configureBlocking(true);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		return response;
	}
	
	
	
	
	/*
	 * RICHIESTA TCP JSON
	 * 
	 * metodo privato della classe utilizzato per richiedere e ricevere dati di tipo json.
	 * 
	 * Returns: ritorna la stringa parsata.
	 * 
	 * Exceptions:
	 */
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
	
	
	
	
	/*
	 * Metodo privato utilizzato per parsare la risposta JSON ricevuta dal server
	 */
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
	
	
	
	
	
	
	/*
	 * TIMER
	 * 
	 * classe utilizzata per avviare il timer della sfida.
	 * Se allo scadere del timer la sfida non e' ancora finita, manda automaticamente un messaggio al server
	 * comunicando la terminazione della sfida da parte di username.
	 * 
	 */
	private class Timer implements Runnable{
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
				//Dormo fino allo scadere del timer
				Thread.sleep(time*1000);
				
				//Controllo che la sfida non sia finita
				if(!sfidaTerminata.get()) {
					sfidaTerminata.set(true);
					System.out.println("Tempo scaduto!\r\nVerranno visualizzati i risultati a breve.");
					
					//Se non e' finita invio il messaggio al server ogni 3 sec. per richiedere i risultati
					String resp = new String();
					do {
						resp = richiestaTCP("termineSfida " + keyMap + " " + tipo + " " + username);
						if(resp.equals("-1")) {
							try {
								Thread.sleep(3000);
							} catch (InterruptedException e) {
								return;
							}
						}
					}
					while(resp.equals("-1"));
					
					System.out.println(resp);
					System.out.println("Premere un qualsiasi tasto per tornare alla scelta dei comandi...");
				}
				
			} catch (InterruptedException e) {
				return;
			}
		}
	}
}
