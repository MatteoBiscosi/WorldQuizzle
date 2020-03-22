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
	/*
	 * Classe che gestisce i vari eventi richiesti dai clients
	 */
	
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
	
	
	//Metodo in cui faccio lo switch case sulla richiesta ricevuta per individuare l'operazione richiesta
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
			
			
		case "sfidaPronto":
			sfidaPronta(splitting[1]);
			break;
			
			
		case "parolaSfida":
			parolaSfida(Integer.parseInt(splitting[1]), splitting[2], Integer.parseInt(splitting[3]));
			break;
			
			
		case "rispostaParola":
			rispostaParola(Integer.parseInt(splitting[1]), splitting[2], Integer.parseInt(splitting[3]), splitting[4]);
			break;
			
			
		case "termineSfida":
			termineSfida(splitting[1], Integer.parseInt(splitting[2]), splitting[3]);
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
		
		//Controllo che username sia corretto
		if((tmp = server.getConnessioni().get(username)) != null) {
			//Controllo della password
			if(tmp.getPassword().equals(password)) {
				//Controllo che non sia gia' connesso
				if(tmp.getConnesso() == 1) {
					response = "-2";
					return;
				}
				tmp.setConnesso(1);
				tmp.setSocketChannel(channel);
				tmp.setUdpPort(Integer.parseInt(udpPort));
				
				server.getChannelUtenti().put(channel, username);
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
		
		//Controllo username
		if((tmp = server.getConnessioni().get(username)) != null) {
			
			//Controllo che sia connesso
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
	 * - username esiste ma utente non connesso (ritorno messaggio di errore adeguato)
	 * - username e userAmico esistono e l'aggiunta va a buon fine (ritorno messaggio di successo)
	 * - userAmico non esiste (ritorno messaggio di errore adeguato)
	 */
	public void aggiungiAmico(String username, String userAmico) {
		ArrayList<String> tmp;
		
		tmp = server.getUtenti().get(username);
		
		//Amico non esistente
		if(!server.getUtenti().containsKey(userAmico))
			response = "0";
		
		//Amico gia' nella lista di amici
		else if(tmp.contains(userAmico))
			response = "-2";
		
		else {
			response = "1";
			server.getJson().getLock().lock();
			server.getJson().scriviJSON(username, null, userAmico, null);
			server.getJson().getLock().unlock();
			tmp.add(userAmico);
		}
	}
	
	
	
	
	
	/*
	 * OPERAZIONE DI LISTA AMICI:
	 * Metodo che restituisce la lista degli amici di username
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
		
		response = server.getJson().scriviJSONAmici(punteggioAmici, 1);
		
		response = response.length() + "\r\n" + response;
	}
	
	
	
	
	/*
	 * OPERAZIONE DI SFIDA:
	 * Abbiamo i seguenti possibili casi: 
	 * - amico non presente nella lista di amici di username;
	 * - amico connesso;
	 * - amico non connesso;
	 * - amico gia' in una sfida;
	 * - amico disponibile alla sfida;
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
					
					//Messaggio di sfida inviato all'amico
					if(setUdpComm(username, amico, tmp.getUdpPort()) == 1) {
						response = "attesaSfida";
						tmp2.setKey(key);
						tmp2.setSelectorNum(selectorNum);
					}
						
					else {
						//Messaggio non possibile da inviare per errori vari
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
	
	
	
	
	/*
	 * Metodo utilizzato per inviare la richiesta di sfida da username ad amico tramite protocollo UDP
	 */
	private int setUdpComm(String username, String amico, int udpPort) {
		SocketChannel userSfidante = (SocketChannel) server.getConnessioni().get(username).getSocketChannel();
		InetSocketAddress sockaddr;
		try {
			sockaddr = (InetSocketAddress) userSfidante.getRemoteAddress();
			InetAddress inaddr = sockaddr.getAddress();
			System.out.println(inaddr);
		
			//Stabilisco un datagram socket
			DatagramSocket tmpSock = new DatagramSocket();
			
			tmpSock.setSoTimeout(DEFAULT_SO);
			
				
			String req = "richiestaSfida " + username;
				
			byte[] reqBytes = req.getBytes();
				
			//Invio il pacchetto ad amico, utilizzando le info che ho
			DatagramPacket sendPacket = new DatagramPacket(reqBytes, reqBytes.length, inaddr, udpPort);
				
			
				
			tmpSock.send(sendPacket);
			
			//Chiudo il datagramSocket
			tmpSock.close();
			
		} catch (IOException e) {
			return -1;
		}
		
		
		return 1;
	}
	
	
	
	/*
	 * ACCETTA SFIDA
	 * Metodo utilizzato da username, che ha ricevuto la richiesta di sfida da parte di userAmico,
	 * per inviare la risposta all'amico; in caso la risposta sia affermativa, creo la nuova entry 
	 * all'interno della map delle sfide e mi salvo le varie info necessarie alla sfida.
	 * In caso negativo, invio il messaggio di risposta all'altro utente che ha richiesto la sfida.
	 */
	public void accettaSfida(String risposta, String username, String userAmico) {
		UserInfo lockSfida, lockSfida2;
		UserInfo user2 = server.getConnessioni().get(userAmico);
		lockSfida = server.getConnessioni().get(userAmico);
		lockSfida2 = server.getConnessioni().get(username);
		
		
		if(risposta.equalsIgnoreCase("si")) {
			if(user2.getConnesso() == 1) {
				//Genero la partita, inserendo un nuovo elemento nella map corrispondente
				Random randSfide = new Random();
				int numSfide = randSfide.nextInt(10);
				
				//numero casuale delle parole da tradurre
				while(numSfide < 4) {
					numSfide = randSfide.nextInt(10);
				}
				
				System.out.println("Preparazione delle parole " + numSfide);
				
				ChallengeUtilities prova = new ChallengeUtilities(numSfide, server);
				
				server.getMapSfida().put(userAmico + username, prova);
				
				//inserisco le info necessarie per la terminazione corretta della sfida
				lockSfida.setKeySfida(userAmico + username);
				lockSfida.setTipo(0);
				lockSfida2.setKeySfida(userAmico + username);
				lockSfida2.setTipo(1);
				
				//Preparo la risposta da mandare al client che ha accettato la sfida
				this.response = "1";	
				
				Utilities tmp = (Utilities) user2.getKey().attachment();
				
				//Preparo la risposta da mandare al client che ha richiesto la sfida
				byte[] resp = "1".getBytes();
				tmp.buffer.put(resp);
				tmp.setInSfida(1);
				
				
				server.getSelector(user2.getSelectorNum()).setWrite(user2.getKey());
				
							
			} else {
				//Risposta negativa alla sfida, in caso l'utente che la ha richiesta si sia disconnesso
				this.response = "-1";
				
				lockSfida.getLock().lock();
				lockSfida2.getLock().lock();
				lockSfida.setAttualmenteInSfida(0);
				lockSfida2.setAttualmenteInSfida(0);
				lockSfida.getLock().unlock();
				lockSfida2.getLock().unlock();
			}
		} else {
			//Risposta negativa alla sfida
			if(user2.getConnesso() == 1) {
				//Se l'utente che ha richiesto la sfida non si è disconnesso, gli comunico la risposta
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
	
	
	
	
	
	
	/*
	 * SFIDA PRONTA
	 * 
	 * Metodo utilizzato per tradurre la parole della sfida e restituire ai clients il numero di parole da tradurre
	 * 
	 * Sequenza: - Controllo che non siano state tradotte e che la variabile serverRaggiungibile sia == 1, 
	 * 				altrimenti il server di traduzione non è raggiungibile e rispondo con un errore
	 * 
	 * 			 - Richiamo la funzione translateWords, se catcho una IoException annullo la sfida
	 * 
	 * 			 - Ritorno 1 in caso la traduzione sia andata bene, -1 in caso alternativo
	 * 
	 * 			 - Se la variabile serverRaggiungibile == 0, vuol dire che l'altro sfidante ha già chiesto la traduzione
	 * 				ma il servizio non è raggiungibile, per cui tolgo la keyMap dalla sfida e la termino
	 */

	public void sfidaPronta(String keyMap) {
		
		ChallengeUtilities tmp = server.getMapSfida().get(keyMap);
		
		Integer numParole = tmp.getNumParole();
		
		tmp.getLockParoleSfida().lock();
		
		if(tmp.getServerRaggiungibile() == 1) {
			if(tmp.getTraduzione() == null) {
				try {
					tmp.translateWords();
				} catch (IOException e) {
					tmp.setServerRaggiungibile(0);
					tmp.getLockParoleSfida().unlock();
					this.response = "-1";
					return;
				}
			}
			
			tmp.getLockParoleSfida().unlock();
			this.response =  numParole.toString();
		}
		
		else {
			tmp.getLockParoleSfida().unlock();
			this.response = "-1";
			return;
		}
	}
	
	
	
	/*
	 * RISPOSTA PAROLA
	 * 
	 * Metodo utilizzato per verificare la corretta traduzione della parola da parte di uno degli utenti
	 * e l'aggiornamento del punteggio nella struttura dati
	 */
	public void rispostaParola(int numParola, String keyMap, int tipo, String traduzione) {
		ChallengeUtilities tmp = server.getMapSfida().get(keyMap);
		
		if(tmp == null)
			System.out.println("prova");
		
		this.response = tmp.checkWords(numParola, traduzione, tipo);;
	}
	
	
	
	/*
	 * TERMINE SFIDA
	 *
	 * Metodo utilizzato per concludere la sfida in modo corretto, ossia la comunicazione dei risultati della sfida e
	 * l'aggiornamento dei vari punteggi
	 */
	public void termineSfida(String keyMap, int tipo, String username) {
		ChallengeUtilities tmp = server.getMapSfida().get(keyMap);
		
		switch(tipo) {
		
			//caso Sfidante
			case 0:
				tmp.setIndexSfidante(tmp.getNumParole() - 1);
				
				//Controllo che l'altro utente abbia finito, in caso negativo invio -1 come risposta
				if(tmp.getIndexSfidato() != (tmp.getNumParole() - 1) && tmp.getUserSfidato() == 0) {
					this.response = "-1";
				}
				else {
					//In caso positivo, verifico i punteggi, il vincitore
					tmp.getLockParoleSfida().lock();
					tmp.setUserSfidante(1);
					
					int punteggio = tmp.getPuntSfidante();
					
					//vittoria
					if(punteggio > tmp.getPuntSfidato())
						this.response = "Congratulazioni, hai vinto!\r\nHai totalizzato " + punteggio + 
									" punti, mentre l'avversario ha totalizzato " + tmp.getPuntSfidato() + " punti";
					//sconfitta
					else if(punteggio < tmp.getPuntSfidato())
						this.response = "Spiacente, hai perso!\r\nHai totalizzato " + punteggio + 
						" punti, mentre l'avversario ha totalizzato " + tmp.getPuntSfidato() + " punti";
					
					else 
						this.response = "Pareggio!\r\nHai totalizzato " + punteggio + 
								" punti, mentre l'avversario ha totalizzato " + tmp.getPuntSfidato() + " punti";
					
					//in caso che l'altro utente abbia già terminato la sfida, elimino il record della partita dalla map corrispondente
					if(tmp.getUserSfidato() == 1) {
						tmp.getLockParoleSfida().unlock();
						server.getMapSfida().remove(keyMap);
					}
					else
						tmp.getLockParoleSfida().unlock();
					
					
					UserInfo tmpUser = server.getConnessioni().get(username);
					
					punteggio = tmpUser.getPunteggio() + punteggio;
					
					if(punteggio > 0) 						
						tmpUser.setPunteggio(punteggio);
					
					else {
						tmpUser.setPunteggio(0);
						punteggio = 0;
					}
					
					//Infine aggiorno il JSON col nuovo punteggio se è diverso da 0
					
					
					server.getJson().getLock().lock();
					server.getJson().scriviJSON(username, null, null, Integer.toString(punteggio));
					server.getJson().getLock().unlock();
					tmpUser.setPunteggio(punteggio);
					
					server.getConnessioni().get(username).setAttualmenteInSfida(0);
				}
				
				break;
				
			//caso Sfidato, stesso procedimento descritto nel caso dello sfidante
			case 1:
				tmp.setIndexSfidato(tmp.getNumParole() - 1);
				
				if(tmp.getIndexSfidante() != (tmp.getNumParole() - 1) && tmp.getUserSfidante() == 0) {
					this.response = "-1";
				}
				else {
					tmp.getLockParoleSfida().lock();
					tmp.setUserSfidato(1);
					
					int punteggio = tmp.getPuntSfidato();
					
					//vittoria
					if(punteggio > tmp.getPuntSfidante())
						this.response = "Congratulazioni, hai vinto!\r\nHai totalizzato " + punteggio + 
									" punti, mentre l'avversario ha totalizzato " + tmp.getPuntSfidante() + " punti";
					//sconfitta
					else if(punteggio < tmp.getPuntSfidante())
						this.response = "Spiacente, hai perso!\r\nHai totalizzato " + punteggio + 
						" punti, mentre l'avversario ha totalizzato " + tmp.getPuntSfidante() + " punti";
					//pareggio
					else 
						this.response = "Pareggio!\r\nHai totalizzato " + punteggio + 
								" punti, mentre l'avversario ha totalizzato " + tmp.getPuntSfidato() + " punti";
					

					if(tmp.getUserSfidante() == 1) {
						tmp.getLockParoleSfida().unlock();
						server.getMapSfida().remove(keyMap);
					}
					else 
						tmp.getLockParoleSfida().unlock();
					
					
					UserInfo tmpUser = server.getConnessioni().get(username);
					
					punteggio = tmpUser.getPunteggio() + punteggio;
					
					if(punteggio > 0) 						
						tmpUser.setPunteggio(punteggio);
					
					else {
						tmpUser.setPunteggio(0);
						punteggio = 0;
					}
					
					server.getJson().getLock().lock();
					server.getJson().scriviJSON(username, null, null, Integer.toString(punteggio));
					server.getJson().getLock().unlock();
					tmpUser.setPunteggio(punteggio);
					
					server.getConnessioni().get(username).setAttualmenteInSfida(0);
				}
				
				break;
		}
	}
	
	
	
	
	/*
	 * PAROLA SFIDA
	 * 
	 * Metodo utilizzato per ottenere la i-esima parola della sfida richiesta dal client
	 */
	public void parolaSfida(int i, String keyMap, int tipo) {
		ChallengeUtilities tmp = server.getMapSfida().get(keyMap);
		
		this.response =  tmp.getParoleSfida().get(i);
	}
}
