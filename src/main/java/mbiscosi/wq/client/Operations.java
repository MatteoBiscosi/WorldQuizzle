package mbiscosi.wq.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;




public interface Operations {
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
	public String login(String username, String password);
	
	
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
	public String registrazione(String username, String password) throws RemoteException, NotBoundException;
	
	
	/*
	 * LOGOUT
	 * 
	 * effettua il logout dell'utente, solo se l'utente username era effettivamente loggato.
	 * 
	 * Returns: ritorna il messaggio di risposta del server
	 * 
	 * Exceptions: 
	 */
	public String logout(String username);
	
	
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
	public String aggiungiAmico(String username, String userAmico);
	
	
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
	public String listaAmici(String username);
	
	
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
	 *  Returns: messaggio di risposta dal server;
	 *  
	 *  Exceptions: 
	 */
	public String sfida(String username, String userAmico);
	
	
	/*
	 * MOSTRA_PUNTEGGIO
	 * 
	 * mostra il punteggio totale dell'utente username
	 * 
	 * Returns: punteggio dell'utente
	 */
	public String mostraPunteggio(String username);
	
	
	/*
	 * MOSTRA_CLASSIFICA
	 * 
	 * mostra la classifica tra gli utenti username e la sua lista di amici. 
	 * 
	 * Returns: classifica, in ordine di punteggio decrescente, degli amici di username e username
	 * 
	 * Exceptions: 
	 */
	public String mostraClassifica(String username);
	
	
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
	public String accettaSfida(String response, String username, String userAmico); 
	
	
	/*
	 * Mostra i vari comandi disponibili
	 */
	public void help();
}
