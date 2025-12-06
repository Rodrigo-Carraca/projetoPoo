package pt.iscte.poo.game;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import java.util.List;
import java.awt.event.KeyEvent;

import objects.SmallFish;
import objects.BigFish;
import objects.GameCharacter;
import objects.GameObject;
import objects.Krab;

import pt.iscte.poo.gui.ImageGUI;
import pt.iscte.poo.observer.Observed;
import pt.iscte.poo.observer.Observer;
import pt.iscte.poo.utils.Direction;
import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;

import highscores.HighscoreManager;
import highscores.Highscore;

public class GameEngine implements Observer {

	private static GameEngine INSTANCE = null;

	public static GameEngine getInstance() {
		if (INSTANCE == null)
			INSTANCE = new GameEngine();
		return INSTANCE;
	}

	private final Map<String, Room> rooms;
	private Room currentRoom;
	private int lastTickProcessed = 0;
	private GameCharacter controlled;

	// Contagem
	private HighscoreManager highscoreManager = new HighscoreManager();
	private long gameStartTime = -1L; // tempo total desde início do jogo
	private int totalMoves = 0; // movimentos acumulados da run inteira
	private boolean gameEnded = false; // evita pedido infinito de nome

	private GameEngine() {
		rooms = new HashMap<>();
		loadGame();

		currentRoom = rooms.get("room0.txt");
		if (currentRoom == null && !rooms.isEmpty())
			currentRoom = rooms.values().iterator().next();

		// Associar room aos singletons
		try {
			if (SmallFish.getInstance() != null)
				SmallFish.getInstance().setRoom(currentRoom);
		} catch (Throwable ignored) {
		}
		try {
			if (BigFish.getInstance() != null)
				BigFish.getInstance().setRoom(currentRoom);
		} catch (Throwable ignored) {
		}

		pickInitialControlled();

		// Iniciar contagem global APENAS uma vez
		gameStartTime = System.currentTimeMillis();
		totalMoves = 0;
		gameEnded = false;

		updateStatusMessage();
		updateGUI();
	}

	private void loadGame() {
		File folder = new File("./rooms");
		File[] files = (folder.exists()) ? folder.listFiles() : null;
		if (files == null)
			return;
		for (File f : files) {
			if (!f.isFile() || !f.getName().endsWith(".txt"))
				continue;
			Room r = Room.readRoom(f, this);
			if (r != null)
				rooms.put(f.getName(), r);
		}
	}

	@Override
	public void update(Observed source) {
		// Se jogo terminou → só aceitar 'R', e mais nada
		if (gameEnded) {
			if (ImageGUI.getInstance().wasKeyPressed()) {
				int k = ImageGUI.getInstance().keyPressed();
				if (k == KeyEvent.VK_R) {
					restartLevel();
					gameEnded = false;
				}
			}
			return;
		}

		//Utilizador
		if (ImageGUI.getInstance().wasKeyPressed()) {
			int k = ImageGUI.getInstance().keyPressed();

			if (k == KeyEvent.VK_SPACE) {
				toggleControlled();
				updateStatusMessage();

			} else if (k == KeyEvent.VK_R) {
				restartLevel(); // NÃO reinicia contadores!

			} else {
				try {
					Vector2D delta = Direction.directionFor(k).asVector();
					if (controlled != null && !controlled.isOut() && controlled.getRoom() != null
							&& controlled.isAlive()) {

						Point2D oldPos = clonePosition(controlled.getPosition());
						controlled.move(delta);
						Point2D newPos = clonePosition(controlled.getPosition());

						if (!positionsEqual(oldPos, newPos)) {
							totalMoves++; // movimento válido
						}

						notifyCrabsOnPlayerMove();
					}

					checkExit(BigFish.getInstance());
					checkExit(SmallFish.getInstance());
					updateStatusMessage();

				} catch (Exception ignored) {
				}
			}
		}

		//Gravidade/Ticks
		int t = ImageGUI.getInstance().getTicks();
		while (lastTickProcessed < t)
			processTick();

		notifyCrabsOnPlayerMove();

		//Atualiza GUI
		try {
			ImageGUI.getInstance().update();
		} catch (Exception ignored) {
		}

		checkExit(BigFish.getInstance());
		checkExit(SmallFish.getInstance());

		ensureControlledStillValid();
		updateStatusMessage();

		//GameOver (Morte)
		SmallFish sf = SmallFish.getInstance();
		BigFish bf = BigFish.getInstance();
		boolean smallDead = sf != null && !sf.isAlive();
		boolean bigDead = bf != null && !bf.isAlive();

		if (smallDead || bigDead) {
			String who;
			if (smallDead && bigDead)
				who = "Both fish died!";
			else if (smallDead)
				who = "SmallFish morreu!";
			else
				who = "BigFish morreu!";

			final String msg = "GAME OVER!\n" + who;

			try {
				SwingUtilities.invokeAndWait(
						() -> JOptionPane.showMessageDialog(null, msg, "Message", JOptionPane.INFORMATION_MESSAGE));
			} catch (Exception e) {
				try {
					JOptionPane.showMessageDialog(null, msg, "Message", JOptionPane.INFORMATION_MESSAGE);
				} catch (Exception ignored) {
				}
			}

			restartLevel();
			return;
		}
		//Passagem de nível
		checkLevelCompletion();
	}

