package objects;

import pt.iscte.poo.game.Room;
import pt.iscte.poo.utils.Point2D;

public interface Explodable {
    void explode(Room room, Point2D center);
}
