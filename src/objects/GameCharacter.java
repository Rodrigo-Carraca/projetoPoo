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
    private boolean isOut = false; // Estado para saber se o peixe já saiu

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

    // Getter e Setter para saber se o peixe saiu
    public boolean isOut() {
        return isOut;
    }

    public void setOut(boolean out) {
        this.isOut = out;
    }

    @Override
    public void move(Vector2D delta) {
        if (delta == null || !alive || isOut)
            return;
        Room r = getRoom();
        if (r == null)
            return;

        Point2D dest = getPosition().plus(delta);

        // --- USAR limites FIXOS 10x10 
        boolean inside10x10 = dest.getX() >= 0 && dest.getX() < 10 && dest.getY() >= 0 && dest.getY() < 10;

        // se o destino estiver FORA dos limites 10x10 => considerada saída
        if (!inside10x10) {
            setOut(true);
            try { ImageGUI.getInstance().removeImage(this); } catch (Exception ignored) {}
            if (r != null) r.removeObject(this);
            try {
                setPosition(new Point2D(-1, -1));
                setRoom(null);
            } catch (Throwable ignored) {}
            try { if (ImageGUI.getInstance() != null) ImageGUI.getInstance().update(); } catch (Exception ignored) {}
            return;
        }

        GameObject top = r.getTopObjectAt(dest);

        // 1) CÉLULA VAZIA → MOVE
        if (top == null) {
            r.moveObject(this, dest);
            return;
        }

        // 2) PASSABLE (Hollow Wall, etc.)
        if (top instanceof Passable) {
            if (((Passable) top).canPass(this)) {
                r.moveObject(this, dest);
            }
            return;
        }

        // 3) OBJETO TRANSPOSABLE (ex: water, EXIT)
        if (top.isTransposable()) {
            r.moveObject(this, dest);
            return;
        }

        // resto das regras de empurrar etc. (se aplicável serão tratadas nas subclasses)
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
