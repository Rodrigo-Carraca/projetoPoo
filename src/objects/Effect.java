package objects;

import pt.iscte.poo.game.Room;
import pt.iscte.poo.utils.Point2D;
import pt.iscte.poo.utils.Vector2D;


/**
 * Effect visual temporário (ex.: sangue, explosão).
 * - É um GameObject leve (Weight.NONE), não interactivo, e auto-destrói-se após N ticks.
 * - getLayer() deve ser maior do que a layer das personagens para ficar visível por cima.
 */
public class Effect extends GameObject {

    private int remainingTicks;
    private final String effectName; // nome usado pelo ImageGUI (ex.: "blood", "boom", "spark")

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
    public boolean isTransposable() {
        // efeito não bloqueia movimentos (pode ser atravessado)
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

    /**
     * Chamado pelo Room a cada tick para decrementar o life-time.
     * Quando terminar, remove-se da room (e da GUI).
     */
    public void tick() {
        remainingTicks--;
        if (remainingTicks <= 0) {
            Room r = getRoom();
            if (r != null) {
                // removeObject cuida de imageTiles/GUI
                r.removeObject(this);
            }
        }
    }
}
