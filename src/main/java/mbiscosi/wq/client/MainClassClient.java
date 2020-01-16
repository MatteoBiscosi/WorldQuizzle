package mbiscosi.wq.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;



public class MainClassClient {
	

	public static boolean terminazione = false;
	
	private static SocketAddress address;
	private static SocketChannel client;
	private static Operations op;
	private static String username;
	
	
	
	
	public static void main(String[] args) {
		
	
		op = new OperationsImpl(address, client);
		
		
		//Parametri usati per leggere la stringa messa in input
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String request = new String();
		
		
		//Ciclo continuo del client
		do {
			try {
				/*
				 * Leggo la richiesta, faccio il parsing della richiesta, lo richiedo correttamente al server
				 * Se la richiesta non corrisponde a nessuna procedura, mando un messaggio di errore
				 */
				request = reader.readLine();
				
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
				 * LOGOUT
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
				
			} catch (Exception e1) {
				e1.printStackTrace();
				terminazione = true;
			}
			
		} while(!terminazione);
	}
}