	private void processTick() {
		lastTickProcessed++;
		if (currentRoom != null)
			currentRoom.applyGravity();
	}

	public void updateGUI() {
		if (currentRoom != null) {
			try {
				ImageGUI.getInstance().clearImages();
				ImageGUI.getInstance().addImages(currentRoom.getObjects());
			} catch (Exception ignored) {
			}
		} else {
			try {
				ImageGUI.getInstance().clearImages();
			} catch (Exception ignored) {
			}
		}
	}

	private void toggleControlled() {
		GameCharacter big = BigFish.getInstance();
		GameCharacter small = SmallFish.getInstance();

		boolean bigA = big != null && big.isAlive() && !big.isOut();
		boolean smallA = small != null && small.isAlive() && !small.isOut();

		if (!bigA && !smallA) {
			controlled = null;
			return;
		}

		if (controlled == null) {
			controlled = bigA ? big : small;
			return;
		}

		if (!controlled.isAlive() || controlled.isOut()) {
			controlled = bigA ? big : smallA ? small : null;
			return;
		}

		if (bigA && smallA) {
			controlled = (controlled == big ? small : big);
			return;
		}

		if (bigA)
			controlled = big;
		else if (smallA)
			controlled = small;
	}

	private void ensureControlledStillValid() {
		if (controlled == null || !controlled.isAlive() || controlled.isOut()) {
			pickInitialControlled();
		}
	}

	private void pickInitialControlled() {
		GameCharacter big = BigFish.getInstance();
		GameCharacter small = SmallFish.getInstance();

		if (big != null && big.isAlive() && !big.isOut())
			controlled = big;
		else if (small != null && small.isAlive() && !small.isOut())
			controlled = small;
		else
			controlled = null;
	}

	private void updateStatusMessage() {
		try {
			String name = (controlled != null) ? controlled.getName() : "none";
			ImageGUI.getInstance().setStatusMessage("Controlling: " + name);
		} catch (Exception ignored) {
		}
	}

	private void checkExit(GameCharacter fish) {
		if (fish == null || fish.isOut())
			return;

		if (fish.getRoom() == null || fish.getPosition() == null) {
			fish.setOut(true);
			try {
				ImageGUI.getInstance().removeImage(fish);
			} catch (Exception ignored) {
			}
			return;
		}

		int x = fish.getPosition().getX();
		int y = fish.getPosition().getY();

		if (x < 0 || x >= 10 || y < 0 || y >= 10) {
			fish.setOut(true);
			try {
				ImageGUI.getInstance().removeImage(fish);
			} catch (Exception ignored) {
			}
		}
	}

	private void checkLevelCompletion() {
		BigFish big = BigFish.getInstance();
		SmallFish small = SmallFish.getInstance();

		if ((big == null || big.isOut()) && (small == null || small.isOut())) {
			loadNextLevel();
		}
	}

