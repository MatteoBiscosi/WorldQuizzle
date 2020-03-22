package mbiscosi.wq.server;

public class UtilityUserInfo {
	/*
	 * Classe che contiene delle info banali, utilizzate per comodita' dai metodi per gestire le classifiche 
	 * Utilizzate dal metodo mostra_classifica della classe eventHandler
	 */
	
	private String username;
	private int punteggio;
	
	public UtilityUserInfo(String username, int punteggio) {
		this.username = username;
		this.punteggio = punteggio;
	}
	
	
	public String getUsername() {
		return username;
	}
	
	public int getPunteggio() {
		return punteggio;
	}
}
