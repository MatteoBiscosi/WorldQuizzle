package mbiscosi.wq.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;



public class ServerService implements Runnable{
	/*
	 * Classe che gestisce il server principale, tutte le strutture dati e i file JSON
	 * utilizzati per il funzionamento corretto del server
	 */
	
	private static int BUFFER_SIZE = 1024;
	private static int DEFAULT_PORT = 13200;
	
	//STRUTTURE DATI
	//Utenti connessi e le loro info
	private ConcurrentHashMap<String, UserInfo> connessioni;
	//Utenti connessi con le loro liste di amici
	private ConcurrentHashMap<String, ArrayList<String>> utenti;
	//Sfide con le utility della sfida
	private ConcurrentHashMap<String, ChallengeUtilities> mapSfida;
	//Associazione tra un channel e l'username connesso a quel channel
	private ConcurrentHashMap<SelectableChannel, String> channelUtenti;
	//Parole da tradurre nella sfida
	private ArrayList<String> parole;
	
	
	
	private JsonCreator json;
	private Selector mainSelector;
	private ServerSocketChannel server;
	private ServerSocket serverSock;
	private Thread[] secondarySelectors;
	
	
	private int port;
	public static boolean terminate;
	private int counter;
	private WorkerSelector[] selectors;
	
	
	public ServerService(int port) {
		//Setto la porta del server
		if(port == -1) {
			System.out.println("Il server richiede un parametro in input: [port]\r\n"
					+ "Sara' impostata la porta standard: 13200");
			
			this.port = DEFAULT_PORT;
		}
		
		else
			this.port = port;
		
		//La hash map per associare ad ogni channel l'username connesso
		channelUtenti = new ConcurrentHashMap<SelectableChannel, String>();
		
		//La hash map per le connessioni
		connessioni = new ConcurrentHashMap<String, UserInfo>();
		
		//La hash map per gli utenti, che fa da lista di adiacenza
		utenti = new ConcurrentHashMap<String, ArrayList<String>>();
		
		parole = new ArrayList<String>();
		
		mapSfida = new ConcurrentHashMap<String, ChallengeUtilities>();
		
		//Alloco il json, se non esiste lo creo, altrimenti lo leggo e 
		//mi salvo le info nelle due strutture, connessioni e utenti 
		json = new JsonCreator(this);	
		
		
		try {
			//Definisco il server, apro il socketChannel e faccio la bind del server 
			//sull'indirizzo e la porta; poi lo setto non bloccante
			server = ServerSocketChannel.open();
			serverSock = server.socket();
			serverSock.bind(new InetSocketAddress(this.port));
			server.configureBlocking(false);
			
			
			//Faccio un array di selector, ognuno dei quali verrà gestito da un thread differente 
			//e saranno quelli che gestiranno le richieste dei clients
			secondarySelectors = new Thread[10];
			selectors = new WorkerSelector[10];
			
			for(int i = 0; i < 10; i++) {
				try {
					selectors[i] = new WorkerSelector(this, i);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				secondarySelectors[i] = new Thread(selectors[i]);
				secondarySelectors[i].start();
			}
			
			
			//A questo punto configuro il selettore ed ho finito il settaggio del server
			mainSelector = Selector.open();
			SelectionKey key2 = server.register(mainSelector, SelectionKey.OP_ACCEPT);
			
			//Ci attacco un Acceptor che mi servirà per inoltrare le nuove richieste
			//di connessione al selector corretto
			key2.attach(new Acceptor());
			
			
			System.out.println("Server attivo in attesa sulla porta: " + this.port);
		} catch (IOException e) {
			System.out.println("Server in terminazione. Errore durante l'avvio: " + e);
			return;
		}
		
		
	}
	
	
	
	
	/*
	 * Metodo run: Si mette in attesa di richieste connessioni e nel caso di nuova
	 * richiesta, la inoltra ai vari selector che la gestiscono
	 */
	public void run() {
		
		while(!terminate) {
			try {
				//mi metto in attesa sulla select
				mainSelector.select();
				
				//creo il set di key e un iterator su cui scorrere le chiavi della select
				Set<SelectionKey> set = mainSelector.selectedKeys(); 
				Iterator<SelectionKey> iterator = set.iterator();
				
				
				//Faccio un ciclo per tutte le chiavi che mi arrivano e le invio al dispatcher
				while(iterator.hasNext())
					dispatch((SelectionKey) iterator.next());
				
				set.clear();
				
				
			} catch (IOException e) {			
				if(Thread.interrupted())
					shutdown();
				else {
					System.err.println("Errore: " + e);
					shutdown();
				}
			}
		}
		
		
		shutdownNow();
	}
	
	
	
	
	
	/*
	 * Procedura utilizzata per aggiungere un elemento al selectors i-esimo
	 */
	private void dispatch(SelectionKey key) {
		Acceptor acceptor = (Acceptor) key.attachment();
		if(acceptor != null) {
			acceptor.run();
		}
	}
	
	
	
	/*
	 * Classe utilizzata per fare da Acceptor
	 */
	private class Acceptor {
		public synchronized void run() {
			try {
				SocketChannel client = server.accept();				
				
				//Aggiungo il client al selector e controllo il next
				if(client != null) {
					selectors[counter].addClient(client);
					if(++counter == 10)
						counter = 0;
				}
			} catch(IOException ex) {
				ex.printStackTrace();
			}
		}
	}	
	
	
	

	/*
	 * Funzione utilizzata come protocollo di terminazione,
	 * Sveglio tutti i selectors e li termino
	 */
	private void shutdownNow() {
		try {
			for(int i = 0; i < 10; i ++) 
				selectors[i].shutdown();
			
			mainSelector.close();
			server.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	public void shutdown() {
		terminate = true;
	}
	
	public ConcurrentHashMap<String, UserInfo> getConnessioni() {
		return connessioni;
	}
	
	public ConcurrentHashMap<String, ArrayList<String>> getUtenti() {
		return utenti;
	}
	
	public JsonCreator getJson() {
		return json;
	}
	
	public WorkerSelector getSelector(int selectorNum) {
		return this.selectors[selectorNum];
	}
	
	public ArrayList<String> getParole() {
		return parole;
	}	

	public ConcurrentHashMap<String, ChallengeUtilities> getMapSfida() {
		return mapSfida;
	}
	
	public ConcurrentHashMap<SelectableChannel, String> getChannelUtenti() {
		return channelUtenti;
	}

	public void setChannelUtenti(ConcurrentHashMap<SelectableChannel, String> channelUtenti) {
		this.channelUtenti = channelUtenti;
	}
}
