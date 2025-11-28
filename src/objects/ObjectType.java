package objects;

import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.game.Room;

public final class ObjectType {

    private ObjectType() {
    }

    public static GameObject create(char token, Point2D pos, Room room) {
        switch (token) {
            case ' ':
                return new Water(pos, room);
            case 'W':
                return new Wall(pos, room);
            case 'X':
                return new HoleWall(pos, room);
            case 'H':
                return new SteelH(pos, room);
            case 'V':
                return new SteelVertical(pos, room);
            case 'C':
                return new Cup(pos, room);
            case 'R':
                return new Rock(pos, room);
            case 'A':
                return new Anchor(pos, room);
            case 'b':
                return new Bomb(pos, room);
            case 'T':
                return new Trap(pos, room);
            case 'Y':
                return new Log(pos, room);
            case 'U':
                return new Buoy(pos, room);     // Boia
            case 'K':
                return new Crab(pos, room);     // Caranguejo (krab)
            case 'B':
                return BigFish.getInstance(pos, room);
            case 'S':
                return SmallFish.getInstance(pos, room);
            default:
                return null;
        }
    }
}
