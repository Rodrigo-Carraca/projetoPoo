package objects;

import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;

import java.util.List;

import pt.iscte.poo.game.Room;

public class Cup extends Movable {
	public Cup(Point2D p, Room r) {
		super(p, r);
	}

	@Override
	public String getName() {
		return "cup";
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

	@Override
	public void onFall(Room room, Point2D from, Point2D to) {
		if (room == null || from == null || to == null)
			return;
		if (!room.isInsideBounds(to))
			return;

		// Recolher objects na célula abaixo
		List<GameObject> cell = room.getObjectsAt(to);

		int movableCount = 0;
		int heavyCount = 0;
		GameCharacter character = null;

		for (GameObject obj : cell) {
			if (obj instanceof Movable) {
				movableCount++;
				if (obj.getWeight() == GameObject.Weight.HEAVY)
					heavyCount++;
			}
			if (obj instanceof GameCharacter) {
				character = (GameCharacter) obj;
			}
		}

		int newMovable = movableCount + 1; // inclui este Cup
		int newHeavy = heavyCount + (this.getWeight() == GameObject.Weight.HEAVY ? 1 : 0);

		if (character != null) {
			boolean dies = false;

			// regras SmallFish
			if (character instanceof SmallFish) {
				if (newHeavy > 0)
					dies = true;
				else if (newMovable > 1)
					dies = true;
			}
			// regras BigFish
			else if (character instanceof BigFish) {
				if (newHeavy > 1)
					dies = true;
			}
			// outro personagem
			else {
				if (newMovable > 0)
					dies = true;
			}

			if (dies) {
				character.die();
				room.removeObject(character);
				room.moveObject((GameObject) this, to);
				return;
			} else {
				// personagem sobrevive -> cup fica onde está
				return;
			}
		}

		// queda normal: se célula abaixo vazia ou transposable -> mover
		GameObject top = room.getTopObjectAt(to);
		if (top == null || top.isTransposable()) {
			room.moveObject((GameObject) this, to);
		}
		// caso contrário: bloqueado, não mover
	}
}
