package pt.iscte.poo.game;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import pt.iscte.poo.gui.ImageTile;
import pt.iscte.poo.utils.Point2D;

import objects.GameObject;
import objects.Water;
import objects.BigFish;
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
	public GameObject getTopObjectAt(pt.iscte.poo.utils.Point2D p) {
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
	public boolean isInsideBounds(pt.iscte.poo.utils.Point2D p) {
		try {
			if (pt.iscte.poo.gui.ImageGUI.getInstance() != null) {
				return pt.iscte.poo.gui.ImageGUI.getInstance().isWithinBounds(p);
			}
		} catch (Exception e) {
			// ignora e considera in-bounds
		}
		return true;
	}
}