	private void loadNextLevel() {
		int currentLevel = 0;

		try {
			String digits = currentRoom.getName().replaceAll("\\D+", "");
			if (!digits.isEmpty())
				currentLevel = Integer.parseInt(digits);
		} catch (Exception ignored) {
		}

		int nextLevel = currentLevel + 1;
		String nextName = "room" + nextLevel + ".txt";

		BigFish.resetInstance();
		SmallFish.resetInstance();

		File f = new File("./rooms/" + nextName);
		Room next = f.exists() ? Room.readRoom(f, this) : rooms.get(nextName);

		//Fim do Jogo
		if (next == null) {
			onGameEnd();
			return;
		}

		currentRoom = next;

		// reconstruir GUI e singletons
		try {
			ImageGUI.getInstance().clearImages();
			ImageGUI.getInstance().addImages(currentRoom.getObjects());
			ImageGUI.getInstance().update();
		} catch (Exception ignored) {
		}

		try {
			if (currentRoom.getBigFish() != null)
				BigFish.getInstance(currentRoom.getBigFish().getPosition(), currentRoom);
		} catch (Throwable ignored) {
		}
		try {
			if (currentRoom.getSmallFish() != null)
				SmallFish.getInstance(currentRoom.getSmallFish().getPosition(), currentRoom);
		} catch (Throwable ignored) {
		}

		ensureControlledStillValid();
		updateStatusMessage();
	}

	// Reiniciar o nível NÃO reinicia tempo nem movimentos!
	private void restartLevel() {
		String roomName = currentRoom.getName();

		BigFish.resetInstance();
		SmallFish.resetInstance();

		File f = new File("./rooms/" + roomName);
		Room r = (f.exists() ? Room.readRoom(f, this) : null);

		if (r == null)
			return;

		rooms.put(roomName, r);
		currentRoom = r;

		try {
			ImageGUI.getInstance().clearImages();
			ImageGUI.getInstance().addImages(currentRoom.getObjects());
			ImageGUI.getInstance().update();
		} catch (Exception ignored) {
		}

		ensureControlledStillValid();
		updateStatusMessage();
	}

	public Room getCurrentRoom() {
		return currentRoom;
	}

	public void setCurrentRoom(Room r) {
		this.currentRoom = r;
		ensureControlledStillValid();
		updateGUI();
	}

	private void notifyCrabsOnPlayerMove() {
		if (currentRoom == null)
			return;
		List<GameObject> gos = currentRoom.getGameObjects();
		for (GameObject go : gos) {
			if (go instanceof Krab) {
				try {
					((Krab) go).move(null);
				} catch (Throwable ignored) {
				}
			}
		}
		try {
			updateGUI();
		} catch (Exception ignored) {
		}
	}

	//HighScore
	private void onGameEnd() {

		if (gameEnded)
			return; // garantir que só corre 1 vez

		long elapsed = System.currentTimeMillis() - gameStartTime;

		String playerName = null;
		try {
			final String[] holder = new String[1];
			SwingUtilities.invokeAndWait(() -> holder[0] = JOptionPane.showInputDialog(null, "Nome:",
					"Guardar Highscore", JOptionPane.PLAIN_MESSAGE));
			playerName = holder[0];
		} catch (Exception ignored) {
		}

		if (playerName == null || playerName.trim().isEmpty())
			playerName = "Jogador";

		highscoreManager.addScore(playerName, elapsed, totalMoves);

		showHighscoresPopup();

		gameEnded = true;
	}

	private void showHighscoresPopup() {
		StringBuilder sb = new StringBuilder("HIGHSCORES:\n\n");
		int i = 1;

		for (Highscore h : highscoreManager.getScores()) {
			sb.append(i++).append(". ").append(h.getName()).append(" - ").append(h.getTimeMillis() / 1000)
					.append("s - ").append(h.getMoves()).append(" moves\n");
		}

		try {
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, sb.toString(), "Highscores",
					JOptionPane.INFORMATION_MESSAGE));
		} catch (Exception ignored) {
			System.out.println(sb);
		}
	}
	
	//Funções para ajudar
	private boolean positionsEqual(Point2D a, Point2D b) {
		if (a == null && b == null)
			return true;
		if (a == null || b == null)
			return false;
		return a.getX() == b.getX() && a.getY() == b.getY();
	}

	private Point2D clonePosition(Point2D p) {
		if (p == null)
			return null;
		return new Point2D(p.getX(), p.getY());
	}
}
