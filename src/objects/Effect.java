package objects;

import pt.iscte.poo.game.Room;
import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;

public class Effect extends GameObject {

    private int remainingTicks;
    private final String effectName; // blood ou boom

    public Effect(Point2D pos, Room r, String effectName, int ticks) {
        super(pos, r);
        this.effectName = effectName;
        this.remainingTicks = Math.max(1, ticks);
    }

    @Override
    public String getName() {
        return effectName;
    }

    // Colocar uma layer alta para sobrepor personagens (as personagens usam layer 3)
    @Override
    public int getLayer() {
        return 10;
    }

    @Override
    public boolean isTransposable() { //efeito pode ser atravessado
        return true;
    }

    @Override
    public int mutation() {
        return 0;
    }

    @Override
    public void move(Vector2D delta) {
        // não se move por si
    }

    @Override
    public Weight getWeight() {
        return Weight.NONE;
    }

    //Chamado pelo Room a cada tick para decrementar o life-time.
    //Quando terminar, remove-se da room (e da GUI).
    public void tick() {
        remainingTicks--;
        if (remainingTicks <= 0) {
            Room r = getRoom();
            if (r != null) {
                r.removeObject(this);
            }
        }
    }
}
