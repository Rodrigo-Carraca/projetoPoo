package objects;

import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;
import pt.iscte.poo.game.Room;

public class Log extends Movable implements Crushable {
    public Log(Point2D p, Room r) {
        super(p, r);
    }

    @Override
    public String getName() {
        return "trunk";
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
        /* static */ 
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
     * Log não cai por si — onFall deixa-o onde está.
     * O comportamento de partir quando um HEAVY cai sobre ele
     * é tratado por Crushable.onCrushedBy(), chamado por Room.applyGravity().
     */
    @Override
    public void onFall(Room room, Point2D from, Point2D to) {
        // não faz nada (log é estático)
    }

    /** Quando esmagado por um objeto pesado, parte-se (remove-se da sala). */
    @Override
    public void onCrushedBy(Room room, GameObject crusher, Point2D pos) {
        if (room == null)
            return;
        room.removeObject(this);
    }
}
