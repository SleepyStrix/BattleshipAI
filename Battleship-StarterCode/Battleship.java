/**
 * @ AUTHOR NAME HERE
 * @ Starter Code By Guocheng
 *
 * 2016-01-30
 * For: Purdue Hackers - Battleship
 * Battleship Client
 */

import java.io.*;
import java.net.Socket;
import java.net.InetAddress;
import java.lang.Thread;

public class Battleship {
	public static String API_KEY = "249124271"; ///////// PUT YOUR API KEY HERE /////////
	public static String GAME_SERVER = "battleshipgs.purduehackers.com";

	//////////////////////////////////////  PUT YOUR CODE HERE //////////////////////////////////////
	enum Status {
		HIT, SUNK, MISS, TAKEN, NONE
	}

	class Tile {
		int probability;
		Status status;

		public Tile () {
			this.probability = 0;
			this.status = Status.NONE;
		}
	}

	char[] letters;
	int[][] grid;
	Tile[][] tileGrid;
	Tile[][] ourShips;
	int WIDTH = 7;
	boolean hitLastTurn = false;
	boolean outOfDiagonals = false;

	boolean isValidPosition(int x1, int y1, int x2, int y2) {
		if (x1 > WIDTH || x2 > WIDTH || y1 > WIDTH || y2 > WIDTH
			|| x1 < 0 || x2 < 0 || y1 < 0 || y2 < 0) {
			return false;
		}

		// Vertical
		if (x1 == x2) {
			if (y1 > y2) {
				int t = y2;
				y2 = y1;
				y1 = t;
			}

			for (int i = y1; i <= y2; i++) {
				if (this.ourShips[x1][i].status == Status.TAKEN) {
					return false;
				}
			}
		}
		// Horizontal
		else if (y1 == y2) {
			if (x1 > x2) {
				int t = x2;
				x2 = x1;
				x1 = t;
			}

			for (int i = x1; i <= x2; i++) {
				if (this.ourShips[i][y1].status == Status.TAKEN) {
					return false;
				}
			}
		}
		// Error
		else {
			return false;
		}

		return true;
	}

	int[] findPosition (int len) {
		while (true) {
			// Horizontal
			if (Math.random() > 0.5) {
				int x1 = (int)(Math.random() * (WIDTH - len));
				int y1 = (int)(Math.random() * (WIDTH));
				int x2 = x1 + len;
				int y2 = y1;

				if (this.isValidPosition(x1, y1, x2, y2)) {
					return new int[]{x1, y1, x2, y2};
				}
			}
			// Vertical
			else {
				int x1 = (int)(Math.random() * (WIDTH));
				int y1 = (int)(Math.random() * (WIDTH - len));
				int x2 = x1;
				int y2 = y1 + len;

				if (this.isValidPosition(x1, y1, x2, y2)) {
					return new int[]{x1, y1, x2, y2};
				}
			}
		}
	}

	void placeShips(String opponentID) {
		this.ourShips = new Tile[WIDTH][WIDTH];

		for (int i = 0; i < WIDTH; i++) {
			for (int j = 0; j < WIDTH; j++) {
				this.ourShips[i][j] = new Tile();
			}
		}

		// Fill Grid With -1s
		for(int i = 0; i < tileGrid.length; i++) {
			for(int j = 0; j < tileGrid[i].length; j++) {
				tileGrid[i][j] = new Tile(); tileGrid[i][j].probability = 0;
			}
		}


		// Place Ships
		int[] d = findPosition(2);
		placeDestroyer(this.letters[d[0]] + String.valueOf(d[1]), this.letters[d[2]] + String.valueOf(d[3]));

		d = findPosition(3);
		placeSubmarine(this.letters[d[0]] + String.valueOf(d[1]), this.letters[d[2]] + String.valueOf(d[3]));

		d = findPosition(3);
		placeCruiser(this.letters[d[0]] + String.valueOf(d[1]), this.letters[d[2]] + String.valueOf(d[3]));

		d = findPosition(4);
		placeBattleship(this.letters[d[0]] + String.valueOf(d[1]), this.letters[d[2]] + String.valueOf(d[3]));

		d = findPosition(5);
		placeCarrier(this.letters[d[0]] + String.valueOf(d[1]), this.letters[d[2]] + String.valueOf(d[3]));
	}

