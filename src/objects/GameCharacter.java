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
    public void move(pt.iscte.poo.utils.Vector2D delta) {
        if (delta == null) return;
        Room r = getRoom();
        if (r == null) return;

        Point2D dest = getPosition().plus(delta);
        if (!r.isInsideBounds(dest)) return;

        GameObject top = r.getTopObjectAt(dest);

        // ---------------------------------------------------------
        // 1) CÉLULA VAZIA → MOVE
        // ---------------------------------------------------------
        if (top == null) {
            r.moveObject(this, dest);
            return;
        }

        // ---------------------------------------------------------
        // 2) PASSABLE (Hollow Wall) → depende do peixe
        // ---------------------------------------------------------
        if (top instanceof objects.Passable) {
            if (((objects.Passable) top).canPass(this)) {
                r.moveObject(this, dest);
            }
            return; // se não puder passar, bloqueia
        }

        // ---------------------------------------------------------
        // 3) OBJETO TRANSPONÍVEL (ex: water)
        // ---------------------------------------------------------
        if (top.isTransposable()) {
            r.moveObject(this, dest);
            return;
        }

        // ---------------------------------------------------------
        // 4) TENTAR EMPURRAR
        //     -> construir cadeia de Movables
        // ---------------------------------------------------------
        List<GameObject> chain = new ArrayList<>();
        Point2D cur = dest;

        while (true) {
            if (!r.isInsideBounds(cur)) break;

            GameObject g = r.getTopObjectAt(cur);
            if (g == null) break;
            if (!(g instanceof Movable)) break;

            chain.add(g);
            cur = cur.plus(delta); // avança na direção do movimento
        }

        if (chain.isEmpty()) {
            return; // nada empurrável → bloqueado
        }

        Point2D beyond = cur;
        if (!r.isInsideBounds(beyond)) return;

        GameObject beyondTop = r.getTopObjectAt(beyond);
        boolean beyondFree =
                (beyondTop == null)
                || beyondTop.isTransposable()
                || (beyondTop instanceof objects.Passable
                    && ((objects.Passable)beyondTop).canPass(this));

        if (!beyondFree) return;

        // ---------------------------------------------------------
        // CONTAGEM DE HEAVIES NA CADEIA
        // ---------------------------------------------------------
        int heavyCount = 0;
        for (GameObject obj : chain) {
            if (obj.getWeight() == GameObject.Weight.HEAVY) {
                heavyCount++;
            }
        }

        boolean horizontal = (delta.getY() == 0 && delta.getX() != 0);
        boolean vertical   = (delta.getX() == 0 && delta.getY() != 0);

        // ---------------------------------------------------------
        // 5) REGRAS DO SMALLFISH
        // ---------------------------------------------------------
        if (this instanceof objects.SmallFish) {

            // No máximo UM objeto
            if (chain.size() != 1) return;

            GameObject only = chain.get(0);

            // SmallFish só empurra LIGHT
            if (only.getWeight() != GameObject.Weight.LIGHT) return;

            // horizontal OU vertical — empurra 1 light
            Point2D objTo = only.getPosition().plus(delta);
            r.moveObject(only, objTo);
            r.moveObject(this, dest);
            return;
        }

        // ---------------------------------------------------------
        // 6) REGRAS DO BIGFISH
        // ---------------------------------------------------------
        if (this instanceof objects.BigFish) {

            // BigFish NÃO atravessa hole walls (já tratado)
        	// --- bloco horizontal para BigFish (substituir o anterior) ---
        	if (horizontal) {
        	    // Permitir empurrar cadeia de qualquer tamanho (sem restrição de heavies)
        	    // Desde que o espaço "beyond" esteja livre/transponível/passable (já verificado antes)

        	    // mover do fim da cadeia para o início para evitar overlap
        	    for (int i = chain.size() - 1; i >= 0; i--) {
        	        GameObject obj = chain.get(i);
        	        Point2D objTo = obj.getPosition().plus(delta);
        	        r.moveObject(obj, objTo);
        	    }

        	    // finalmente mover o peixe
        	    r.moveObject(this, dest);
        	    return;
        	}


            // BigFish → empurrar vertical apenas 1 objeto (light ou heavy)
            if (vertical) {
                if (chain.size() != 1) return;

                GameObject obj = chain.get(0);
                Point2D objTo = obj.getPosition().plus(delta);
                r.moveObject(obj, objTo);
                r.moveObject(this, dest);
                return;
            }

            return; // diagonais não suportadas
        }

        // Outros personagens não empurram
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
