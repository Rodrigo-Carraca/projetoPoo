package objects;

import pt.iscte.poo.game.Room;

public interface Movable {

	default boolean canBePushedBy(GameCharacter c) {
        if (c == null) return false;
        if (!(this instanceof GameObject)) return false;

        GameObject g = (GameObject) this;
        return c.canPushWeight(g.getWeight());
    }
	
	default void onFall(Room room) {
        // comportamento por omissão: nada
    }

}
