package mbiscosi.wq.server;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;



public class UserInfo {
	
	private String password;
	private int connesso;
	private SelectableChannel client;
	private int punteggio;
	
	
	public UserInfo(String password, int punteggio) {
		this.password = password;
		this.connesso = 0;
		this.punteggio = punteggio;
	}
	
	public String getPassword() {
		return password;
	}
	
	public int getConnesso() {
		return connesso;
	}
	
	public void setConnesso(int connesso) {
		this.connesso = connesso;
	}
	
	public void setSocketChannel(SelectableChannel channel) {
		this.client = channel;
	}
	
	public SelectableChannel getSocketChannel() {
		return this.client;
	}
	
	public int getPunteggio() {
		return punteggio;
	}
	
	public void setPunteggio(int punteggio) {
		this.punteggio += punteggio;
	}
}