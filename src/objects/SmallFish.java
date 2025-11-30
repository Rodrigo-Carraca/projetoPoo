package objects;
	
	import pt.iscte.poo.utils.Point2D;
	import pt.iscte.poo.utils.Vector2D;

import java.util.ArrayList;
import java.util.List;

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

			// atualizar facing
			if (delta.getX() > 0)
				facingRight = true;
			else if (delta.getX() < 0)
				facingRight = false;

			// guarda posição inicial para saber se super.move já moveu o peixe
			Point2D start = getPosition();

			// executa a parte comum (move simples se possível)
			super.move(delta);

			// se super.move alterou a posição, já movemos — nada mais a fazer
			if (!getPosition().equals(start))
				return;

			// ---- A partir daqui: super.move não moveu o peixe -> recalcular e aplicar regras específicas

			Room r = getRoom();
			if (r == null)
				return;

			Point2D dest = start.plus(delta);
			if (!r.isInsideBounds(dest))
				return;

			GameObject top = r.getTopObjectAt(dest);
			if (top == null) {
				// caso improvável porque super.move faria isto, mas mantemos coerência
				r.moveObject(this, dest);
				return;
			}

			// se é Passable e pode passar, já teria sido movido por super; se não, bloqueia
			if (top instanceof Passable) {
				if (((Passable) top).canPass(this)) {
					r.moveObject(this, dest);
				}
				return;
			}

			if (top.isTransposable()) {
				r.moveObject(this, dest);
				return;
			}

			// Construir lista de Movables
			List<GameObject> chain = new ArrayList<>();
			Point2D cur = dest;
			while (true) {
				if (!r.isInsideBounds(cur))
					break;

				GameObject g = r.getTopObjectAt(cur);
				if (g == null)
					break;
				if (!(g instanceof Movable))
					break;

				chain.add(g);
				cur = cur.plus(delta);
			}

			// SmallFish rules: must be exactly one movable and it must be LIGHT
			if (chain.size() != 1)
				return;

			GameObject only = chain.get(0);
			if (only.getWeight() != GameObject.Weight.LIGHT)
				return;

			Point2D beyond = cur;
			if (!r.isInsideBounds(beyond))
				return;

			GameObject beyondTop = r.getTopObjectAt(beyond);
			boolean beyondFree = (beyondTop == null) || beyondTop.isTransposable()
					|| (beyondTop instanceof Passable && ((Passable) beyondTop).canPass(this));
			if (!beyondFree)
				return;

			// Empurra o único objeto (horizontal ou vertical) e move o peixe
			Point2D objTo = only.getPosition().plus(delta);
			r.moveObject(only, objTo);
			r.moveObject(this, dest);
		}
	
		@Override
		public boolean isTransposable() {
			return false;
		}
	
		@Override
		public int mutation() {
			return 0;
		}
	
		@Override
		public boolean canPassHole() {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public Weight getWeight() {
			// TODO Auto-generated method stub
			return Weight.NONE;
		}

		@Override
		public boolean canPushWeight(Weight w) {
			// TODO Auto-generated method stub
			return w == GameObject.Weight.LIGHT;
		}
		public static void resetInstance() {
		    INSTANCE = null;
		}

	}
