package mbiscosi.wq.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
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
	
	private static int BUFFER_SIZE = 1024;
	private static int DEFAULT_PORT = 13200;
	
	
	private ConcurrentHashMap<String, UserInfo> connessioni;
	private ConcurrentHashMap<String, ArrayList<String>> utenti;
	private ConcurrentHashMap<String, ChallengeUtilities> mapSfida;
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
			//e saranno quelli che accetteranno le richieste dei clients
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
				System.out.println("Server in attesa sulla select...");
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
			Thread tmp = new Thread(acceptor);
			tmp.start();
		}
	}
	
	
	
	/*
	 * Classe utilizzata per fare da Acceptor
	 */
	private class Acceptor implements Runnable{
		public synchronized void run() {
			try {
				SocketChannel client = server.accept();
				//client.
				
				
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
	
	
	private void stampaParole() {
		for(int i = 0; i < parole.size(); i++) {
			System.out.println(parole.get(i));
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
	
	
	public void stampaConnessioni() {
		Iterator<Entry<String, UserInfo>> it = connessioni.entrySet().iterator();
		while (it.hasNext()) {
			ConcurrentHashMap.Entry<String, UserInfo> pair = (ConcurrentHashMap.Entry<String, UserInfo>) it.next();
			System.out.println(pair.getKey() + ", " + pair.getValue().getPassword() + ", " + pair.getValue().getPunteggio() + ", " + pair.getValue().getConnesso());
		}
	}
	
	public void stampaUtenti() {
		Iterator<Entry<String, ArrayList<String>>> it = utenti.entrySet().iterator();
		while (it.hasNext()) {
			ConcurrentHashMap.Entry<String, ArrayList<String>> pair = (ConcurrentHashMap.Entry<String, ArrayList<String>>) it.next();
			System.out.print(pair.getKey() + ", Amici -> ");
			for(String amici : pair.getValue())
				System.out.print(amici + ", ");
			System.out.println();
		}
	}
}
