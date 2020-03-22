package mbiscosi.wq.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;




public class WorkerSelector implements Runnable{
	/*
	 * Classe che gestisce le varie richieste dei clients
	 */

	private static final int BUFFER_SIZE = 2048;
	private boolean terminate = false;
	private Selector selector;
	private ServerService server;
	private int selectorNum;

	
	
	public WorkerSelector(ServerService server, int selectorNum) throws IOException{
		this.selector = Selector.open();
		this.server = server;
		
		//identificatore del selector
		this.selectorNum = selectorNum;
	}
	
	
	
	
	@Override
	public synchronized void run() {
		while(!terminate) {
			try {
				//mi metto in attesa sulla select
				selector.select();
				
				//creo il set di key e un iterator su cui scorrere le chiavi della select
				Set<SelectionKey> set = selector.selectedKeys(); 
				Iterator<SelectionKey> iterator = set.iterator();
				
				
				while(iterator.hasNext()) {
					SelectionKey key = iterator.next();
					iterator.remove();
					
					
					//Controllo se qualcuno mi ha scritto un nuovo messaggio
					if(key.isReadable()) {
						readRequest(key);
					}
					
					//Se ho letto un messaggio, ho settao la key a Write, per cui scrivo e solo dopo aver
					//scritto quello che c'era nel buffer ritorno in lettura
					else if(key.isWritable()) {
						writeResponse(key);
					}
				}
				
				set.clear();
			} catch (IOException e) {			
				if(Thread.interrupted())
					continue;
				else
					System.err.println("Errore: " + e);
			}
		}
		
		
		try {
			selector.close();
			System.out.println("Selector terminato");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	/*
	 * Metodo utilizzato per leggere le richieste dei clients, una volta lette
	 * creo un nuovo oggetto di tipo EventHandler che utilizzero' per gestire le richieste
	 */
	private void readRequest(SelectionKey key) {

		SocketChannel client = (SocketChannel) key.channel();	
		Utilities tmp = (Utilities) key.attachment();
		ByteBuffer buffer = tmp.buffer;
				
		
		//leggo il nuovo messaggio a blocchi di 1024 bytes
		try {
			int readed = 0;
			int offset = 0;
			while((offset = client.read(buffer)) > 0) {
				readed += offset;
			}
			
			
			//Controllo che la connessione non sia stata terminata dal client
			//Altrimenti chiudo la connessione
			if(offset == -1) {
				try {
					key.cancel();
					System.out.println("Connessione con " + key.channel() + " terminata.");
					chiudiConnessione(client);
					key.channel().close();
					return;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			
			
			buffer.flip();
			
			//Leggo la richiesta
			byte[] requestBytes = new byte[readed];
			buffer.get(requestBytes, 0, readed);
			String request = new String(requestBytes);
			
			tmp.setReq(new EventHandler(request, server, key.channel(), key, selectorNum));
			
			buffer.clear();
		} catch (IOException e) {
			try {
				System.out.println("Connessione con " + key.channel() + " terminata.");
				chiudiConnessione(key.channel());
				key.channel().close();
				return;
			} catch (IOException e1) {}
		}
		
		key.interestOps(SelectionKey.OP_WRITE);
	}
	
	
	
	/*
	 * Metodo utilizzato per gestire le risposte ai clients
	 */
	private void writeResponse(SelectionKey key) {
		

		SocketChannel client = (SocketChannel) key.channel();	
		Utilities tmp = (Utilities) key.attachment();
		ByteBuffer buffer = tmp.buffer;
		
		
		try {
			String response;
			
			//Gestisco le richieste
			if(tmp.getInSfida() == 0) {
				response = tmp.getReq().process();
				
				//Caso particolare utilizzato per gestire la richiesta di sfida da parte di un client A
				if(response.equals("attesaSfida")) {
					key.interestOps(SelectionKey.OP_READ);
					return;
				}
				
				byte[] resp = response.getBytes();
				buffer.put(resp);
			}
			else
				//Utilizzato per gestire la richiesta di sfida
				tmp.setInSfida(0);
			

			buffer.flip();
			
			
			client.write(buffer);
			
			//Controllo che non ci sia altro da leggere e setto in lettura e pulisco il buffer
			if(!buffer.hasRemaining()) {
				key.interestOps(SelectionKey.OP_READ);
				buffer.clear();
			}
			
		//In caso di errori durante la scrittura, è stata terminata la connessione
		//da parte del client e termino
		} catch (IOException | NullPointerException e) {
			key.cancel();
			try {
				System.out.println("Connessione con " + key.channel() + " terminata.");
				chiudiConnessione(client);
				key.channel().close();
			} catch (IOException e1) {}
		}	
	}
	
	
	
	/*
	 * Metodo utilizzato per chiudere la connessione ed eliminare eventuali dati inutili dei client che si disconnettono
	 */
	private void chiudiConnessione(SelectableChannel selectableChannel) {
		if(server.getChannelUtenti().containsKey(selectableChannel)) {
			//Qui modifico i dati presenti nella map "connessioni"
			String utente = server.getChannelUtenti().get(selectableChannel);
			
			
			UserInfo tmpInfo = server.getConnessioni().get(utente);
			
			tmpInfo.setSocketChannel(null);
			tmpInfo.setConnesso(0);
			tmpInfo.setAttualmenteInSfida(0);
			
			
			String keySfida = tmpInfo.getKeySfida();
			//Qui controllo che l'utente non sia in sfida altrimenti modifico i valori corretti per indicare che l'utente si e' disconnesso
			if(keySfida != null) {
				if(server.getMapSfida().containsKey(keySfida)) {
					ChallengeUtilities tmp = server.getMapSfida().get(keySfida);
					switch(tmpInfo.getTipo()) {
						case 0:
							//Caso dello sfidante
							tmp.getLockParoleSfida().lock();
							if(tmp.getUserSfidato() == 1) {
								tmp.getLockParoleSfida().unlock();
								server.getMapSfida().remove(keySfida);
							}
							else {
								tmp.setUserSfidante(1);
								tmp.getLockParoleSfida().unlock();
							}
							
							break;
							
						case 1:
							//Caso dello sfidato
							tmp.getLockParoleSfida().lock();
							if(tmp.getUserSfidante() == 1) {
								tmp.getLockParoleSfida().unlock();
								server.getMapSfida().remove(keySfida);
							}
							else {
								tmp.setUserSfidato(1);
								tmp.getLockParoleSfida().unlock();
							}
							
							break;
					}
				}
			}
		}
	}
	
	
	
	//Metodo utilizzato per aggiungere il client al selector
	public void addClient(SocketChannel client) throws IOException{
		System.out.println(client);
		client.configureBlocking(false);
		SelectionKey key = client.register(this.selector, SelectionKey.OP_READ);
		key.attach(new Utilities());
		this.selector.wakeup();
	}
	
	
	
	public void shutdown() {
		terminate = true;
		selector.wakeup();
	}
	
	
	public void setWrite(SelectionKey key) {
		key.interestOps(SelectionKey.OP_WRITE);
		selector.wakeup();
	}
	
	
	//Classe utilizzata per assegnare ad ogni chiave del selector, un insieme di valori utilizzati per le diverse operazioni
	public class Utilities {
		public ByteBuffer buffer;
		public long timer;
		private int inSfida;
		private int sfidaAccetta;
		private EventHandler req;
		
		
		public Utilities() {
			buffer = ByteBuffer.allocate(BUFFER_SIZE);
			inSfida = 0;
		}

		public synchronized int getInSfida() {
			return inSfida;
		}

		public synchronized void setInSfida(int inSfida) {
			this.inSfida = inSfida;
		}

		public int getSfidaAccetta() {
			return sfidaAccetta;
		}

		public void setSfidaAccetta(int sfidaAccetta) {
			this.sfidaAccetta = sfidaAccetta;
		}

		public EventHandler getReq() {
			return req;
		}

		public void setReq(EventHandler req) {
			this.req = req;
		}

	}
}
