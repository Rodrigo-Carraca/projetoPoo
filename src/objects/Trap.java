package objects;

import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;

import java.util.List;

import pt.iscte.poo.game.Room;

public class Trap extends Movable implements Passable {
	public Trap(Point2D p, Room r) {
		super(p, r);
	}

	@Override
	public String getName() {
		return "trap";
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
		return Weight.HEAVY;
	}

	@Override
	public boolean canPass(GameCharacter c) {
		if(c instanceof BigFish) {
			c.die();
			return false;
		}
		return c.canPassHole();
	}
	
	@Override
	public void onFall(Room room, Point2D from, Point2D to) {
		if (room == null || from == null || to == null)
			return;
		if (!room.isInsideBounds(to))
			return;

		// recolher objects na célula abaixo
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
			if (obj instanceof GameCharacter)
				character = (GameCharacter) obj;
		}

		int newMovable = movableCount + 1;
		int newHeavy = heavyCount + (this.getWeight() == GameObject.Weight.HEAVY ? 1 : 0);

		if (character != null) {
			boolean dies = false;
			if (character instanceof SmallFish) {
				if (newHeavy > 0)
					dies = true;
				else if (newMovable > 1)
					dies = true;
			} else if (character instanceof BigFish) {
				if (newHeavy > 1)
					dies = true;
			} else {
				if (newMovable > 0)
					dies = true;
			}

			if (!dies && character instanceof BigFish) {
				int heaviesAbove = 0;
				Point2D cur = new Point2D(from.getX(), from.getY());
				while (room.isInsideBounds(cur)) {
					List<GameObject> objsHere = room.getObjectsAt(cur);
					boolean foundMovable = false;
					for (GameObject o : objsHere) {
						if (o instanceof Movable) {
							foundMovable = true;
							if (o == this) {
								if (this.getWeight() == GameObject.Weight.HEAVY)
									heaviesAbove++;
							} else {
								if (o.getWeight() == GameObject.Weight.HEAVY)
									heaviesAbove++;
							}
						}
					}
					if (!foundMovable)
						break;
					cur = new Point2D(cur.getX(), cur.getY() - 1);
				}
				if (heaviesAbove >= 2)
					dies = true;
			}

			if (dies) {
				character.die();
				room.removeObject(character);
				room.moveObject((GameObject) this, to);
				return;
			} else {
				return;
			}
		}

		// queda normal
		GameObject top = room.getTopObjectAt(to);
		if (top == null || top.isTransposable()) {
			room.moveObject((GameObject) this, to);
		}
	}
}
