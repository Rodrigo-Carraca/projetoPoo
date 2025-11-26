package pt.iscte.poo.game;

import java.io.File;
import java.util.*;

import pt.iscte.poo.gui.ImageTile;
import pt.iscte.poo.gui.ImageGUI;
import pt.iscte.poo.utils.Point2D;

import objects.GameObject;
import objects.Water;
import objects.BigFish;
import objects.SmallFish;
import objects.Movable;
import objects.GameCharacter;

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

    /* ---------------------------------------------------------
     * LER MAPA
     * --------------------------------------------------------- */
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

    /* ---------------------------------------------------------
     * GUI
     * --------------------------------------------------------- */
    public List<ImageTile> getObjects() {
        List<ImageTile> list = new ArrayList<>(imageTiles);

        list.sort((a, b) -> {
            int la = (a instanceof GameObject) ? ((GameObject)a).getLayer() : 0;
            int lb = (b instanceof GameObject) ? ((GameObject)b).getLayer() : 0;
            if (la != lb)
                return Integer.compare(la, lb);

            Point2D pa = (a instanceof GameObject) ? ((GameObject)a).getPosition() : null;
            Point2D pb = (b instanceof GameObject) ? ((GameObject)b).getPosition() : null;
            if (pa == null || pb == null) return 0;

            if (pa.getY() != pb.getY())
                return Integer.compare(pa.getY(), pb.getY());

            return Integer.compare(pa.getX(), pb.getX());
        });

        return list;
    }

    /* ---------------------------------------------------------
     * GETTERS
     * --------------------------------------------------------- */
    public List<GameObject> getGameObjects() {
        return new ArrayList<>(objects);
    }

    public BigFish getBigFish() { return bigFish; }
    public SmallFish getSmallFish() { return smallFish; }

    public void setBigFish(BigFish b) { bigFish = b; }
    public void setSmallFish(SmallFish s) { smallFish = s; }

    /* ---------------------------------------------------------
     * UTILIDADES DE CELULAS
     * --------------------------------------------------------- */
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
        } catch (Exception ignored) {}
        return true;
    }

    /* ---------------------------------------------------------
     * REMOVER OBJETO
     * --------------------------------------------------------- */
    public void removeObject(GameObject obj) {
        if (obj == null) return;
        while (objects.remove(obj)) {}
        while (imageTiles.remove(obj)) {}

        if (obj == bigFish) bigFish = null;
        if (obj == smallFish) smallFish = null;
    }

    /* ---------------------------------------------------------
     * MOVER OBJETO
     * --------------------------------------------------------- */
    public void moveObject(GameObject obj, Point2D to) {
        obj.setPosition(to);
    }

    /* ---------------------------------------------------------
     * GRAVIDADE COM CONTAGEM DE HEAVIES ACIMA
     * --------------------------------------------------------- */
    public void applyGravity() {

        List<GameObject> snapshot = new ArrayList<>(objects);
        Map<GameObject, Point2D> origPos = new HashMap<>();
        for (GameObject g : snapshot)
            origPos.put(g, g.getPosition());

        for (GameObject go : snapshot) {

            Point2D start = origPos.get(go);
            if (start == null) continue;
            if (go.getPosition() == null) continue;

            // já moveu este tick
            if (!go.getPosition().equals(start)) continue;

            // não móvel → ignora
            if (!(go instanceof Movable)) continue;
            GameObject.Weight w = go.getWeight();
            if (w == GameObject.Weight.NONE) continue;

            // SE HÁ PERSONAGEM NA MESMA CÉLULA → NÃO CAI MAIS
            boolean characterInStart = false;
            for (GameObject obj : getObjectsAt(start)) {
                if (obj instanceof GameCharacter) {
                    characterInStart = true;
                    break;
                }
            }
            if (characterInStart) continue;

            Point2D below = new Point2D(start.getX(), start.getY() + 1);
            if (!isInsideBounds(below)) continue;

            List<GameObject> cell = getObjectsAt(below);

            int movableCount = 0;
            int heavyCount = 0;

            for (GameObject obj : cell) {
                if (obj instanceof Movable) {
                    movableCount++;
                    if (obj.getWeight() == GameObject.Weight.HEAVY)
                        heavyCount++;
                }
            }

            int newMovable = movableCount + 1;
            int newHeavy = heavyCount + (w == GameObject.Weight.HEAVY ? 1 : 0);

            GameCharacter character = null;
            for (GameObject obj : cell) {
                if (obj instanceof GameCharacter) {
                    character = (GameCharacter) obj;
                    break;
                }
            }

            if (character != null) {

                boolean dies = false;

                // regras SmallFish
                if (character instanceof SmallFish) {
                    if (newHeavy > 0) dies = true;
                    else if (newMovable > 1) dies = true;
                }
                // regras BigFish (inicial: conta heavies na célula do peixe)
                else if (character instanceof BigFish) {
                    if (newHeavy > 1) dies = true;
                }
                // outro personagem
                else {
                    if (newMovable > 0) dies = true;
                }

                // se BigFish sobreviveu à verificação direta, contamos a pilha contígua acima
                if (!dies && character instanceof BigFish) {
                    int heaviesAbove = 0;

                    // começamos em 'start' (célula imediatamente acima do peixe)
                    Point2D cur = new Point2D(start.getX(), start.getY());

                    while (isInsideBounds(cur)) {
                        List<GameObject> objsHere = getObjectsAt(cur);
                        boolean foundMovable = false;
                        for (GameObject o : objsHere) {
                            if (o instanceof Movable) {
                                foundMovable = true;
                                // se for o próprio go já em objsHere, evitamos dupla contagem:
                                if (o == go) {
                                    if (go.getWeight() == GameObject.Weight.HEAVY) {
                                        heaviesAbove++;
                                    }
                                } else {
                                    if (o.getWeight() == GameObject.Weight.HEAVY) {
                                        heaviesAbove++;
                                    }
                                }
                            }
                        }

                        // Se não encontrou movables nesta célula, a pilha contígua termina
                        if (!foundMovable) break;

                        // sobe uma célula (y-1)
                        cur = new Point2D(cur.getX(), cur.getY() - 1);
                    }

                    // se existem 2 ou mais heavies empilhados imediatamente acima do peixe -> morre
                    if (heaviesAbove >= 2) {
                        dies = true;
                    }
                }

                if (dies) {
                    // personagem morre → objeto entra na célula
                    character.die();
                    removeObject(character);
                    moveObject(go, below);
                    continue;
                } else {
                    // personagem sobrevive → objeto fica NA CÉLULA DE CIMA (start)
                    continue;
                }
            }

            // queda normal
            GameObject top = getTopObjectAt(below);

            if (top == null || top.isTransposable()) {
                moveObject(go, below);
                continue;
            }
        }
    }
}
