package model;

import java.net.Socket;

public class Player implements Runnable{
	private String ip;
	private String port;
	private Socket socket;
	private Granary granary;
	private boolean isPlaying;
	private Action lastAction;
	
	public Player(String ip, String port, Socket socket, Granary granary) {
		this.ip = ip;
		this.port = port;
		this.socket = socket;
		this.granary = granary;
		this.isPlaying = false;
	}
	
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	
	public Socket getSocket() {
		return socket;
	}
	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	
	public Granary getGranary() {
		return granary;
	}
	public void setGranary(Granary granary) {
		this.granary = granary;
	}
	
	public boolean isPlaying() {
		return isPlaying;
	}
	public void setPlaying(boolean isPlaying) {
		this.isPlaying = isPlaying;
	}
	
	public Action getLastAction() {
		return lastAction;
	}
	public void setLastAction(Action lastAction) {
		this.lastAction = lastAction;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
}
