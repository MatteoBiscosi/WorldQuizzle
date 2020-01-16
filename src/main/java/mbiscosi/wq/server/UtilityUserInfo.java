package mbiscosi.wq.server;

public class UtilityUserInfo {
	
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
