package pt.iscte.poo.game;

import java.io.File;
import java.util.*;

import pt.iscte.poo.gui.ImageTile;
import pt.iscte.poo.gui.ImageGUI;
import pt.iscte.poo.utils.Point2D;

import objects.GameObject;
import objects.Krab;
import objects.Water;
import objects.BigFish;
import objects.SmallFish;
import objects.Movable;
import objects.Rock;
import objects.GameCharacter;
import objects.Crushable;


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

					// Água sempre adicionada
					Water w = new Water(pos, room);
					room.objects.add(w);
					room.imageTiles.add(w);

					// Criar objeto
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
			System.err.println("Erro a ler sala: " + e.getMessage());
			return null;
		}

		return room;
	}

	//GUI
	public List<ImageTile> getObjects() {
		List<ImageTile> list = new ArrayList<>(imageTiles);

		list.sort((a, b) -> {
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
		});

		return list;
	}

	
	//GETTERS e SETTERS
	public List<GameObject> getGameObjects() {
		return new ArrayList<>(objects);
	}

	public BigFish getBigFish() {
		return bigFish;
	}

	public SmallFish getSmallFish() {
		return smallFish;
	}

	public String getName() {
		return name;
	}

	public void setBigFish(BigFish b) {
		bigFish = b;
	}

	public void setSmallFish(SmallFish s) {
		smallFish = s;
	}

	
	public GameObject getTopObjectAt(Point2D p) {
		GameObject top = null;
		for (GameObject go : objects) {
			if (go.getPosition() != null && go.getPosition().equals(p)) {
				if (top == null || go.getLayer() >= top.getLayer())
					top = go;
			}
		}
		return top;
	}

	public List<GameObject> getObjectsAt(Point2D p) {
		List<GameObject> l = new ArrayList<>();
		for (GameObject go : objects) {
			if (go.getPosition() != null && go.getPosition().equals(p))
				l.add(go);
		}
		return l;
	}

	public boolean isInsideBounds(Point2D p) {
		try {
			if (ImageGUI.getInstance() != null)
				return ImageGUI.getInstance().isWithinBounds(p);
		} catch (Exception ignored) {
		}
		return true;
	}

	public void removeObject(GameObject obj) {
	    if (obj == null)
	        return;

	    // remover de objects
	    while (objects.remove(obj)) {
	    }
	    // remover de imageTiles
	    while (imageTiles.remove(obj)) {
	    }

	    // se era um peixe, limpar referência
	    if (obj == bigFish)
	        bigFish = null;
	    if (obj == smallFish)
	        smallFish = null;

	    // Tentar também remover a imagem da GUI e forçar update.
	    try {
	        if (ImageGUI.getInstance() != null) {
	            // removeImage tende a ser mais seguro do que clear/addImages em termos de flicker
	            ImageGUI.getInstance().removeImage(obj);
	            ImageGUI.getInstance().update();
	        }
	    } catch (Exception ignored) {
	    }
	}


	public void moveObject(GameObject obj, Point2D to) {
	    if (obj == null || to == null) return;

	    Point2D from = obj.getPosition();
	    // se não havia posição anterior, só atualiza
	    if (from == null) {
	        obj.setPosition(to);
	        return;
	    }

	    // Calcular deslocamento
	    int dx = to.getX() - from.getX();

	    // Executa o movimento (actualiza posição do objecto)
	    obj.setPosition(to);

	    // --- REGRAS ESPECIAIS: quando uma Rock é MOVIMENTADA HORIZONTALMENTE,
	    // cria-se um Crab por cima da posição ORIGINAL, se possível.
	    try {
	        if (obj instanceof Rock && dx != 0) { // movimento horizontal
	            Point2D above = new Point2D(from.getX(), from.getY() - 1);
	            // verificar limites
	            if (isInsideBounds(above)) {
	                // verificar se a célula acima está livre (top == null ou transposable)
	                GameObject topAbove = getTopObjectAt(above);
	                boolean free = (topAbove == null) || topAbove.isTransposable();

	                // não criar se não estiver livre
	                if (free) {
	                    // garantir que não exista já um Crab nessa célula
	                    boolean crabAlready = false;
	                    for (GameObject g : getObjectsAt(above)) {
	                        if (g instanceof Krab) {
	                            crabAlready = true;
	                            break;
	                        }
	                    }

	                    if (!crabAlready) {
	                        Krab crab = new Krab(above, this);
	                        objects.add(crab);
	                        imageTiles.add(crab);
	                        // tentar actualizar GUI com segurança
	                        try {
	                            if (ImageGUI.getInstance() != null) {
	                                // adicionar só o novo tile é suficiente
	                                List<ImageTile> single = new ArrayList<>();
	                                single.add(crab);
	                                ImageGUI.getInstance().addImages(single);
	                                ImageGUI.getInstance().update();
	                            }
	                        } catch (Exception ignored) {}
	                    }
	                }
	            }
	        }
	    } catch (Throwable ignored) {
	        // nunca falhar o movimento por causa da lógica de spawn
	    }
	}


	public void applyGravity() {

	    List<GameObject> snapshot = new ArrayList<>(objects);
	    Map<GameObject, Point2D> origPos = new HashMap<>();
	    for (GameObject g : snapshot)
	        origPos.put(g, g.getPosition());

	    for (GameObject go : snapshot) {

	        Point2D start = origPos.get(go);
	        if (start == null)
	            continue;
	        if (go.getPosition() == null)
	            continue;

	        // Já moveu este tick?
	        if (!go.getPosition().equals(start))
	            continue;

	        // Só nos interessa Movable com peso
	        if (!(go instanceof Movable))
	            continue;
	        GameObject.Weight w = go.getWeight();
	        if (w == GameObject.Weight.NONE)
	            continue;

	        // Se há personagem na mesma célula → não cai
	        boolean characterInStart = false;
	        for (GameObject obj : getObjectsAt(start)) {
	            if (obj instanceof GameCharacter) {
	                characterInStart = true;
	                break;
	            }
	        }
	        if (characterInStart)
	            continue;

	        Point2D below = new Point2D(start.getX(), start.getY() + 1);
	        if (!isInsideBounds(below))
	            continue;

	        // Obter o objecto top na célula de baixo
	        GameObject topBelow = getTopObjectAt(below);

	        // --- Caso ESPECIAL: heavy a cair sobre um Crushable -> esmagamento
	        if (w == GameObject.Weight.HEAVY && topBelow instanceof Crushable) {
	            try {
	                ((Crushable) topBelow).onCrushedBy(this, go, below);
	            } catch (Exception ignored) {
	            }
	            // mover o objeto pesado para a célula abaixo (após esmagamento)
	            moveObject(go, below);
	            continue;
	        }

	        // Caso normal: delegar ao comportamento do objeto (onFall)
	        try {
	            ((Movable) go).onFall(this, start, below);
	        } catch (Exception ignored) {
	        }
	    }
	}
}
