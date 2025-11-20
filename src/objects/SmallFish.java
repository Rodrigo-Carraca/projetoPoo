package objects;

import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;
import pt.iscte.poo.game.Room;

/**
 * SmallFish (jogador). Singleton.
 */
public class SmallFish extends GameCharacter {

	private static SmallFish INSTANCE = null;
	private boolean facingRight = true;

	private SmallFish(Point2D pos, Room r) {
		super(pos, r);
	}

	public static synchronized SmallFish getInstance(Point2D pos, Room r) {
		if (INSTANCE == null)
			INSTANCE = new SmallFish(pos, r);
		return INSTANCE;
	}

	public static SmallFish getInstance() {
		return INSTANCE;
	}

	@Override
	public String getName() {
		return facingRight ? "smallFishRight" : "smallFishLeft";
	}

	@Override
	public void move(Vector2D delta) {
		if (delta == null)
			return;
		if (delta.getX() > 0)
			facingRight = true;
		else if (delta.getX() < 0)
			facingRight = false;
		tryMove(delta);
	}

	@Override
	public boolean isTransposable() {
		return false;
	}

	@Override
	public int mutation() {
		return 0;
	}
}
