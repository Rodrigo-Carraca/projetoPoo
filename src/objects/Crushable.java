package objects;

import pt.iscte.poo.game.Room;
import pt.iscte.poo.utils.Point2D;

/**
 * Marca um objecto que pode ser esmagado por outro objecto.
 * onCrushedBy é invocado quando 'crusher' (o objecto que cai) esmaga este objecto
 * na posição 'pos' dentro de 'room'.
 */
public interface Crushable {
    void onCrushedBy(Room room, GameObject crusher, Point2D pos);
}
