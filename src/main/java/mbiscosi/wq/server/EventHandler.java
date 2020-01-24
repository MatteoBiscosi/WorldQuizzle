package mbiscosi.wq.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Random;

import mbiscosi.wq.server.WorkerSelector.Utilities;



public class EventHandler {
	
	private static int DEFAULT_SIZE = 1024;
	private static int DEFAULT_SO = 500;
	
	private String request;
	private String response;
	private ServerService server;
	private SelectableChannel channel;
	private SelectionKey key;
	private int selectorNum;
	
	
	public EventHandler(String request, ServerService server, SelectableChannel channel, SelectionKey key, int selectorNum) {
		this.request = request;
		this.server = server;
		this.channel = channel;
		this.key = key;
		this.selectorNum = selectorNum;
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
			login(splitting[1], splitting[2], splitting[3]);
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
			
		
		case "sfida":
			sfida(splitting[1], splitting[2]);
			break;	
			
			
		case "accettaSfida":
			accettaSfida(splitting[1], splitting[2], splitting[3]);
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
	
	public void login(String username, String password, String udpPort) {
		UserInfo tmp;
		
		
		if((tmp = server.getConnessioni().get(username)) != null) {
			if(tmp.getPassword().equals(password)) {
				if(tmp.getConnesso() == 1) {
					response = "-2";
					return;
				}
				tmp.setConnesso(1);
				tmp.setSocketChannel(channel);
				tmp.setUdpPort(Integer.parseInt(udpPort));
				
				System.out.println(udpPort);
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
	
	
	
	
	/*
	 * OPERAZIONE DI SFIDA:
	 * Abbiamo i seguenti possibili casi: 
	 * - Username o password sbagliati (ritorno un messaggio di risposta adeguato)
	 * - Utente già connesso (ritorno un messaggio di risposta adeguato e non gli permetto il login)
	 * - Username e password corretti (ritorno un messaggio affermativo e imposto la var. "connesso" della hashmap ad 1)
	 */
	
	public void sfida(String username, String amico) {
		UserInfo tmp, tmp2;
		
		
		if((server.getUtenti().get(username).contains(amico))) {
			
			tmp = server.getConnessioni().get(amico);
			tmp2 = server.getConnessioni().get(username);
			
			//amico connesso
			if(tmp.getConnesso() == 1) {
			
				tmp.getLock().lock();
				
				tmp2.getLock().lock();
				//amico attualmente già in un'altra sfida
				if(tmp.getAttualmenteInSfida() != 0 && tmp2.getAttualmenteInSfida() != 0) {
					response = "0";
					tmp.getLock().unlock();
					tmp2.getLock().unlock();
				}
				
				//amico non è in un'altra sfida
				else {
					tmp.setAttualmenteInSfida(1);
					tmp2.setAttualmenteInSfida(1);
					tmp.getLock().unlock();
					tmp2.getLock().unlock();
					
					if(setUdpComm(username, amico, tmp.getUdpPort()) == 1) {
						response = "attesaSfida";
						tmp2.setKey(key);
						tmp2.setSelectorNum(selectorNum);
					}
						
					else {
						tmp.getLock().lock();
						tmp2.getLock().lock();
						tmp.setAttualmenteInSfida(0);
						tmp2.setAttualmenteInSfida(0);
						tmp.getLock().unlock();
						tmp2.getLock().unlock();
						response = "-3";
					}
				}
				
			}
			
			//amico non connesso
			else {
				response = "-2";
			}
			
			return;
		}
		
		
		//Username non è amico di amico
		response = "-1";
	}
	
	
	
	private int setUdpComm(String username, String amico, int udpPort) {
		SocketChannel userSfidante = (SocketChannel) server.getConnessioni().get(username).getSocketChannel();
		InetSocketAddress sockaddr;
		try {
			sockaddr = (InetSocketAddress) userSfidante.getRemoteAddress();
			InetAddress inaddr = sockaddr.getAddress();
			System.out.println(inaddr);
		
			
			DatagramSocket tmpSock = new DatagramSocket();
			
			tmpSock.setSoTimeout(DEFAULT_SO);
			
				
			String req = "richiestaSfida " + username;
				
			byte[] reqBytes = req.getBytes();
				
			DatagramPacket sendPacket = new DatagramPacket(reqBytes, reqBytes.length, inaddr, udpPort);
				
			
				
			tmpSock.send(sendPacket);
			
			tmpSock.close();
			
		} catch (IOException e) {
			return -1;
		}
		
		
		return 1;
	}
	
	
	
	private void accettaSfida(String risposta, String username, String userAmico) {
		UserInfo lockSfida, lockSfida2;
		UserInfo user2 = server.getConnessioni().get(userAmico);
		lockSfida = server.getConnessioni().get(userAmico);
		lockSfida2 = server.getConnessioni().get(username);
		
		
		if(risposta.equals("si")) {
			if(user2.getConnesso() == 1) {
				Utilities tmp = (Utilities) user2.getKey().attachment();
				
				//Preparo la risposta da mandare al client che ha richiesto la sfida
				byte[] resp = "1".getBytes();
				tmp.buffer.put(resp);
				tmp.setInSfida(1);
				
				
				server.getSelector(user2.getSelectorNum()).setWrite(user2.getKey());
				
				//Preparo la risposta da mandare al client che ha accettato la sfida
				this.response = "1";
			} else {
				//In caso il client che ha chiesto la sfida si sia disconnesso
				this.response = "-1";
				
				lockSfida.getLock().lock();
				lockSfida2.getLock().lock();
				lockSfida.setAttualmenteInSfida(0);
				lockSfida2.setAttualmenteInSfida(0);
				lockSfida.getLock().unlock();
				lockSfida2.getLock().unlock();
			}
		} else {
			if(user2.getConnesso() == 1) {
				Utilities tmp = (Utilities) user2.getKey().attachment();
				
				//Preparo la risposta da mandare al client che ha richiesto la sfida
				byte[] resp = "2".getBytes();
				tmp.buffer.put(resp);
				tmp.setInSfida(1);
				
				
				server.getSelector(user2.getSelectorNum()).setWrite(user2.getKey());
			}
			
			lockSfida.getLock().lock();
			lockSfida2.getLock().lock();
			lockSfida.setAttualmenteInSfida(0);
			lockSfida2.setAttualmenteInSfida(0);
			lockSfida.getLock().unlock();
			lockSfida2.getLock().unlock();
			
			//Preparo la risposta da mandare al client che ha accettato la sfida
			this.response = "2";
		}
	}
}
