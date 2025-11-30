package objects;

import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.game.Room;

public interface Crushable {
    void onCrushedBy(Room room, GameObject crusher, Point2D pos);
}
