package mbiscosi.wq.server;



import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;


public class MainClassServer {

	public static void main(String[] args) {
		ServerService server;
		Registry reg;
		ConnectionInterface stub;
		long timer = System.currentTimeMillis();
		
		//System.setProperty("java.rmi.server.hostname","192.168.56.1");
		//Controllo i parametri in Input
		if(args.length != 1)			
			//Lettura del json file e instaurazione delle varie strutture
			server = new ServerService(-1);
		
		else {
			try {
				server = new ServerService(Integer.parseInt(args[0]));
			} catch (RuntimeException ex) {
				ex.printStackTrace();
				server = new ServerService(-1);
				
			}
		}
		System.out.println("Secondi: " + (System.currentTimeMillis() - timer));

		
		
		
		//Creazione della stub usata per la registrazione
		try {	
			
			stub = new Connection(server);
			
			
			LocateRegistry.createRegistry(1099);
			reg = LocateRegistry.getRegistry();
			
			reg.rebind("registrazione", stub);
			
			
			
			//Creo il thread del server e lo mando in esecuzione
			Thread thread = new Thread(server);
			thread.start();
			System.out.println("Server in esecuzione..., secondi: " + (System.currentTimeMillis() - timer));
			
			
			//Ora mi metto in attesa e se inserisco la stringa "termina", 
			//faccio terminare il server
			boolean tmp = false;
			Scanner sc = new Scanner(System.in);
			
			
			while(!tmp) {
				if(sc.next().equalsIgnoreCase("termina")) {
					try {
						reg.unbind("registrazione");
					} catch (NotBoundException e) {
						e.printStackTrace();
					}
					server.shutdown();
					tmp = true;
				}
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
	}
}
