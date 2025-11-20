package objects;

import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;
import pt.iscte.poo.game.Room;

public class SteelVertical extends GameObject {
	public SteelVertical(Point2D p, Room r) {
		super(p, r);
	}

	@Override
	public String getName() {
		return "steelvertical";
	}

	@Override
	public int getLayer() {
		return 1;
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
}
