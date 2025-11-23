package objects;

import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.game.Room;

/**
 * Factory responsável pela criação de GameObjects a partir
 * do caractere lido do ficheiro de mapa.
 */
public final class ObjectType {

    private ObjectType() {
    }

    /**
     * Cria um GameObject correspondente ao token lido do mapa.
     * Retorna null para tokens desconhecidos.
     */
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
            case 'B':
                return BigFish.getInstance(pos, room);   // adapta se BigFish não for singleton
            case 'S':
                return SmallFish.getInstance(pos, room); // adapta se SmallFish não for singleton
            default:
                return null;
        }
    }
}
