package objects;

import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;
import pt.iscte.poo.game.Room;

public class Cup extends GameObject {
    public Cup(Point2D p, Room r) { super(p, r); }

    @Override public String getName() { return "cup"; }
    @Override public int getLayer() { return 2; }
    @Override public boolean isTransposable() { return false; }
    @Override public void move(Vector2D d) { /* static */ }
    @Override public int mutation() { return 0; }
}
