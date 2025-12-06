package objects;

import pt.iscte.poo.game.GameEngine;
import pt.iscte.poo.game.Room;
import pt.iscte.poo.gui.ImageGUI;
import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;

import java.util.ArrayList;
import java.util.List;

public class Bomb extends Movable implements Explodable {

	private boolean armed = false; // arma-se apenas quando começa a cair
	private boolean wasFalling = false; // true se no tick anterior a bomba moveu-se para baixo

	public Bomb(Point2D p, Room r) {
		super(p, r);
	}

	public Bomb(Room r) {
		super(r);
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
		// sem movimento de comando direto
	}

	@Override
	public int mutation() {
		return 0;
	}

	@Override
	public Weight getWeight() {
		return Weight.LIGHT;
	}

	@Override
	public void explode(Room room, Point2D center) {
		if (room == null || center == null)
			return;
		if (!room.isInsideBounds(center))
			return;

		int bx = center.getX();
		int by = center.getY();

		// --- 1) Remover objecto central se sólido ou matar peixe se for peixe (mas
		// spawn effect antes)
		GameObject centerTop = room.getTopObjectAt(center);
		if (centerTop != null) {
			try {
				// spawn effect na célula central (mesmo que seja peixe)
				Effect fxCenter = new Effect(center, room, "boom", 8);
				room.addObject(fxCenter);
			} catch (Throwable ignored) {
			}
			try {
				if (centerTop instanceof GameCharacter) {
					((GameCharacter) centerTop).die();
					room.removeObject(centerTop);
				} else if (!centerTop.isTransposable()) {
					room.removeObject(centerTop);
				}
			} catch (Throwable ignored) {
			}
		} else {
			// mesmo que a célula esteja vazia, podemos deixar um efeito central
			try {
				Effect fxCenter = new Effect(center, room, "boom", 8);
				room.addObject(fxCenter);
			} catch (Throwable ignored) {
			}
		}

		// --- 2) Posições adjacentes — removemos sobre uma cópia da lista para evitar
		// concurrent modification
		Point2D[] adj = new Point2D[] { new Point2D(bx, by - 1), new Point2D(bx, by + 1), new Point2D(bx - 1, by),
				new Point2D(bx + 1, by) };

		for (Point2D p : adj) {
			if (p == null)
				continue;
			if (!room.isInsideBounds(p))
				continue;

			// spawn visual na célula adjacente (se válida) antes de remover objetos
			try {
				Effect fx = new Effect(p, room, "boom", 8);
				room.addObject(fx);
			} catch (Throwable ignored) {
			}

			List<GameObject> objs = new ArrayList<>(room.getObjectsAt(p));
			for (GameObject o : objs) {
				if (o == null)
					continue;
				try {
					// ignorar tiles transposable (ex: água)
					if (o.isTransposable())
						continue;
					if (o instanceof GameCharacter) {
						((GameCharacter) o).die();
						room.removeObject(o);
					} else {
						room.removeObject(o);
					}
				} catch (Throwable ignored) {
				}
			}
		}

		// --- 3) remover a própria bomba
		try {
			room.removeObject(this);
		} catch (Throwable ignored) {
		}

		// FORÇAR actualização do GUI para refletir imediatamente as remoções/effects.
		try {
			if (ImageGUI.getInstance() != null) {
				try {
					GameEngine.getInstance().updateGUI();
				} catch (Exception ignore) {
					ImageGUI.getInstance().update();
				}
			}
		} catch (Exception ignored) {
		}
	}

	@Override
	public void onFall(Room room, Point2D from, Point2D to) {
		if (room == null || from == null || to == null)
			return;
		if (!room.isInsideBounds(to))
			return;

		GameObject top = room.getTopObjectAt(to);

		// Caso 1: célula vazia ou apenas com transponíveis (ex: água) -> mover para lá
		if (top == null || top.isTransposable()) {
			room.moveObject(this, to);
			wasFalling = true;
			armed = true; // arma-se quando começa/continua a cair
			return;
		}

		// Caso 2: topo é GameCharacter -> não explode e não se move
		if (top instanceof GameCharacter) {
			wasFalling = false;
			return;
		}
		// topo é objecto sólido "normal" (não parede): explode apenas se vinha de queda e está armada
		if (wasFalling && armed) {
			explode(room, to);
			return;
		} else {
			// está pousada sobre um objecto sem ter vindo de queda => não explode
			wasFalling = false;
			return;
		}
	}
}
