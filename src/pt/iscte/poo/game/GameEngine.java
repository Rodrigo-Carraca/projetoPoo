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

	private GameEngine() {
		rooms = new HashMap<>();
		loadGame();

		currentRoom = rooms.get("room0.txt");
		if (currentRoom == null && !rooms.isEmpty())
			currentRoom = rooms.values().iterator().next();

		// associar room às instâncias singletons 
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
		// 1) Input do utilizador
		if (ImageGUI.getInstance().wasKeyPressed()) {
			int k = ImageGUI.getInstance().keyPressed();

			if (k == KeyEvent.VK_SPACE) { //Alternar
				toggleControlled();
				updateStatusMessage();
				
			} else if (k == KeyEvent.VK_R) {
				restartLevel();
				
			} else {	
				try {
					Vector2D delta = Direction.directionFor(k).asVector();
					if (controlled != null && !controlled.isOut() && controlled.getRoom() != null && controlled.isAlive()) {
						controlled.move(delta);
						
						notifyCrabsOnPlayerMove(); //Notificar Krabs qd um jogador se move
					}

					// Após mover via teclado, verificar possíveis saídas
					checkExit(BigFish.getInstance());
					checkExit(SmallFish.getInstance());
					
					// escolha do jogador
					updateStatusMessage();

				} catch (Exception ignored) {
					// tecla não mapeada
				}
			}
		}
		// 2) Ticks / Gravidade
		int t = ImageGUI.getInstance().getTicks();
		while (lastTickProcessed < t)
			processTick();

		notifyCrabsOnPlayerMove();  // Permite que crabs reajam a movimentos através do processo de ticks/gravidade

		// 3) Atualizar GUI
		try {
			ImageGUI.getInstance().update();
		} catch (Exception ignored) {
		}

		try {
			checkExit(BigFish.getInstance());
		} catch (Exception ignored) {
		}
		try {
			checkExit(SmallFish.getInstance());
		} catch (Exception ignored) {
		}
		ensureControlledStillValid(); // só altera se o controlado deixou de ser válido
		updateStatusMessage();

		// 5) Reinício se algum peixe morreu — mostrar diálogo antes de reiniciar
		SmallFish sf = SmallFish.getInstance();
		BigFish bf = BigFish.getInstance();
		boolean smallDead = (sf != null && !sf.isAlive());
		boolean bigDead = (bf != null && !bf.isAlive());

		if (smallDead || bigDead) {
			String who;
			if (smallDead && bigDead) who = "Both fish died!";
			else if (smallDead) who = "SmallFish morreu!";
			else who = "BigFish morreu!";

			final String msg = "GAME OVER!\n" + who;

			// mostrar diálogo modal no EDT (Event Dispatch Thread)
			try {
				SwingUtilities.invokeAndWait(() -> {
					JOptionPane.showMessageDialog(
						null,
						msg,
						"Message",
						JOptionPane.INFORMATION_MESSAGE
					);
				});
			} catch (Exception e) {
				// fallback simples caso invokeAndWait falhe por qualquer motivo
				try {
					JOptionPane.showMessageDialog(
						null,
						msg,
						"Message",
						JOptionPane.INFORMATION_MESSAGE
					);
				} catch (Exception ignored) {}
			}

			// agora reiniciar o nível
			restartLevel();
			return;
		}

		// 6) Verificar se ambos saíram -> avançar nível
		checkLevelCompletion();
	}

	private void processTick() {
		lastTickProcessed++;
		if (currentRoom != null)
			currentRoom.applyGravity();
	}

	//Reconstrói a GUI a partir dos imageTiles da currentRoom. 
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

	// Caso esteja disponível alterna o personagem controlado pelo jogador (SPACE)
	private void toggleControlled() {
		GameCharacter big = BigFish.getInstance();
		GameCharacter small = SmallFish.getInstance();

		boolean bigAvailable = big != null && big.isAlive() && !big.isOut() && big.getRoom() != null;
		boolean smallAvailable = small != null && small.isAlive() && !small.isOut() && small.getRoom() != null;

		// nenhum disponível
		if (!bigAvailable && !smallAvailable) {
			controlled = null;
			return;
		}

		// se controlado actual é null -> escolhe preferencialmente big
		if (controlled == null) {
			if (bigAvailable) {
				controlled = big;
				return;
			}
			if (smallAvailable) {
				controlled = small;
				return;
			}
		}

		// se controlado actual não é mais válido -> escolher outro disponível
		if (controlled != null && (!controlled.isAlive() || controlled.isOut() || controlled.getRoom() == null)) {
			if (bigAvailable) {
				controlled = big;
				return;
			}
			if (smallAvailable) {
				controlled = small;
				return;
			}
			controlled = null;
			return;
		}

		// se ambos disponíveis -> alterna
		if (bigAvailable && smallAvailable) {
			if (controlled == big)
				controlled = small;
			else
				controlled = big;
			return;
		}

		// caso apenas um disponível -> escolhe esse
		if (bigAvailable)
			controlled = big;
		else if (smallAvailable)
			controlled = small;
	}

	// Garante que a personagem controlada está valida
	private void ensureControlledStillValid() {
		if (controlled == null) {
			pickInitialControlled();
			return;
		}
		if (controlled.isAlive() && !controlled.isOut() && controlled.getRoom() != null) {
			return;
		}
		pickInitialControlled(); // senão escolhe outro disponível (prefere BigFish)
	}

	//Escolhe o personagem inicial (prefere BigFish)
	private void pickInitialControlled() {
		GameCharacter big = BigFish.getInstance();
		GameCharacter small = SmallFish.getInstance();

		boolean bigAvailable = big != null && big.isAlive() && !big.isOut() && big.getRoom() != null;
		boolean smallAvailable = small != null && small.isAlive() && !small.isOut() && small.getRoom() != null;

		if (bigAvailable) {
			controlled = big;
			return;
		}
		if (smallAvailable) {
			controlled = small;
			return;
		}
		controlled = null;
	}

	private void updateStatusMessage() {
		try {
			String name = (controlled != null) ? controlled.getName() : "none";
			ImageGUI.getInstance().setStatusMessage("Controlling: " + name);
		} catch (Exception ignored) {
		}
	}

	//Verifica se algum peixe saiu
	private void checkExit(GameCharacter fish) {
		if (fish == null)
			return;
		if (fish.isOut())
			return;

		// sem room => out
		if (fish.getRoom() == null) {
			fish.setOut(true);
			try {
				ImageGUI.getInstance().removeImage(fish);
			} catch (Exception ignored) {
			}
			return;
		}

		// posição nula => out
		if (fish.getPosition() == null) {
			fish.setOut(true);
			try {
				ImageGUI.getInstance().removeImage(fish);
			} catch (Exception ignored) {
			}
			try {
				fish.setPosition(new Point2D(-1, -1));
				fish.setRoom(null);
			} catch (Throwable ignored) {
			}
			return;
		}

		// limites fixos 10x10
		int x = fish.getPosition().getX();
		int y = fish.getPosition().getY();
		boolean inside10x10 = (x >= 0 && x < 10 && y >= 0 && y < 10);

		if (!inside10x10) {
			fish.setOut(true);
			try {
				ImageGUI.getInstance().removeImage(fish);
			} catch (Exception ignored) {
			}
			try {
				fish.setPosition(new Point2D(-1, -1));
				fish.setRoom(null);
			} catch (Throwable ignored) {
			}
		}
	}

	private void checkLevelCompletion() {
		BigFish big = BigFish.getInstance();
		SmallFish small = SmallFish.getInstance();

		boolean bigOut = (big == null || big.isOut());
		boolean smallOut = (small == null || small.isOut());

		if (bigOut && smallOut) { //Se ambos tiverem fora, passou o nível
			loadNextLevel();
		}
	}

	private void loadNextLevel() {
		int currentLevel = 0;
		if (currentRoom != null && currentRoom.getName() != null) {
			try {
				String digits = currentRoom.getName().replaceAll("\\D+", "");
				if (!digits.isEmpty())
					currentLevel = Integer.parseInt(digits);
			} catch (Exception ignored) {
			}
		}

		int nextLevel = currentLevel + 1;
		String nextName = "room" + nextLevel + ".txt";

		// reset singletons antes de ler nova room
		try {
			BigFish.resetInstance();
		} catch (Throwable ignored) {
		}
		try {
			SmallFish.resetInstance();
		} catch (Throwable ignored) {
		}

		Room next = null;
		File f = new File("./rooms/" + nextName);
		if (f.exists()) {
			Room freshlyRead = Room.readRoom(f, this);
			if (freshlyRead != null) {
				next = freshlyRead;
				rooms.put(nextName, freshlyRead);
				
			} else {
				next = rooms.get(nextName);
			}
			
		} else {
	
			next = rooms.get(nextName); 
		}

		if (next == null) {
			return;
		}

		currentRoom = next;

		// sincronizar singletons a partir de instâncias locais na room (se existirem)
		try {
			GameCharacter bfLocal = currentRoom.getBigFish();
			if (bfLocal != null)
				BigFish.getInstance(bfLocal.getPosition(), currentRoom);
		} catch (Throwable ignored) {
		}
		try {
			GameCharacter sfLocal = currentRoom.getSmallFish();
			if (sfLocal != null)
				SmallFish.getInstance(sfLocal.getPosition(), currentRoom);
		} catch (Throwable ignored) {
		}

		// garantir estado dos singletons
		try {
			if (BigFish.getInstance() != null) {
				BigFish.getInstance().setRoom(currentRoom);
				BigFish.getInstance().setOut(false);
			}
		} catch (Throwable ignored) {
		}
		try {
			if (SmallFish.getInstance() != null) {
				SmallFish.getInstance().setRoom(currentRoom);
				SmallFish.getInstance().setOut(false);
			}
		} catch (Throwable ignored) {
		}

		// reconstruir GUI
		try {
			ImageGUI.getInstance().clearImages();
			ImageGUI.getInstance().addImages(currentRoom.getObjects());
			ImageGUI.getInstance().update();
		} catch (Exception ignored) {
		}

		// escolher controlado (se current == null ou inválido)
		ensureControlledStillValid();
		updateStatusMessage();
	}

	//Reset no nível caso algum peixe morra
	private void restartLevel() {
		String roomName = (currentRoom != null && currentRoom.getName() != null) ? currentRoom.getName() : "room0.txt";

		try {
			BigFish.resetInstance();
		} catch (Throwable ignored) {
		}
		try {
			SmallFish.resetInstance();
		} catch (Throwable ignored) {
		}

		File f = new File("./rooms/" + roomName);
		if (!f.exists())
			return;

		Room r = Room.readRoom(f, this);
		if (r == null)
			return;

		rooms.put(roomName, r);
		currentRoom = r;

		// sincronizar singletons pela room recarregada
		try {
			GameCharacter bfLocal = currentRoom.getBigFish();
			if (bfLocal != null)
				BigFish.getInstance(bfLocal.getPosition(), currentRoom);
		} catch (Throwable ignored) {
		}
		try {
			GameCharacter sfLocal = currentRoom.getSmallFish();
			if (sfLocal != null)
				SmallFish.getInstance(sfLocal.getPosition(), currentRoom);
		} catch (Throwable ignored) {
		}

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
		try {
			if (SmallFish.getInstance() != null)
				SmallFish.getInstance().setRoom(r);
		} catch (Throwable ignored) {
		}
		try {
			if (BigFish.getInstance() != null)
				BigFish.getInstance().setRoom(r);
		} catch (Throwable ignored) {
		}
		ensureControlledStillValid();
		updateGUI();
	}

	//Notifica todos os Crabs na currentRoom para reagirem a um movimento de peixe, através de ticks
	private void notifyCrabsOnPlayerMove() {
		try {
			if (currentRoom == null)
				return;
			List<GameObject> gos = currentRoom.getGameObjects();
			for (GameObject go : gos) {
				if (go instanceof Krab) {
					try {
						((Krab) go).move(null); //move chama randomStep
					} catch (Throwable ignored) {
					}
				}
			}
			// atualizar GUI para reflectir eventuais movimentos dos crabs
			try {
				updateGUI();
			} catch (Exception ignored) {
			}
		} catch (Throwable ignored) {
		}
	}
}
