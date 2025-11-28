package objects;

import pt.iscte.poo.game.Room;
import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;

import java.util.ArrayList;
import java.util.List;

/**
 * Bomb — comportamento de queda / explosão.
 *
 * Regras:
 * - Só explode se estiver a cair (wasFalling) e colidir com um objecto "explodível".
 * - Não explode em contacto com peixe.
 * - NÃO explode nem remove Walls ou HoleWalls — essas são parte do mapa.
 * - Explosão remove objectos não-transponíveis (exceto Walls/HoleWalls) na célula central e nas 4 adjacentes,
 *   e mata/removerá peixes nas células afetadas.
 */
public class Bomb extends Movable implements Explodable {

    private boolean armed = false;      // arma-se apenas quando começa a cair
    private boolean wasFalling = false; // true se no tick anterior a bomba moveu-se para baixo

    public Bomb(Point2D p, Room r) {
        super(p, r);
    }

    public Bomb(Room r) {
        super(r);
    }

    @Override
    public String getName() {
        return "bomb";
    }

    @Override
    public int getLayer() {
        return 2;
    }

    @Override
    public boolean isTransposable() {
        return false;
    }

    @Override
    public void move(Vector2D d) {
        // sem movimento de comando direto
    }

    @Override
    public int mutation() {
        return 0;
    }

    @Override
    public Weight getWeight() {
        return Weight.LIGHT;
    }

    /**
     * Helper: verifica se um objecto é uma parede que NÃO deve ser removida por explosão.
     */
    private boolean isIndestructibleWall(GameObject o) {
        if (o == null) return false;
        // evita dependência de nomes: verificamos classes concretas
        return (o.getClass().getSimpleName().equals("Wall") ||
                o.getClass().getSimpleName().equals("HoleWall"));
        // Se preferir, substitua acima por instanceof Wall / instanceof HoleWall se essas classes estiverem no mesmo pacote.
    }

    /**
     * Explode centrado em 'center'. Remove objectos solidos (não transponíveis) excepto Walls/HoleWalls,
     * mata peixes nas células afetadas e remove a si própria.
     */
    @Override
    public void explode(Room room, Point2D center) {
        if (room == null || center == null) return;
        if (!room.isInsideBounds(center)) return;

        int bx = center.getX();
        int by = center.getY();

        // CENTRO: se houver peixe -> mata ; se houver objecto sólido e NÃO for parede -> remove
        GameObject centerTop = room.getTopObjectAt(center);
        if (centerTop != null) {
            if (centerTop instanceof GameCharacter) {
                ((GameCharacter) centerTop).die();
                room.removeObject(centerTop);
            } else if (!centerTop.isTransposable() && !isIndestructibleWall(centerTop)) {
                room.removeObject(centerTop);
            }
            // se for parede (Wall/HoleWall) ou transponível -> não remover
        }

        // ADJACENTES N,S,E,W
        Point2D[] adj = new Point2D[] {
            new Point2D(bx, by - 1),
            new Point2D(bx, by + 1),
            new Point2D(bx - 1, by),
            new Point2D(bx + 1, by)
        };

        for (Point2D p : adj) {
            if (!room.isInsideBounds(p)) continue;
            List<GameObject> objs = new ArrayList<>(room.getObjectsAt(p));
            for (GameObject o : objs) {
                if (o == null) continue;
                // ignorar tiles transponíveis (ex: água)
                if (o.isTransposable()) continue;
                // não remover paredes indestrutíveis
                if (isIndestructibleWall(o)) continue;
                if (o instanceof GameCharacter) {
                    ((GameCharacter) o).die();
                    room.removeObject(o);
                } else {
                    room.removeObject(o);
                }
            }
        }

        // remover a própria bomba
        room.removeObject(this);

        // Forçar atualização do GUI para refletir as remoções
        try {
            if (pt.iscte.poo.gui.ImageGUI.getInstance() != null) {
                try {
                    pt.iscte.poo.game.GameEngine.getInstance().updateGUI();
                } catch (Exception ignore) {
                    pt.iscte.poo.gui.ImageGUI.getInstance().update();
                }
            }
        } catch (Exception ignored) { }
    }

    /**
     * onFall é invocado pelo Room para que a bomba decida se se move, explode, etc.
     *
     * Lógica:
     * - Se célula abaixo vazia/transposable -> move para lá; marcar wasFalling = true e armá-la.
     * - Se célula abaixo contém um peixe -> não explode e não se move.
     * - Se célula contém um objecto sólido:
     *      - se objecto é Wall ou HoleWall -> NÃO explode (é indestrutível por bomba) e a bomba fica pousada;
     *      - caso contrário (objecto "explodível"): explode apenas se wasFalling && armed.
     */
    @Override
    public void onFall(Room room, Point2D from, Point2D to) {
        if (room == null || from == null || to == null) return;
        if (!room.isInsideBounds(to)) return;

        GameObject top = room.getTopObjectAt(to);

        // Caso 1: célula vazia ou apenas com transponíveis (ex: água) -> mover para lá
        if (top == null || top.isTransposable()) {
            room.moveObject(this, to);
            wasFalling = true;
            armed = true; // arma-se quando começa/continua a cair
            return;
        }

        // Caso 2: topo é GameCharacter -> não explode e não se move
        if (top instanceof GameCharacter) {
            wasFalling = false;
            return;
        }

        // Caso 3: topo é objecto sólido -> verificar se é parede indestrutível
        if (isIndestructibleWall(top)) {
            // não explode sobre paredes, e não se move; fica pousada
            wasFalling = false;
            return;
        }

        // topo é objecto sólido "normal" (não parede): explode apenas se vinha de queda e está armada
        if (wasFalling && armed) {
            explode(room, to);
            return;
        } else {
            // está pousada sobre um objecto sem ter vindo de queda => não explode
            wasFalling = false;
            return;
        }
    }
}
