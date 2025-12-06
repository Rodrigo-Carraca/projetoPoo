package objects;

import pt.iscte.poo.game.Room;
import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;
import pt.iscte.poo.gui.ImageGUI;

import java.util.Random;

public class Krab extends Movable {

    private final Random rnd = new Random();

    public Krab(Point2D p, Room r) {
        super(p, r);
    }

    @Override
    public String getName() {
        return "krab";
    }

    @Override
    public int getLayer() {
        return 3;
    }

    @Override
    public boolean isTransposable() {
        return false;
    }

	@Override
	public void move(Vector2D d) {
		// Apenas dispara um passo aleatório quando o motor o invoca
		try {
			Room r = getRoom();
			if (r == null)
				return;
			randomStep(r);
		} catch (Throwable t) {
		}
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
    public boolean canBePushedBy(GameCharacter c) {
        // não pode ser empurrado
        return false;
    }

    @Override
    public void onFall(Room room, Point2D from, Point2D to) {
        if (room == null || from == null || to == null) return;
        if (!room.isInsideBounds(to)) return;
        GameObject top = room.getTopObjectAt(to);
        if (top == null || top.isTransposable()) {
            room.moveObject(this, to);
        }
    }

    //Tenta mover horizontalmente; tratamento de interações com peixes/armadilha.
    public boolean tryMoveHorizontal(Room room, int dx) {
        if (room == null || getPosition() == null) return false;
        Point2D dest = new Point2D(getPosition().getX() + dx, getPosition().getY());
        if (!room.isInsideBounds(dest)) return false;

        GameObject top = room.getTopObjectAt(dest);

        if (top == null || top.isTransposable()) {
            room.moveObject(this, dest);
            return true;
        }

        if (top instanceof SmallFish) {
            ((SmallFish) top).die();
            room.removeObject(top);
            room.moveObject(this, dest);
            return true;
        } else if (top instanceof BigFish) {
            // Crab morre se tocar no BigFish -> remover crab (e imagem)
            try { ImageGUI.getInstance().removeImage(this); } catch (Exception ignored) {}
            room.removeObject(this);
            return false;
        } else if (top instanceof Trap) {
            try { ImageGUI.getInstance().removeImage(this); } catch (Exception ignored) {}
            room.removeObject(this);
            return false;
        } else {
            return false;
        }
    }

    public void randomStep(Room room) {
        int dx = rnd.nextBoolean() ? 1 : -1;
        tryMoveHorizontal(room, dx);
    }
}
