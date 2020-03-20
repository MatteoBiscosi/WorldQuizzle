package mbiscosi.wq.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;




public class ChallengeUtilities {
	
	private int userSfidante;
	private int userSfidato;
	
	private int serverRaggiungibile = 1;
	
	private ReentrantLock lockParoleSfida;
	private ArrayList<String> paroleSfida;
	private ArrayList<ArrayList<String>> traduzione;
	private int puntSfidante;
	private int puntSfidato;
	private int numParole;
	private int indexSfidante;
	private int indexSfidato;
	private ServerService server;
	
	
	public ChallengeUtilities(int numParole, ServerService server) {
		this.server = server;
		this.userSfidante = 0;
		this.userSfidato = 0;
		this.puntSfidante = 0;
		this.puntSfidato = 0;
		this.numParole = numParole;
		lockParoleSfida = new ReentrantLock();
		
		this.paroleSfida = new ArrayList<String>(numParole);
		
		
		Random rand = new Random();
		
		for(int i = 0; i < numParole; i++) {
			String parola = server.getParole().get(rand.nextInt(server.getParole().size()));
			
			System.out.println(parola);
			
			paroleSfida.add(parola);
		}
	}
	
	
	
	
	//Metodo per tradurre le parole
	public void translateWords() throws IOException{
		
		this.traduzione = new ArrayList<ArrayList<String>>(this.numParole);
		
		int i = 0;
		
        for (String word : paroleSfida) {
            URL url1 = new URL("https://api.mymemory.translated.net/get?q=" + word + "&langpair=it|en");

            try (BufferedReader in = new BufferedReader(new InputStreamReader(url1.openStream()))) {
                StringBuilder inputLine = new StringBuilder();
                String reader;

                while ((reader = in.readLine()) != null) {
                    inputLine.append(reader);
                }

                JSONObject jsonObject;
                JSONParser parser = new JSONParser();

                try {
                    jsonObject = (JSONObject) parser.parse(inputLine.toString());

                    JSONArray array = (JSONArray) jsonObject.get("matches");
                    
                    ArrayList<String> tmpArray = new ArrayList<String>(array.size());

                    for (Object o : array) {
                        JSONObject obj = (JSONObject) o;
                        String stampa1 = (String) obj.get("translation");
                        System.out.println(stampa1);
                        tmpArray.add(stampa1);
                    }
                    
                    this.traduzione.add(tmpArray);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

            } catch (MalformedURLException mue) {
                mue.printStackTrace(System.err);
            }
            
            i++;
        }
    }
	
	
	
	public String checkWords(int index, String traduzione, int tipo) {
		int checkTraduzione = 0;
		
		for(String word : this.traduzione.get(index)) {
			if(traduzione.equalsIgnoreCase(word)) {
				checkTraduzione = 1;	
				break;
			}
		}
		
		

		switch(tipo) {
			case 0:
				if(checkTraduzione == 1)
					this.puntSfidante += 2;
				else
					this.puntSfidante -= 1;
				
				this.indexSfidante = index;
				break;
				
			case 1:
				if(checkTraduzione == 1)
					this.puntSfidato += 2;
				else
					this.puntSfidato -= 1;
				
				this.indexSfidato = index;
				break;
		}
		
		return "1";
	}
	


	public int getUserSfidante() {
		return userSfidante;
	}


	public void setUserSfidante(int userSfidante) {
		this.userSfidante = userSfidante;
	}


	public int getUserSfidato() {
		return userSfidato;
	}


	public void setUserSfidato(int userSfidato) {
		this.userSfidato = userSfidato;
	}


	public ArrayList<String> getParoleSfida() {
		return paroleSfida;
	}


	public void setParoleSfida(ArrayList<String> paroleSfida) {
		this.paroleSfida = paroleSfida;
	}


	public ArrayList<ArrayList<String>> getTraduzione() {
		return traduzione;
	}
	

	public int getPuntSfidante() {
		return puntSfidante;
	}


	public void setPuntSfidante(int puntSfidante) {
		this.puntSfidante = puntSfidante;
	}


	public int getPuntSfidato() {
		return puntSfidato;
	}


	public void setPuntSfidato(int puntSfidato) {
		this.puntSfidato = puntSfidato;
	}


	public int getNumParole() {
		return numParole;
	}


	public void setNumParole(int numParole) {
		this.numParole = numParole;
	}

	
	public ReentrantLock getLockParoleSfida() {
		return lockParoleSfida;
	}

	
	public void setLockParoleSfida(ReentrantLock lockParoleSfida) {
		this.lockParoleSfida = lockParoleSfida;
	}


	public int getIndexSfidante() {
		return indexSfidante;
	}


	public void setIndexSfidante(int indexSfidante) {
		this.indexSfidante = indexSfidante;
	}


	public int getIndexSfidato() {
		return indexSfidato;
	}


	public void setIndexSfidato(int indexSfidato) {
		this.indexSfidato = indexSfidato;
	}
	
	
	public int getServerRaggiungibile() {
		return serverRaggiungibile;
	}


	public void setServerRaggiungibile(int serverRaggiungibile) {
		this.serverRaggiungibile = serverRaggiungibile;
	}
}
