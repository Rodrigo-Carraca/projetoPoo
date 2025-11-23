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
    
    /**
     * Move a personagem segundo um delta, aplicando regras de colisão:
     * 1) não sai dos limites
     * 2) move se célula vazia
     * 3) move se o tile for Passable e autorizar esta personagem
     * 4) move se o tile for transponível
     * 5) senão, bloqueia
     */
    @Override
	public void move(Vector2D delta) {

		Room r = getRoom();
		if (r == null || delta == null)
			return;

		Point2D dest = getPosition().plus(delta);

		if (!r.isInsideBounds(dest))
			return;

		GameObject top = r.getTopObjectAt(dest);

		// 1) célula vazia -> mover via Room
		if (top == null) {
			r.moveObject(this, dest);
			return;
		}

		// 2) Passable (ex.: HoleWall que permite passagem)
		if (top instanceof Passable) {
			if (((Passable) top).canPass(this)) {
				r.moveObject(this, dest);
				return;
			}
		}

		// 3) transponível (genérico)
		if (top.isTransposable()) {
			r.moveObject(this, dest);
			return;
		}

		// 4) tentar empurrar em cadeia se houver movables consecutivos na direção delta
		// (somente horizontal push faz sentido para cadeias, mas esta versão funciona
		// em qualquer direção)
		// coletar cadeia de movables começando em dest
		List<GameObject> chain = new ArrayList<>();
		Point2D cur = new Point2D(dest.getX(), dest.getY());
		while (true) {
			if (!r.isInsideBounds(cur))
				break;
			GameObject g = r.getTopObjectAt(cur);
			if (g == null || !(g instanceof Movable))
				break;
			chain.add(g);
			// avança uma célula na direção delta
			cur = cur.plus(delta);
		}

		// se nao há movables inchain, não é pushable neste ramo
		if (chain.isEmpty()) {
			// nada a fazer, bloqueado
			return;
		}

		Point2D beyond = new Point2D(cur.getX(), cur.getY()); // célula logo após a cadeia

		// beyond precisa estar valid e livre/transponível
		if (!r.isInsideBounds(beyond))
			return;
		GameObject beyondTop = r.getTopObjectAt(beyond);
		if (!(beyondTop == null || beyondTop.isTransposable())) {
			// não há espaço para empurrar
			return;
		}

		// regra para SmallFish: só permite empurrar cadeia se for exatamente 1 elemento
		// e LIGHT
		if (this instanceof SmallFish) {
			if (chain.size() == 1) {
				GameObject only = chain.get(0);
				if (only.getWeight() == GameObject.Weight.LIGHT) {
					// empurra
					// mover o objeto (apenas 1) para beyond
					r.moveObject(only, beyond);
					// mover o peixe para dest
					r.moveObject(this, dest);
				}
			}
			return;
		}

		// regra para BigFish (ou outras personagens que suportam empurrar cadeias):
		if (this instanceof BigFish) {
			// conta heavies na cadeia
			int heavyCount = 0;
			for (GameObject c : chain) {
				if (c.getWeight() == GameObject.Weight.HEAVY)
					heavyCount++;
			}
			// BigFish pode empurrar a cadeia se heavyCount <= 1
			if (heavyCount <= 1) {
				// empurra a cadeia inteira: mover do fim para o início para evitar sobreposição
				for (int i = chain.size() - 1; i >= 0; i--) {
					GameObject obj = chain.get(i);
					Point2D from = obj.getPosition();
					Point2D to = from.plus(delta);
					r.moveObject(obj, to);
				}
				// agora move o peixe
				r.moveObject(this, dest);
			}
			return;
		}

		// outros tipos de personagens: por omissão não empurram cadeias
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