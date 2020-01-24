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

	private static final int BUFFER_SIZE = 2048;
	private boolean terminate = false;
	private Selector selector;
	private EventHandler req;
	private ServerService server;
	private int selectorNum;

	
	
	public WorkerSelector(ServerService server, int selectorNum) throws IOException{
		this.selector = Selector.open();
		this.server = server;
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
	
	
	

	private void readRequest(SelectionKey key) {

		System.out.println("Messaggio arrivato");

		SocketChannel client = (SocketChannel) key.channel();	
		Utilities tmp = (Utilities) key.attachment();
		ByteBuffer buffer = tmp.buffer;
		
		System.out.println("Arriva richiesta di lettura...");
		
		
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
			System.out.println(request);
			
			req = new EventHandler(request, server, key.channel(), key, selectorNum);
			
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
	
	
	
	
	private void writeResponse(SelectionKey key) {
		

		SocketChannel client = (SocketChannel) key.channel();	
		Utilities tmp = (Utilities) key.attachment();
		ByteBuffer buffer = tmp.buffer;
		
		
		try {
			String response;
			
			if(tmp.getInSfida() == 0) {
				response = req.process();
				if(response.equals("attesaSfida")) {
					key.interestOps(SelectionKey.OP_READ);
					return;
				}
				
				byte[] resp = response.getBytes();
				buffer.put(resp);
				System.out.println(response);
			}
			else
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
	
	
	
	
	
	private void chiudiConnessione(SelectableChannel selectableChannel) {
		Iterator<Entry<String, UserInfo>> it = server.getConnessioni().entrySet().iterator();
		while (it.hasNext()) {
			ConcurrentHashMap.Entry<String, UserInfo> pair = (ConcurrentHashMap.Entry<String, UserInfo>) it.next();
			if(pair.getValue().getSocketChannel() != null && pair.getValue().getSocketChannel().isOpen()) {
				pair.getValue().setSocketChannel(null);
				pair.getValue().setConnesso(0);
			}
		}
	}
	
	
	
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
	
	public class Utilities {
		public ByteBuffer buffer;
		public long timer;
		private int inSfida;
		private int sfidaAccetta;
		
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
	}
}
