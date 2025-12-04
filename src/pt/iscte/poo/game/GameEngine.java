package pt.iscte.poo.game;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.awt.event.KeyEvent;

import objects.SmallFish;
import objects.BigFish;
import objects.GameCharacter;
import objects.GameObject;
import objects.Krab;

import pt.iscte.poo.gui.ImageGUI;
import pt.iscte.poo.observer.Observed;
import pt.iscte.poo.observer.Observer;
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

		// associar room às instâncias singletons (defensivo)
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

		// seleccionar controlado inicial (prefere BigFish se ambos estiverem
		// indisponíveis controlado fica null)
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

			if (k == KeyEvent.VK_SPACE) {
				// Alterna entre peixes disponíveis imediatamente
				toggleControlled();
				updateStatusMessage();
			} else if (k == KeyEvent.VK_R) {
				// reiniciar nível manual
				restartLevel();
			} else {
				try {
					Vector2D delta = pt.iscte.poo.utils.Direction.directionFor(k).asVector();
					if (controlled != null && !controlled.isOut() && controlled.getRoom() != null
							&& controlled.isAlive()) {
						controlled.move(delta);

						// Notificar crabs: sempre que um peixe se move pelo jogador,
						// os crabs fazem um passo aleatório.
						notifyCrabsOnPlayerMove();
					}

					// Após mover via teclado, verificar possíveis saídas
					checkExit(BigFish.getInstance());
					checkExit(SmallFish.getInstance());
					// NOTA: aqui não chamamos normalizeControlled() porque queremos PRESERVAR a
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

		// Depois de processar ticks/gravidade, permitir que crabs reajam a movimentos
		// provocados pela gravidade (peixes ou objectos que se moveram).
		notifyCrabsOnPlayerMove();

		// 3) Atualizar GUI
		try {
			ImageGUI.getInstance().update();
		} catch (Exception ignored) {
		}

		// 4) Pós-gravidade: verificar saídas, e garantir que, se o controlado actual
		// passou a não estar disponível,
		// escolhemos automaticamente outro (mas sem sobrescrever uma escolha válida do
		// jogador).
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

		// 5) Reinício se algum peixe morreu
		SmallFish sf = SmallFish.getInstance();
		BigFish bf = BigFish.getInstance();
		if ((sf != null && !sf.isAlive()) || (bf != null && !bf.isAlive())) {
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

	/** Reconstrói a GUI a partir dos imageTiles da currentRoom. */
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

	/**
	 * Alterna o personagem controlado pelo jogador (SPACE). - Se ambos disponíveis:
	 * alterna. - Se apenas um disponível: escolhe esse (ou mantém). - Se nenhum
	 * disponível: controlled = null.
	 */
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

	/**
	 * Garante que o personagem actualmente controlado ainda é válido. Se não for,
	 * escolhe automaticamente outro (prefere BigFish). IMPORTANT: NÃO sobrescreve a
	 * escolha do jogador se esta ainda for válida.
	 */
	private void ensureControlledStillValid() {
		if (controlled == null) {
			pickInitialControlled();
			return;
		}
		// se o controlado actual ainda é válido, não faz nada
		if (controlled.isAlive() && !controlled.isOut() && controlled.getRoom() != null) {
			return;
		}
		// senão escolhe outro disponível (prefere BigFish)
		pickInitialControlled();
	}

	/**
	 * Escolhe o personagem inicial (prefere BigFish) — usado apenas quando
	 * current==null ou inválido.
	 */
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

	/**
	 * Marca peixe como "out" quando: sem room OR posição fora dos limites fixos
	 * 10x10. Remove imagem e desliga room/posição para não bloquear.
	 */
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
				fish.setPosition(new pt.iscte.poo.utils.Point2D(-1, -1));
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
				fish.setPosition(new pt.iscte.poo.utils.Point2D(-1, -1));
				fish.setRoom(null);
			} catch (Throwable ignored) {
			}
		}
	}

	/**
	 * Se ambos os peixes estão out (ou não existem), avança para o próximo nível.
	 */
	private void checkLevelCompletion() {
		BigFish big = BigFish.getInstance();
		SmallFish small = SmallFish.getInstance();

		boolean bigOut = (big == null || big.isOut());
		boolean smallOut = (small == null || small.isOut());

		if (bigOut && smallOut) {
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

		// FIRST: Try to read a fresh Room from disk (to avoid using a possibly mutated
		// cached Room).
		Room next = null;
		File f = new File("./rooms/" + nextName);
		if (f.exists()) {
			Room freshlyRead = Room.readRoom(f, this);
			if (freshlyRead != null) {
				next = freshlyRead;
				// replace cache entry with the fresh instance (ensures we don't reuse mutated
				// rooms later)
				rooms.put(nextName, freshlyRead);
			} else {
				// failed to read fresh room -> fallback to cache if available
				next = rooms.get(nextName);
			}
		} else {
			// file doesn't exist -> maybe no next level
			next = rooms.get(nextName); // try cache anyway (old behaviour)
		}

		if (next == null) {
			// no next level available
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

	/**
	 * Recarrega a room actual (quando um peixe morre). Reset singletons e relê do
	 * disco.
	 */
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

	/**
	 * Notifica todos os Crabs na currentRoom para reagirem a um movimento de peixe.
	 * Chamado sempre que um peixe se movimenta (teclado ou após o processamento de
	 * ticks/gravidade).
	 */
	private void notifyCrabsOnPlayerMove() {
		try {
			if (currentRoom == null)
				return;
			List<GameObject> gos = currentRoom.getGameObjects();
			for (GameObject go : gos) {
				if (go instanceof Krab) {
					try {
						// chamar move() do crab (a implementação de Crab deve chamar randomStep)
						((Krab) go).move(null);
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
