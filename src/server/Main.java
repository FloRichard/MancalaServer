package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import model.Board;
import model.Hole;
import model.SimplePlayer;

public class Main {
	 public static void main(String[] zero){
		 ServerSocket listener;
		 
		 ArrayList<Hole> holes = new ArrayList<Hole>();
		 for(int i =0;i<12;i++) {
			 Hole newH = new Hole(4);
			 holes.add(newH);
		 }
//		 holes.get(0).setSeeds(0);
//		 holes.get(1).setSeeds(0);
//		 holes.get(2).setSeeds(0);
//		 holes.get(3).setSeeds(0);
//		 holes.get(4).setSeeds(2);
//		 holes.get(5).setSeeds(2);
//		 holes.get(6).setSeeds(1);
//		 holes.get(7).setSeeds(1);
//		 holes.get(8).setSeeds(4);
//		 holes.get(9).setSeeds(0);
//		 holes.get(10).setSeeds(0);
//		 holes.get(11).setSeeds(0);
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
