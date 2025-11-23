package pt.iscte.poo.game;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import pt.iscte.poo.gui.ImageGUI;
import pt.iscte.poo.gui.ImageTile;
import pt.iscte.poo.utils.Point2D;

import objects.GameObject;
import objects.Movable;
import objects.Water;
import objects.BigFish;
import objects.GameCharacter;
import objects.SmallFish;

/**
 * Representa uma sala (Room). Implementação de readRoom usando Scanner e
 * fornece getObjects() público.
 */
public class Room {

	private List<ImageTile> imageTiles;
	private List<GameObject> objects;
	private BigFish bigFish;
	private SmallFish smallFish;
	private String name;

	public Room(String name) {
		this.name = name;
		this.objects = new ArrayList<>();
		this.imageTiles = new ArrayList<>();
	}

	/**
	 * Lê um ficheiro de sala usando Scanner.
	 */
	public static Room readRoom(File f, GameEngine ge) {
		Room room = new Room(f.getName());

		try (Scanner sc = new Scanner(f)) {
			int y = 0;
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line == null)
					line = "";

				for (int x = 0; x < line.length(); x++) {
					char c = line.charAt(x);
					Point2D pos = new Point2D(x, y);

					// 1) adiciona sempre água no fundo
					Water w = new Water(pos, room);
					room.objects.add(w);
					room.imageTiles.add(w);

					// 2) cria o objecto principal (se houver)
					GameObject go = GameObject.fromChar(c, room, x, y);
					if (go != null) {
						room.objects.add(go);
						room.imageTiles.add(go);

						if (go instanceof BigFish)
							room.bigFish = (BigFish) go;
						if (go instanceof SmallFish)
							room.smallFish = (SmallFish) go;
					}
				}
				y++;
			}
		} catch (Exception e) {
			System.err.println("Erro ao ler room: " + f.getName() + " -> " + e.getMessage());
			e.printStackTrace();
			return null;
		}

		return room;
	}
	/**
	 * Retorna a lista ordenada por layer, y e x — método público usado pelo
	 * GameEngine.
	 */
	public List<ImageTile> getObjects() {
		// copia para não alterar a lista original
		List<ImageTile> list = new ArrayList<>(imageTiles);

		Collections.sort(list, new Comparator<ImageTile>() {
			@Override
			public int compare(ImageTile a, ImageTile b) {
				int la = (a instanceof GameObject) ? ((GameObject) a).getLayer() : 0;
				int lb = (b instanceof GameObject) ? ((GameObject) b).getLayer() : 0;
				if (la != lb)
					return Integer.compare(la, lb);

				Point2D pa = (a instanceof GameObject) ? ((GameObject) a).getPosition() : null;
				Point2D pb = (b instanceof GameObject) ? ((GameObject) b).getPosition() : null;
				if (pa == null || pb == null)
					return 0;
				if (pa.getY() != pb.getY())
					return Integer.compare(pa.getY(), pb.getY());
				return Integer.compare(pa.getX(), pb.getX());
			}
		});

		return list;
	}

	public List<GameObject> getGameObjects() {
		return new ArrayList<>(objects);
	}

	public BigFish getBigFish() {
		return bigFish;
	}

	public SmallFish getSmallFish() {
		return smallFish;
	}

	public void setBigFish(BigFish b) {
		this.bigFish = b;
	}

	public void setSmallFish(SmallFish s) {
		this.smallFish = s;
	}

	public String getName() {
		return name;
	}

	/**
	 * Retorna o GameObject de mais alta layer na posição indicada (x,y), ou null se
	 * não houver objecto nessa posição.
	 */
	public GameObject getTopObjectAt(Point2D p) {
		GameObject top = null;
		for (GameObject go : objects) {
			if (go.getPosition() != null && go.getPosition().getX() == p.getX()
					&& go.getPosition().getY() == p.getY()) {
				if (top == null || go.getLayer() >= top.getLayer()) {
					top = go;
				}
			}
		}
		return top;
	}

	/**
	 * Verifica se a posição está dentro dos limites visíveis (delegate para
	 * ImageGUI). Se ImageGUI não estiver inicializada, assume true para evitar
	 * NPEs.
	 */
	public boolean isInsideBounds(Point2D p) {
		try {
			if (ImageGUI.getInstance() != null) {
				return ImageGUI.getInstance().isWithinBounds(p);
			}
		} catch (Exception e) {
			// ignora e considera in-bounds
		}
		return true;
	}
	
	/**
     * Retorna todos os GameObjects que estão na posição p.
     * Útil para gravidade no checkpoint seguinte.
     */
    public List<GameObject> getObjectsAt(Point2D p) {
        List<GameObject> list = new ArrayList<>();
        for (GameObject go : objects) {
            if (go.getPosition() != null && go.getPosition().equals(p))
                list.add(go);
        }
        return list;
    }
    
    public void moveObject(GameObject obj, Point2D to) {
        obj.setPosition(to);
    }
    
	// Room.java
	public void removeObject(GameObject obj) {
		if (obj == null)
			return;
		while (objects.remove(obj)) {
		}
		while (imageTiles.remove(obj)) {
		}
		if (obj instanceof BigFish && bigFish == obj)
			bigFish = null;
		if (obj instanceof SmallFish && smallFish == obj)
			smallFish = null;
		try {
			if (ImageGUI.getInstance() != null)
				ImageGUI.getInstance().update();
		} catch (Exception ignored) {
		}
	}
	
	public void applyGravity() {
		// snapshot para processar um tick sem efeitos em cadeia imediatos
		List<GameObject> snapshot = new ArrayList<>(objects);
		Map<GameObject, Point2D> origPos = new HashMap<>();
		for (GameObject go : snapshot)
			origPos.put(go, go.getPosition());

		for (GameObject go : snapshot) {
			Point2D startPos = origPos.get(go);
			if (startPos == null)
				continue;
			if (go.getPosition() == null)
				continue;
			if (!go.getPosition().equals(startPos))
				continue;

			// só para movables com peso
			if (!(go instanceof Movable))
				continue;
			GameObject.Weight w = go.getWeight();
			if (w == GameObject.Weight.NONE)
				continue;

			Point2D below = new Point2D(startPos.getX(), startPos.getY() + 1);

			if (!isInsideBounds(below))
				continue;

			// --- novo: obter todos os objectos nessa célula (não só o 'top')
			List<GameObject> cellObjs = getObjectsAt(below); // implementa getObjectsAt se ainda não tens

			// 1) se existir algum GameCharacter nessa célula -> trata como vítima
			boolean killedSomeone = false;
			for (GameObject cellObj : cellObjs) {
				if (cellObj instanceof GameCharacter) {
					GameCharacter victim = (GameCharacter) cellObj;
					// podes decidir aqui se apenas HEAVY mata. Exemplo: só HEAVY mata:
					if (w == GameObject.Weight.HEAVY) {
						victim.die(); // deve remover do Room (ou faz removeObject depois)
						// garante que a lista interna é atualizada
						removeObject(victim);
						killedSomeone = true;
					} else {
						// se quiseres que LIGHT também mate, descomenta estas linhas:
						// victim.die(); removeObject(victim); killedSomeone = true;
					}
				}
			}

			if (killedSomeone) {
				// desloca o objecto para a célula onde o(s) peixe(s) estavam
				moveObject(go, below);
				if (go instanceof Movable)
					((Movable) go).onFall(this);
				continue;
			}

			// 2) se não houver personagem, procede como antes:
			// verifica top na célula (o teu método getTopObjectAt continua válido)
			GameObject top = getTopObjectAt(below);

			if (top == null) {
				moveObject(go, below);
				if (go instanceof Movable)
					((Movable) go).onFall(this);
				continue;
			}

			// se o top for transponível (por ex. water com isTransposable()=true)
			if (top.isTransposable()) {
				moveObject(go, below);
				if (go instanceof Movable)
					((Movable) go).onFall(this);
				continue;
			}

			// caso contrário -> bloqueado
		}
	}
}
                
