package objects;

import pt.iscte.poo.gui.ImageTile;
import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;
import pt.iscte.poo.game.Room;

/**
 * Base de todos os objectos do jogo.
 */
public abstract class GameObject implements ImageTile {
	
	public enum Weight {
	    NONE,     // objetos fixos 
	    LIGHT,    
	    HEAVY,
	}

	protected Point2D position;
	protected Room room;

	protected GameObject(Point2D position, Room r) {
		this.position = position;
		this.room = r;
	}

	protected GameObject(Room r) {
		this.room = r;
	}

	@Override
	public Point2D getPosition() {
		return position;
	}

	public Room getRoom() {
		return room;
	}

	public void setRoom(Room r) {
		this.room = r;
	}

	public void setPosition(Point2D p) {
		this.position = p;
	}

	public void setPosition(int x, int y) {
		this.position = new Point2D(x, y);
	}
	
	public static GameObject fromChar(char c, Room r, int x, int y) {
	    Point2D pos = new Point2D(x, y);

	    GameObject obj = ObjectType.create(c, pos, r);

	    if (obj instanceof Water)
	        return null;

	    return obj;  
	}

	public abstract boolean isTransposable();
	@Override
	public abstract String getName();
	@Override
	public abstract int getLayer();

	public abstract int mutation();

	public abstract void move(Vector2D delta);
	
	public abstract Weight getWeight();

}
