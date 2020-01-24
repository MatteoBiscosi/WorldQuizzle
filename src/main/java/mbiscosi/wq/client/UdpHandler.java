package mbiscosi.wq.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Random;



public class UdpHandler implements Runnable{
	//Dimensione standard del buffer e variabili per il calcolo dei tempi
	private static int DEFAULT_SIZE = 1024;
	

	byte[] reqBytes = new byte[DEFAULT_SIZE];
	byte[] respBytes = new byte[DEFAULT_SIZE];
	
	
	
	//Varie variabili della classe
	private DatagramChannel clientSock;
	private InetSocketAddress clientAdd;
	private InetAddress serverAdd;
	private int port;
	private static boolean terminate;
	private String username;
	private static Selector selector;
	private ByteBuffer buffer;
	
	
	
	
	//Istanzio tutti le variabili
	public UdpHandler(InetAddress serverAdd, int port, String username) throws IOException {
		this.serverAdd = serverAdd;
		this.port = port;
		this.username = username;
		
		this.port = port;
		selector = Selector.open();
		clientSock = DatagramChannel.open();
		clientAdd = new InetSocketAddress(this.port);
		
		
		clientSock.socket().bind(clientAdd);
		clientSock.configureBlocking(false);
		SelectionKey clientKey = clientSock.register(selector, SelectionKey.OP_READ);
		buffer = ByteBuffer.allocate(DEFAULT_SIZE);
		terminate = false;
	}
	
	
	
	public void run() {
		
		//Ciclo infinito del server
		while(!terminate) {
			try {
				selector.select();
				Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
				
				System.out.println("Server sveglio");
				//Scorro l'iterator con le chiavi che avevo "attivato"
				while(iterator.hasNext()) {
					SelectionKey clientKey = (SelectionKey) iterator.next();
					iterator.remove();
					
					
					//Caso di chiave non valida
					if(!clientKey.isValid()) {
						continue;
					}
					
					//Caso di chiave in lettura
					else if(clientKey.isReadable()) {
						readOp(clientKey);
					}
				}
			} catch (IOException e) {			
				if(Thread.interrupted())
					shutdown();
				else {
					System.err.println("Errore: " + e);
					shutdown();
				}
			}
		}
		
		System.out.println("Server UDP in chiusura");
		
		//In caso di richiesta termino il server
		try {
			selector.close();
			clientSock.close();
		} catch (IOException e) {}
	}
	
	
	//Operazione di lettura del dato e scrittura nel buffer
	//Con decisione sull'invio della risposta
	private void readOp(SelectionKey clientKey) {
		DatagramChannel channel = (DatagramChannel) clientKey.channel();
		
		//Leggo la richiesta scritta e deciso se rispondere
		try {
			channel.receive(buffer);
		} catch (IOException e) {
			clientKey.cancel();
			try {
				channel.close();
			} catch (IOException e1) {}
			return;
		}
		
		buffer.flip();
		
		byte[] tmpByte = new byte[buffer.limit()];
		
		buffer.get(tmpByte);
		
		buffer.clear();
		
		String request = new String(tmpByte);
		
		MainClassClient.richiestaSfida = request;
		
		MainClassClient.sfida.set(true);
		System.out.println("Richiesta di sfida arrivata, inserire qualsiasi valore per visualizzarla.\r\nHai 10 secondi per poterla accettare");
		MainClassClient.setTimer(System.currentTimeMillis());
		timeout();
	}

	
	
	public static void shutdown() {
		terminate = true;
		selector.wakeup();
		System.out.println("Server svegliato");
	}
	
	
	private void timeout() {
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {}
		
		
		if(MainClassClient.getSfidaVisualizzata()) {
			MainClassClient.setSfidaVisualizzata(false);
			MainClassClient.sfida.set(false);
		}
		
		else {
			MainClassClient.setSfidaVisualizzata(false);
			MainClassClient.sfida.set(false);
			MainClassClient.sendNo();
			System.out.println("Timer scaduto, sfida annullata...");
		}
	}
}
