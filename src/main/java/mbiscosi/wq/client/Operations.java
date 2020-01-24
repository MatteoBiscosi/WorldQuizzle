package mbiscosi.wq.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;




public interface Operations {
	/*
	 * LOGIN
	 * Il login è così strutturato:
	 * appena viene richiamata, effettua la connessione al server, dopodichè spedirà
	 * il pacchetto al server contente username e password e controllerà se l'utente
	 * ha inserito credenziali corrette, in caso affermativo lo connette.
	 */
	public String login(String username, String password);
	
	
	/*
	 * REGISTRAZIONE
	 * appena richiamata, effettua la registrazione di un nuovo utente con username
	 * e password passate; l'operazione dovrebbe esser permessa solo se l'utente non ha
	 * effettuato il login. In caso di server non disponibile la registrazione non verrà effettuata.
	 */
	public String registrazione(String username, String password) throws RemoteException, NotBoundException;
	
	
	/*
	 * LOGOUT
	 * effettua il logout dell'utente, solo se l'utente username era effettivamente loggato.
	 */
	public String logout(String username);
	
	
	/*
	 * AGGIUNTA_AMICO
	 * aggiunge un utente alla lista degli amici di username, solo se userAmico esiste e non 
	 * è già amico di username.
	 */
	public String aggiungiAmico(String username, String userAmico);
	
	
	/*
	 * LISTA_AMICI
	 * fornisce la lista degli amici di username, restituendo un array di String,
	 * di cui ogni elemento è un amico.
	 */
	public String listaAmici(String username);
	
	
	/*
	 * SFIDA
	 * effettua una sfida tra i due utenti, username e userAmico.
	 * La sfida verrà gestita con protocollo UDP.
	 */
	public String sfida(String username, String userAmico);
	
	
	/*
	 * MOSTRA_PUNTEGGIO
	 * mostra il punteggio totale dell'utente username
	 */
	public String mostraPunteggio(String username);
	
	
	/*
	 * MOSTRA_CLASSIFICA
	 * mostra la classifica tra gli utenti username e la sua lista di amici. 
	 */
	public String mostraClassifica(String username);
	
	
	
	public String accettaSfida(String response, String username, String userAmico); 
}
