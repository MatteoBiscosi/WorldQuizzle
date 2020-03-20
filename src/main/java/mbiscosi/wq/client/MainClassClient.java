package mbiscosi.wq.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicBoolean;



public class MainClassClient {
	

	public static boolean terminazione = false;
	public static AtomicBoolean sfida = new AtomicBoolean(false);
	public static String richiestaSfida;
	
	private static SocketAddress address;
	private static SocketChannel client;
	private static Operations op;
	private static String username;
	private static BufferedReader reader;
	private static long timer;
	private static AtomicBoolean sfidaVisualizzata = new AtomicBoolean(false);
	
	
	
	public static void main(String[] args) {
		
	
		op = new OperationsImpl(address, client);
		
		
		//Parametri usati per leggere la stringa messa in input
		reader = new BufferedReader(new InputStreamReader(System.in));
		String request = new String();
		
		System.out.println("Benvenuto in Word Quizzle");
		
		
		//Ciclo continuo del client
		do {
			
			/*
			 * Caso particolare della RICHIESTA DI SFIDA
			 */
			try {
				if(sfida.get()) {
					
					setSfidaVisualizzata(true);
					
					System.out.println(richiestaSfida);
					
					if(System.currentTimeMillis() - timer > 10000) {
						System.out.println("Timer scaduto, sfida annullata...");
						sfida.set(false);
						break;
					}
					
					String[] splitting;
					
					splitting = richiestaSfida.split(" ");
					
					System.out.println("Sfida richiesta da " + splitting[1]);
					
					System.out.println("Si desidera accettare la sfida:\r\nSI\r\nNO");
					
					
					request = reader.readLine();
					
					do {
						if(request.equalsIgnoreCase("SI")) {
							if(System.currentTimeMillis() - getTimer() > 10000) {
								System.out.println("Timer scaduto, sfida annullata...");
								op.accettaSfida("no", username, splitting[1]);
								sfida.set(false);
								break;
							}
							op.accettaSfida("si", username, splitting[1]);
							break;
						}
						
						else if(request.equalsIgnoreCase("NO")) {
							if(System.currentTimeMillis() - getTimer() > 10000) {
								System.out.println("Timer scaduto, sfidata annullata...");
								System.out.println(op.accettaSfida("no", username, splitting[1]));
								sfida.set(false);
								break;
							}
							System.out.println(op.accettaSfida("no", username, splitting[1]));
							break;
						}
						
						System.out.println("Si prega di inserire una delle due risposte proposte...");
					} while(true);
				}
			} catch(IOException e) {
				System.err.println(e);
				System.out.println("Errore nella lettura della sfida. Sfida annullata\r\n");
				sfida.set(false);
				continue;
			}
			
			
			
			try {
				request = reader.readLine();
			} catch(IOException e2) {
				//reader = new BufferedReader(new InputStreamReader(System.in));
				//System.in.
				continue;
			}
			
			String[] splitting;
			
			try {
				splitting = request.split(" ");
			} catch(NullPointerException e) {
				System.out.println("Errore nell'inserimento del comando, si prega di riprovare.\r\n");
				continue;
			}
			
			
			
			/*
			 * REGISTRAZIONE
			 */
			if(splitting[0].equalsIgnoreCase("registra_utente")) {
				if(splitting.length != 3 || splitting[1] == null || splitting[2] == null) {
					System.out.println("Errore nell'inserimento dei paramentri;\r\nregistra_utente <username> <password>\r\nI parametri non possono essere nulli");
					continue;
				}
				try {
					System.out.println(op.registrazione(splitting[1], splitting[2]));
				}
				catch (RemoteException | NotBoundException e) {
					System.out.println("Server al momento non raggiungibile, riprovare più tardi");
				}
			}
			
			
			/*
			 * LOGIN
			 */
			else if(splitting[0].equalsIgnoreCase("login")) {
				if(splitting.length != 3 || splitting[1] == null || splitting[2] == null) {
					System.out.println("Errore nell'inserimento dei paramentri;\r\nlogin <username> <password>\r\n");
					continue;
				}
				
				String response = op.login(splitting[1], splitting[2]);
				if(response.equals("Login avvenuto con successo"));
					username = splitting[1];
					
				System.out.println(response);
			}
			
			
			/*
			 * LOGOUT
			 */
			else if(splitting[0].equalsIgnoreCase("logout")) {
				if(splitting.length != 1) {
					System.out.println("Errore nell'inserimento dei paramentri;\r\nlogout\r\n");
					continue;
				}
				
				System.out.println(op.logout(username));
				username = null;
			}
			
			
			
			/*
			 * AGGIUNTA AMICO
			 */
			else if(splitting[0].equalsIgnoreCase("aggiungi_amico")) {
				if(splitting.length != 2 || splitting[1] == null) {
					System.out.println("Errore nell'inserimento dei paramentri;\r\naggiungi_amico <userAmico>\r\n");
					continue;
				}
				
				System.out.println(op.aggiungiAmico(username, splitting[1]));
			}
			
			
			
			/*
			 * MOSTRA LISTA DI AMICI
			 */
			else if(splitting[0].equalsIgnoreCase("lista_amici")) {
				if(splitting.length != 1) {
					System.out.println("Errore nell'inserimento dei paramentri;\r\nlista_amici\r\n");
					continue;
				}
				
				System.out.println(op.listaAmici(username));
			}
			
			
			
			/*
			 * MOSTRA PUNTEGGIO UTENTE
			 */
			else if(splitting[0].equalsIgnoreCase("mostra_punteggio")) {
				if(splitting.length != 1) {
					System.out.println("Errore nell'inserimento dei paramentri;\r\nmostra_punteggio\r\n");
					continue;
				}
				
				System.out.println(op.mostraPunteggio(username));
			}
			
			
			
			
			/*
			 * MOSTRA CLASSIFICA TRA UTENTE E GLI AMICI DI UTENTE
			 */
			else if(splitting[0].equalsIgnoreCase("mostra_classifica")) {
				if(splitting.length != 1) {
					System.out.println("Errore nell'inserimento dei paramentri;\r\nmostra_classifica\r\n");
					continue;
				}
				
				System.out.println(op.mostraClassifica(username));
			}
			
			
			
			/*
			 * SFIDA
			 */
			else if(splitting[0].equalsIgnoreCase("sfida")) {
				if(splitting.length != 2) {
					System.out.println("Errore nell'inserimento dei paramentri;\r\nsfida <userAmico>\r\n");
					continue;
				}
				
				op.sfida(username, splitting[1]);
			}
			
			
			
			
			/*
			 * TERMINAZIONE
			 */
			else if(request.equals("termina"))
				terminazione = true;
			
			
			/*
			 * Caso di default, nessun comando corretto inserito
			 */
			else 
				System.out.println("\r\nComando non riconosciuto...\r\n");
			
			System.out.println();
			
		} while(!terminazione);
	}
	
	
	public static synchronized long getTimer() {
		return timer;
	}
	
	public static synchronized void setTimer(long timer) {
		MainClassClient.timer = timer;
	}
	
	
	
	public static boolean getSfidaVisualizzata() {
		return sfidaVisualizzata.get();
	}


	public static void setSfidaVisualizzata(boolean bool) {
		MainClassClient.sfidaVisualizzata.set(bool);;
	}


	public static void sendNo() {
		String[] splitting = richiestaSfida.split(" ");
		System.out.println("Timer scaduto, sfidata annullata...");
		op.accettaSfida("no", username, splitting[1]);
		sfida.set(false);
	}
}
