package objects;

import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;
import pt.iscte.poo.game.Room;

/** Base para personagens móveis (BigFish, SmallFish). */
public abstract class GameCharacter extends GameObject {

    public GameCharacter(Point2D position, Room room) {
        super(position, room);
    }

    @Override
    public int getLayer() {
        return 3;
    }

    @Override
    public boolean isTransposable() {
        return false;
    }

    @Override
    public abstract void move(Vector2D delta);
    
    /**
     * Helper: tenta mover para dest = position + delta verificando colisões.
     * - SmallFish pode atravessar HoleWall (se for SmallFish) — isso é tratado em chamadas.
     * - Usa Room.getTopObjectAt(dest) e isInsideBounds(dest).
     */
    protected boolean tryMove(Vector2D delta) {
        Point2D dest = this.getPosition().plus(delta);
        Room r = this.getRoom();
        if (r == null) return false;

        if (!r.isInsideBounds(dest)) return false;

        GameObject top = r.getTopObjectAt(dest);
        if (top == null) {
            setPosition(dest);
            return true;
        }

        // SmallFish can pass HoleWall
        if (top instanceof HoleWall && this instanceof SmallFish) {
            setPosition(dest);
            return true;
        }

        if (top.isTransposable()) {
            setPosition(dest);
            return true;
        }

        // blocked
        return false;
    }
}