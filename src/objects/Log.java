package objects;

import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;
import pt.iscte.poo.game.Room;
import pt.iscte.poo.game.GameEngine;
import pt.iscte.poo.gui.ImageGUI;

public class Log extends GameObject implements Crushable {
	public Log(Point2D p, Room r) {
		super(p, r);
	}

	@Override
	public String getName() {
		return "trunk";
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
		/* static */
	}

	@Override
	public int mutation() {
		return 0;
	}

	@Override
	public Weight getWeight() {
		return Weight.NONE;
	}

	/**
	 * Quando esmagado por um objeto pesado, parte o Log e cria efeito de
	 * estilhaços.
	 */
	@Override
	public void onCrushedBy(Room room, GameObject crusher, Point2D pos) {
		if (room == null)
			return;

		Point2D p = getPosition();
		try {
			if (p != null) {
				Effect fx = new Effect(p, room, "boom", 6);
				room.addObject(fx);
			}
		} catch (Throwable ignored) {
		}

		// remover o tronco
		try {
			room.removeObject(this);
		} catch (Throwable ignored) {
		}

		// Forçar GUI update para mostrar efeito/removals
		try {
			GameEngine.getInstance().updateGUI();
		} catch (Throwable t) {
			try {
				if (ImageGUI.getInstance() != null)
					ImageGUI.getInstance().update();
			} catch (Throwable ignored) {
			}
		}
	}
}
