package server;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import model.Board;
import model.Granary;
import model.Hole;
import model.Mancala;
import model.Player;
import model.SimplePlayer;

public class Main {
	 public static void main(String[] zero){
		 ServerSocket listener;
		 
		 ArrayList<Hole> holes = new ArrayList<Hole>();
		 for(int i =0;i<12;i++) {
			 Hole newH = new Hole(4);
			 holes.add(newH);
		 }
		 Board board = new Board(holes);
		 try {
			 listener = new ServerSocket(59001);
			 ExecutorService pool = Executors.newFixedThreadPool(500);
			 while(true) {
				 if (!board.isFull()) {
					 System.out.println("waiting for connection...");
					 pool.execute(new SimplePlayer(listener.accept(), board));
				 }
			 }

		 } catch(IOException e) {
			 
		 };
	 }
}
