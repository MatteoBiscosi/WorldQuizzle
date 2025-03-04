package mbiscosi.wq.server;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.locks.ReentrantLock;



public class UserInfo {
	/*
	 * Classe utilizzata per contenere tutte le informazioni relative ai diversi clients.
	 * Queste info verranno caricate all'avvio del server dalla classe JsonCreator.
	 * La classe ha solamente metodi getters and setters per gestire le varie info.
	 */
	
	private String password;
	private int connesso;
	private SelectableChannel client;
	private int punteggio;
	private int attualmenteInSfida;
	private ReentrantLock sfida;
	private int udpPort;
	private int selectorNum;
	private SelectionKey key;
	
	private String keySfida;
	private int tipo;
	

	public UserInfo(String password, int punteggio) {
		this.password = password;
		this.connesso = 0;
		this.punteggio = punteggio;
		this.attualmenteInSfida = 0;
		this.sfida = new ReentrantLock();
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
		this.punteggio = punteggio;
	}
	
	public int getAttualmenteInSfida() {
		return attualmenteInSfida;
	}
	
	public void setAttualmenteInSfida(int attualmenteInSfida) {
		this.attualmenteInSfida = attualmenteInSfida;
	}
	
	public synchronized ReentrantLock getLock() {
		return sfida;
	}
	
	public int getUdpPort() {
		return udpPort;
	}

	public void setUdpPort(int udpPort) {
		this.udpPort = udpPort;
	}
	
	public int getSelectorNum() {
		return selectorNum;
	}

	public void setSelectorNum(int selectorNum) {
		this.selectorNum = selectorNum;
	}

	public SelectionKey getKey() {
		return key;
	}

	public void setKey(SelectionKey key) {
		this.key = key;
	}
	
	public String getKeySfida() {
		return keySfida;
	}

	public void setKeySfida(String keySfida) {
		this.keySfida = keySfida;
	}

	public int getTipo() {
		return tipo;
	}

	public void setTipo(int tipo) {
		this.tipo = tipo;
	}
}