	void makeMove() {
		moveDecision();
		/*for(int i = 0; i < 8; i++) {
			if (this.step(i)) {
				return;
			}
		}

		for(int i = 7; i <= 0; i--) {
			if (this.step(i)) {
				return;
			}
		}*/
	}

	void moveDecision() {
		Tile t = highestProbability();
		if (t != null) {
			//fire at position of Tile t

		} else {
			if (outOfDiagonals) {
					randomSearch();
			}
			else {
				diagonalSearch();
			}
		}
	}

	Tile highestProbability() {
		Tile best = new Tile();
		best.probability = -1;
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				Tile t = tileGrid[i][j];
				if (t.status == Status.NONE) {
					if (t.probability > best.probability) {
						best = t;
					}
				}
			}
		}
		if (best.probability == -1) {
			return null;
		}
		else {
			return best;
		}
	}

	void diagonalSearch() {
		for(int i = 0; i < 8; i++) {
			if (this.step(i, i)) {
				return;
			}
		}

		for(int i = 0; i < 8; i++) {
			if (this.step(WIDTH - i, i)) {
				return;
			}
		}

		// TODO probability thing
		outOfDiagonals = true;
	}

	void randomSearch() {
		while (true) {
			int i = (int) (Math.random() * WIDTH);
			int j = (int) (Math.random() * WIDTH);

			if (this.tileGrid[i][j].status == Status.NONE) {
				String wasHitSunkOrMiss = placeMove(this.letters[i] + String.valueOf(j));

				if (wasHitSunkOrMiss.equals("Hits")) {
					this.tileGrid[i][j].status = Status.HIT;
				} else if (wasHitSunkOrMiss.equals("Sunk")) {
					this.tileGrid[i][j].status = Status.SUNK;
				} else {
					this.tileGrid[i][j].status = Status.MISS;
				}

				return;
			}
		}
	}

	boolean step(int i, int j) {
		if (this.tileGrid[i][j].status == Status.NONE) {
			String wasHitSunkOrMiss = placeMove(this.letters[i] + String.valueOf(j));

			if (wasHitSunkOrMiss.equals("Hits")) {
				this.tileGrid[i][j].status = Status.HIT;
			} else if (wasHitSunkOrMiss.equals("Sunk")) {
				this.tileGrid[i][j].status = Status.SUNK;
			} else {
				this.tileGrid[i][j].status = Status.MISS;
			}

			return true;
		}

		return false;
	}

	void fireAtPosition(int i, int j) {
		String target = "";
		String row = "";
		String col = "";
		switch (i) {
			case(0): {
				row = "A";
				break;
			}
			case(1): {
				row = "B";
				break;
			}
			case(2): {
				row = "C";
				break;
			}
			case(3): {
				row = "D";
				break;
			}
			case(4): {
				row = "E";
				break;
			}
			case(5): {
				row = "F";
				break;
			}
			case(6): {
				row = "G";
				break;
			}
			case(7): {
				row = "H";
				break;
			}
		}
		col = "" + j;
		target = row + col;
		placeMove(target);
	}

	////////////////////////////////////// ^^^^^ PUT YOUR CODE ABOVE HERE ^^^^^ //////////////////////////////////////

	Socket socket;
	String[] destroyer, submarine, cruiser, battleship, carrier;

	String dataPassthrough;
	String data;
	BufferedReader br;
	PrintWriter out;
	Boolean moveMade = false;

	public Battleship() {
		this.grid = new int[8][8];
		for(int i = 0; i < grid.length; i++) { for(int j = 0; j < grid[i].length; j++) grid[i][j] = -1; }
		this.letters = new char[] {'A','B','C','D','E','F','G','H'};

		destroyer = new String[] {"A0", "A0"};
		submarine = new String[] {"A0", "A0"};
		cruiser = new String[] {"A0", "A0"};
		battleship = new String[] {"A0", "A0"};
		carrier = new String[] {"A0", "A0"};
	}

	void connectToServer() {
		try {
			InetAddress addr = InetAddress.getByName(GAME_SERVER);
			socket = new Socket(addr, 23345);
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);

			out.print(API_KEY);
			out.flush();
			data = br.readLine();
		} catch (Exception e) {
			System.out.println("Error: when connecting to the server...");
			socket = null;
		}

		if (data == null || data.contains("False")) {
			socket = null;
			System.out.println("Invalid API_KEY");
			System.exit(1); // Close Client
		}
	}



	public void gameMain() {
		while(true) {
			try {
				if (this.dataPassthrough == null) {
					this.data = this.br.readLine();
				}
				else {
					this.data = this.dataPassthrough;
					this.dataPassthrough = null;
				}
			} catch (IOException ioe) {
				System.out.println("IOException: in gameMain");
				ioe.printStackTrace();
			}
			if (this.data == null) {
				try { this.socket.close(); }
				catch (IOException e) { System.out.println("Socket Close Error"); }
				return;
			}

			if (data.contains("Welcome")) {
				String[] welcomeMsg = this.data.split(":");
				placeShips(welcomeMsg[1]);
				if (data.contains("Destroyer")) { // Only Place Can Receive Double Message, Pass Through
					this.dataPassthrough = "Destroyer(2):";
				}
			} else if (data.contains("Destroyer")) {
				this.out.print(destroyer[0]);
				this.out.print(destroyer[1]);
				out.flush();
			} else if (data.contains("Submarine")) {
				this.out.print(submarine[0]);
				this.out.print(submarine[1]);
				out.flush();
			} else if (data.contains("Cruiser")) {
				this.out.print(cruiser[0]);
				this.out.print(cruiser[1]);
				out.flush();
			} else if (data.contains("Battleship")) {
				this.out.print(battleship[0]);
				this.out.print(battleship[1]);
				out.flush();
			} else if (data.contains("Carrier")) {
				this.out.print(carrier[0]);
				this.out.print(carrier[1]);
				out.flush();
			} else if (data.contains( "Enter")) {
				this.moveMade = false;
				this.makeMove();
			} else if (data.contains("Error" )) {
				System.out.println("Error: " + data);
				System.exit(1); // Exit sys when there is an error
			} else if (data.contains("Die" )) {
				System.out.println("Error: Your client was disconnected using the Game Viewer.");
				System.exit(1); // Close Client
			} else {
				System.out.println("Recieved Unknown Response:" + data);
				System.exit(1); // Exit sys when there is an unknown response
			}
		}
	}

	void placeDestroyer(String startPos, String endPos) {
		destroyer = new String[] {startPos.toUpperCase(), endPos.toUpperCase()};
	}

	void placeSubmarine(String startPos, String endPos) {
		submarine = new String[] {startPos.toUpperCase(), endPos.toUpperCase()};
	}

	void placeCruiser(String startPos, String endPos) {
		cruiser = new String[] {startPos.toUpperCase(), endPos.toUpperCase()};
	}

	void placeBattleship(String startPos, String endPos) {
		battleship = new String[] {startPos.toUpperCase(), endPos.toUpperCase()};
	}

	void placeCarrier(String startPos, String endPos) {
		carrier = new String[] {startPos.toUpperCase(), endPos.toUpperCase()};
	}

	String placeMove(String pos) {
		if(this.moveMade) { // Check if already made move this turn
			System.out.println("Error: Please Make Only 1 Move Per Turn.");
			System.exit(1); // Close Client
		}
		this.moveMade = true;

		this.out.print(pos);
		out.flush();
		try { data = this.br.readLine(); }
		catch(Exception e) { System.out.println("No response after from the server after place the move"); }

		if (data.contains("Hit")) return "Hit";
		else if (data.contains("Sunk")) return "Sunk";
		else if (data.contains("Miss")) return "Miss";
		else {
			this.dataPassthrough = data;
			return "Miss";
		}
	}

	public static void main(String[] args) {
		Battleship bs = new Battleship();
		while(true) {
			bs.connectToServer();
			if (bs.socket != null) bs.gameMain();
		}
	}
}
