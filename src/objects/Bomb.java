package objects;

import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;
import pt.iscte.poo.game.Room;

public class Bomb extends GameObject implements Movable {
	public Bomb(Point2D p, Room r) {
		super(p, r);
	}

	@Override
	public String getName() {
		return "bomb";
	}

	@Override
	public int getLayer() {
		return 2;
	}

	@Override
	public boolean isTransposable() {
		return false;
	}

	@Override
	public void move(Vector2D d) {
		/* static */ }

	@Override
	public int mutation() {
		return 0;
	}

	@Override
	public Weight getWeight() {
		// TODO Auto-generated method stub
		return Weight.LIGHT;
	}
}
