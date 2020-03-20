package mbiscosi.wq.server;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;



/*
 * Questa classe crea il file .json
 */
public class JsonCreator {
	private File file;
	private File dictionary;
	private ServerService server;
	private ReentrantLock lock;
	
	//Crea un nuovo file .json
	public JsonCreator(ServerService server) {
		file = new File("Utenti.json");
		dictionary = new File("Parole.json");
		this.server = server;
		lock = new ReentrantLock();
		
		try {
			if(file.createNewFile())
				createJson();
			
			else
				leggiJSON();
		
		} catch (IOException e) {
			System.err.println("Errore nella creazione del file JSON...");
		}
	}
	
	
	
	
	public void createJson() {
		JSONObject fileJson = new JSONObject();
		JSONArray utenti = new JSONArray();
		
		fileJson.put("Utenti", utenti);
		
		//A questo punto scrivo l'oggetto di tipo JSONObject sul
		//file json e termino la creazione del file
		try {
			//Apro un fileChannel in scrittura e alloco un byte buffer della dimensione del file
			FileChannel outChannel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE);
			String text = fileJson.toJSONString();
			ByteBuffer buffer = ByteBuffer.allocate(text.length());
			
			buffer.put(text.getBytes());
			buffer.clear();
			
			//Finchè non sono arrivato in fondo al buffer, scrivo sul file
			while(buffer.hasRemaining())
				outChannel.write(buffer);
			
			//Resetto la position
			buffer.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	public void leggiJSON() {
		//In questo metodo leggo il JSON e per ogni oggetto di tipo JSONObject contenuto
		//all'interno dell'array JSONArray, lo mando al ThreadPoolExecutor
		
		//Faccio il parsing del file
		JSONParser jsonParser = new JSONParser();
		JSONParser dictionaryParser = new JSONParser();
		
		//Apro il file, in mod lettura, e con la NIO lo leggo
		try (FileChannel reader = FileChannel.open(file.toPath(), StandardOpenOption.READ);
				FileChannel reader2 = FileChannel.open(dictionary.toPath(), StandardOpenOption.READ)) {
			
			int fileSize = (int) file.length();
			int dictionarySize = (int) dictionary.length();
			
			ByteBuffer dicBuffer = ByteBuffer.allocate(dictionarySize);
			ByteBuffer buffer = ByteBuffer.allocate(fileSize);
			
			int bytesDic = 0;
			int bytesDim = 0;
			
			//Controllo che abbia letto tutto il file
			while(bytesDim != fileSize) {
				bytesDim += reader.read(buffer);
			}
			
			//Controllo che abbia letto tutto il file
			while(bytesDic != dictionarySize) {
				bytesDic += reader2.read(dicBuffer);
			}
			
			//Una volta letto il file, lo faccio diventare una Stringa
			String text = new String(buffer.array());
			String textDic = new String(dicBuffer.array());
			
			
			//Faccio il parsing del JSON file
			Object obj = jsonParser.parse(text);
			Object obj2 = dictionaryParser.parse(textDic);
			
			JSONObject tmp = (JSONObject) obj;
			JSONObject tmp2 = (JSONObject) obj2;
			
			JSONArray utenti = (JSONArray) tmp.get("Utenti");
			JSONArray parole = (JSONArray) tmp2.get("Parole");
			
			Iterator<JSONObject> externIt = utenti.iterator();
			Iterator<String> externIt2 = parole.iterator();
			
			//Ogni ogget del JSON verra aggiunto alle due strutture del server
			//Questo iterator scorre gli Utenti, mentre quello interno alla procedura
			//LettoreJsonObj scorre la lista di amici
			while(externIt.hasNext()) {
				LettoreJSONObj(externIt.next());
			}
			
			while(externIt2.hasNext()) {
				String parola = externIt2.next();
				server.getParole().add(parola);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	public synchronized void scriviJSON(String username, String password, String userAmico, String punteggio) {
		//In questo metodo leggo il JSON e per ogni oggetto di tipo JSONObject contenuto
		//all'interno dell'array JSONArray, lo mando al ThreadPoolExecutor
		
		//Faccio il parsing del file
		JSONParser jsonParser = new JSONParser();
		String text = null;
		
		//Apro il file, in mod lettura, e con la NIO lo leggo
		try (FileChannel reader = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
			
			int fileSize = (int) file.length();
			
			ByteBuffer buffer = ByteBuffer.allocate(fileSize);
			int bytesDim = 0;
			
			//Controllo che abbia letto tutto il file
			while(bytesDim != fileSize) {
				bytesDim += reader.read(buffer);
			}
			
			//Una volta letto il file, lo faccio diventare una Stringa
			text = new String(buffer.array());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		
		
		if(text != null) {
			//Faccio il parsing del JSON file			
			try (FileChannel writer = FileChannel.open(file.toPath(), StandardOpenOption.WRITE)) {
				Object obj = jsonParser.parse(text);
				
				JSONObject tmp = (JSONObject) obj;
				
				JSONArray utenti = (JSONArray) tmp.get("Utenti");
				
				if(password != null) {
					JSONObject tmp2 = new JSONObject();
					tmp2.put("Username", username);
					tmp2.put("Password", password);
					tmp2.put("Punteggio", 0);
					tmp2.put("Amici", new JSONArray());
					
					utenti.add(tmp2);
				}
				
				else if(userAmico != null){
					aggiungiAmico(username, utenti, userAmico);
				}
				
				else if(punteggio != null) {
					modificaPunteggio(username, utenti, Integer.parseInt(punteggio));
				}
				
				String text2 = tmp.toJSONString();
				ByteBuffer buffer = ByteBuffer.allocate(text2.length());
					
				buffer.put(text2.getBytes());
				buffer.clear();
					
				//Finchè non sono arrivato in fondo al buffer, scrivo sul file
				while(buffer.hasRemaining())
					writer.write(buffer);
					
				//Resetto la position
				buffer.clear();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e2) {
				e2.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	
	/*
	 * Procedura usata per scorrere la lista di amici di un Utente
	 * Che si trova all'interno del file JSON
	 */
	public void LettoreJSONObj(JSONObject utente) {
		//Aggiungo alle 2 strutture l'utente
		String username = (String) utente.get("Username");
		String password = (String) utente.get("Password");
		long punteggio = ((long) utente.get("Punteggio"));
		server.getConnessioni().put(username, new UserInfo(password, (int) punteggio));
		server.getUtenti().put(username, new ArrayList<String>());
		
		//Scorro la lista di amici
		JSONArray tmp = (JSONArray) utente.get("Amici");
		
		//Con un iterator scorro nel JSONArray e salvo la lista di amici
		Iterator<JSONObject> internIt = tmp.iterator();
		while(internIt.hasNext()) {
			String userAmico = (String) internIt.next().get("NomeAmico");
			//Aggiungo l'amico alla ArrayList
			server.getUtenti().get(username).add(userAmico);
		}
	}
	
	
	
	
	
	public void aggiungiAmico(String username, JSONArray utenti, String userAmico) {
		
		int counter = 0;
		
		Iterator<JSONObject> externIt = utenti.iterator();
		
		while(externIt.hasNext()) {
			JSONObject internIt = externIt.next();
			
			if(((String) internIt.get("Username")).equals(username)) {
				counter++;
			
				JSONObject tmp3 = new JSONObject();
				
				tmp3.put("NomeAmico", userAmico);
				
				((JSONArray) internIt.get("Amici")).add(tmp3);
				
				if(counter == 2)
					return;
			}
			else if(((String) internIt.get("Username")).equals(userAmico)) {
				counter++;
			
				JSONObject tmp3 = new JSONObject();
				
				tmp3.put("NomeAmico", username);
				
				((JSONArray) internIt.get("Amici")).add(tmp3);
				
				if(counter == 2)
					return;
			}
		}
	}
	
	
	
	public void modificaPunteggio(String username, JSONArray utenti, int punteggio) {
		
		int counter = 0;
		
		Iterator<JSONObject> externIt = utenti.iterator();
		
		while(externIt.hasNext()) {
			JSONObject internIt = externIt.next();
			
			if(((String) internIt.get("Username")).equals(username)) {
				internIt.replace("Punteggio", punteggio);
			}
		}
	}
	
	
	
	
	public String scriviJSONAmici(ArrayList<?> amici, int classifica) {
		
		String response = null;
		
		JSONObject tmp = new JSONObject();
		JSONArray tmp2 = new JSONArray();		
		
		if(classifica == 0) {
			ArrayList<String> tempAmici = (ArrayList<String>) amici;
			for(String nomeAmico : tempAmici) {
				
				JSONObject tmp3 = new JSONObject();
				
				tmp3.put("UserAmico", nomeAmico);
				
				tmp2.add(tmp3);			
			}
		}
		
		else {
			ArrayList<UtilityUserInfo> tempAmici = (ArrayList<UtilityUserInfo>) amici;
			
			Collections.sort(tempAmici, new SortAmici());
			
			for(UtilityUserInfo nomeAmico : tempAmici) {
				
				JSONObject tmp3 = new JSONObject();
				
				tmp3.put("UserAmico", nomeAmico.getUsername());
				tmp3.put("Punteggio", nomeAmico.getPunteggio());
				
				tmp2.add(tmp3);			
			}
		}
			
		tmp.put("ListaAmici", tmp2);

		response = tmp.toJSONString();
		
		System.out.println(response);
		
		return response;
	}
	
	
	private class SortAmici implements Comparator<UtilityUserInfo> {

		@Override
		public int compare(UtilityUserInfo a, UtilityUserInfo b) {
			if(a.getPunteggio() == b.getPunteggio())
				return a.getUsername().compareTo(b.getUsername());
			else
				return b.getPunteggio() - a.getPunteggio();
		}
		
	}
	
	
	
	public File getFile() {
		return file;
	}
	
	
	public ReentrantLock getLock() {
		return lock;
	}	
}
