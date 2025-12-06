package objects;

import pt.iscte.poo.game.Room;
import pt.iscte.poo.utils.Point2D;

public abstract class Movable extends GameObject {

    public Movable(Point2D position, Room room) {
        super(position, room);
    }

    public Movable(Room room) {
        super(room);
    }

    public boolean canBePushedBy(GameCharacter c) {
        if (c == null)
            return false;
        return c.canPushWeight(this.getWeight());
    }

    public void onFall(Room room, Point2D from, Point2D to) {
        // default: no-op
    }
}
