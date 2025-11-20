package objects;

import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;

/**
 * BigFish singleton. Sem IA (movimenta-se apenas quando controlado).
 */
public class BigFish extends GameCharacter {

	private static BigFish INSTANCE = null;
	private boolean facingRight = true;

	private BigFish(Point2D pos, pt.iscte.poo.game.Room r) {
		super(pos, r);
	}

	public static BigFish getInstance(Point2D pos, pt.iscte.poo.game.Room r) {
		if (INSTANCE == null)
			INSTANCE = new BigFish(pos, r);
		return INSTANCE;
	}

	public static BigFish getInstance() {
		return INSTANCE;
	}

	@Override
	public String getName() {
		return facingRight ? "bigFishRight" : "bigFishLeft";
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
