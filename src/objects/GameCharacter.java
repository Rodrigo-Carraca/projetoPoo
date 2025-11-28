package objects;

import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;

import java.util.ArrayList;

import java.util.List;

import pt.iscte.poo.game.Room;
import pt.iscte.poo.gui.ImageGUI;

/** Base para personagens móveis (BigFish, SmallFish). */
public abstract class GameCharacter extends GameObject {

	private boolean alive = true;

	public GameCharacter(Point2D position, Room room) {
		super(position, room);
	}

	@Override
	public int getLayer() {
		return 3;
	}

	@Override
	public boolean isTransposable() {
		return false;
	}

	public boolean isAlive() {
		return alive;
	}

	@Override
	public void move(Vector2D delta) {
		if (delta == null)
			return;

		Room r = getRoom();
		if (r == null)
			return;

		Point2D dest = getPosition().plus(delta);
		if (!r.isInsideBounds(dest))
			return;

		GameObject top = r.getTopObjectAt(dest);

		// 1) CÉLULA VAZIA → MOVE
		if (top == null) {
			r.moveObject(this, dest);
			return;
		}

		// 2) PASSABLE (Hollow Wall) → depende do peixe (canPass implementado por objetos passáveis)
		if (top instanceof Passable) {
			if (((Passable) top).canPass(this)) {
				r.moveObject(this, dest);
			}
			return; // se não puder passar, bloqueia
		}

		// 3) OBJETO TRANSPOSABLE (ex: water)
		if (top.isTransposable()) {
			r.moveObject(this, dest);
			return;
		}
	}

	public void die() {
		if (!alive)
			return;
		alive = false;
		Room r = getRoom();
		if (r != null) {
			r.removeObject(this); // remove de objects e imageTiles
		}
		setRoom(null);
		setPosition(new Point2D(-1, -1));
		try {
			if (ImageGUI.getInstance() != null)
				ImageGUI.getInstance().update();
		} catch (Exception ignored) {
		}
	}

	public abstract boolean canPassHole();

	public abstract boolean canPushWeight(GameObject.Weight w);
}
