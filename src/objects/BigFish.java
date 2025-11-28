package objects;

import java.util.ArrayList;
import java.util.List;

import pt.iscte.poo.game.Room;
import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;

/**
 * BigFish singleton. Sem IA (movimenta-se apenas quando controlado).
 */
public class BigFish extends GameCharacter {

	private static BigFish INSTANCE = null;
	private boolean facingRight = true;

	private BigFish(Point2D pos, Room r) {
		super(pos, r);
	}

	public static BigFish getInstance(Point2D pos, Room r) {
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

	/**
	 * Atualiza facing, chama super.move(delta) para as verificações / movimentos simples.
	 * Se o peixe **não** se moveu, re-calcula localmente a cadeia (chain) e aplica as regras do BigFish.
	 */
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
		// Verificações semelhantes às que estavam originalmente em GameCharacter
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

		if (chain.isEmpty())
			return; // nada empurrável → bloqueado

		Point2D beyond = cur;
		if (!r.isInsideBounds(beyond))
			return;

		GameObject beyondTop = r.getTopObjectAt(beyond);
		boolean beyondFree = (beyondTop == null) || beyondTop.isTransposable()
				|| (beyondTop instanceof Passable && ((Passable) beyondTop).canPass(this));
		if (!beyondFree)
			return;

		boolean horizontal = (delta.getY() == 0 && delta.getX() != 0);
		boolean vertical = (delta.getX() == 0 && delta.getY() != 0);

		// bloco horizontal: empurra toda a cadeia
		if (horizontal) {
			for (int i = chain.size() - 1; i >= 0; i--) {
				GameObject obj = chain.get(i);
				Point2D objTo = obj.getPosition().plus(delta);
				r.moveObject(obj, objTo);
			}
			r.moveObject(this, dest);
			return;
		}

		// vertical: apenas 1 objeto (light ou heavy)
		if (vertical) {
			if (chain.size() != 1)
				return;
			GameObject obj = chain.get(0);
			Point2D objTo = obj.getPosition().plus(delta);
			r.moveObject(obj, objTo);
			r.moveObject(this, dest);
			return;
		}

		// outros casos: bloqueia
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
		return false;
	}

	@Override
	public Weight getWeight() {
		return  Weight.NONE;
	}

	@Override
	public boolean canPushWeight(Weight w) {
		// TODO Auto-generated method stub
		return w == GameObject.Weight.LIGHT || w == GameObject.Weight.HEAVY;
	}
}
