package objects;

import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.game.Room;

/**
 * Enumerado que mapeia caracteres do ficheiro para tipos de objecto
 * e implementa a fábrica para criação de instâncias.
 */
public enum ObjectType {

    WATER(' ', null) {
        @Override public GameObject create(Point2D pos, Room r) { return new Water(pos, r); }
    },
    WALL('W', "wall") {
        @Override public GameObject create(Point2D pos, Room r) { return new Wall(pos, r); }
    },
    HOLE_WALL('X', "holedWall") {
        @Override public GameObject create(Point2D pos, Room r) { return new HoleWall(pos, r); }
    },
    STEEL_H('H', "steelHorizontal") {
        @Override public GameObject create(Point2D pos, Room r) { return new SteelH(pos, r); }
    },
    STEEL_V('V', "steelVertical") {
        @Override public GameObject create(Point2D pos, Room r) { return new SteelVertical(pos, r); }
    },
    CUP('C', "cup") {
        @Override public GameObject create(Point2D pos, Room r) { return new Cup(pos, r); }
    },
    ROCK('R', "stone") {
        @Override public GameObject create(Point2D pos, Room r) { return new Rock(pos, r); }
    },
    ANCHOR('A', "anchor") {
        @Override public GameObject create(Point2D pos, Room r) { return new Anchor(pos, r); }
    },
    BOMB('b', "bomb") {
        @Override public GameObject create(Point2D pos, Room r) { return new Bomb(pos, r); }
    },
    TRAP('T', "trap") {
        @Override public GameObject create(Point2D pos, Room r) { return new Trap(pos, r); }
    },
    LOG('Y', "trunk") {
        @Override public GameObject create(Point2D pos, Room r) { return new Log(pos, r); }
    },
    BIGFISH('B', "bigFishRight") {
        @Override public GameObject create(Point2D pos, Room r) { return BigFish.getInstance(pos, r); }
    },
    SMALLFISH('S', "smallFishRight") {
        @Override public GameObject create(Point2D pos, Room r) { return SmallFish.getInstance(pos, r); }
    },
    UNKNOWN((char)0, null) {
        @Override public GameObject create(Point2D pos, Room r) { return null; }
    };

    private final char ch;
    private final String defaultSprite;

    ObjectType(char ch, String defaultSprite) {
        this.ch = ch;
        this.defaultSprite = defaultSprite;
    }

    public char getChar() { return ch; }
    public String getDefaultSprite() { return defaultSprite; }

    public abstract GameObject create(Point2D pos, Room r);

    public static ObjectType fromChar(char c) {
        for (ObjectType t : values()) {
            if (t.ch == c) return t;
        }
        return UNKNOWN;
    }
}
