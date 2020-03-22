package mbiscosi.wq.server;



import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;


public class MainClassServer {
	/*
	 * Classe main del server, questa classe avvia tutti i componenti del server
	 */

	public static void main(String[] args) {
		/*
		 * Prende in input la porta su cui il server si mettera' in ascolto; se non viene inserito nessun dato,
		 * viene utilizzata la porta di default 13200
		 */
		ServerService server;
		Registry reg;
		ConnectionInterface stub;
		
		//Controllo i parametri in Input
		
		if(args.length != 1)			
			//Lettura del json file e instaurazione delle varie strutture
			//In caso di inserimento della porta in input, gliela passo, se c'è qualche problema
			//o se non è stata inserita la porta gli passo la standard 13200
			server = new ServerService(-1);
			
		
		else {
			try {
				server = new ServerService(Integer.parseInt(args[0]));
			} catch (RuntimeException ex) {
				ex.printStackTrace();
				server = new ServerService(-1);
			}
		}

		
		
		
		//Creazione della stub usata per la registrazione
		try {	
			
			stub = new Connection(server);
			
			
			reg = LocateRegistry.createRegistry(1099);
			
			reg.rebind("registrazione", stub);
			
			
			
			//Creo il thread del server e lo mando in esecuzione
			Thread thread = new Thread(server);
			thread.start();
			
			
			//Ora mi metto in attesa e se inserisco la stringa "termina", 
			//faccio terminare il server
			boolean tmp = false;
			Scanner sc = new Scanner(System.in);
			
			
			while(!tmp) {
				if(sc.next().equalsIgnoreCase("termina")) {
					server.shutdown();
					tmp = true;
				}
			}
			
			//
			if(reg != null) {
				UnicastRemoteObject.unexportObject(reg, true);
				System.out.println("Registro liberato");
			}
			
			try {
				thread.interrupt();
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			sc.close();
			System.out.println("Server terminato...");		
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		System.exit(0);
	}
}
