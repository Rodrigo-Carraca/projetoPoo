package pt.iscte.poo.game;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import java.awt.event.KeyEvent;

import objects.SmallFish;
import objects.BigFish;
import objects.GameCharacter;

import pt.iscte.poo.gui.ImageGUI;
import pt.iscte.poo.observer.Observed;
import pt.iscte.poo.observer.Observer;
import pt.iscte.poo.utils.Vector2D;

/**
 * GameEngine - Singleton clássico (private constructor + getInstance()).
 */
public class GameEngine implements Observer {

    private static GameEngine INSTANCE = null;

    public static GameEngine getInstance() {
        if (INSTANCE == null)
        	INSTANCE = new GameEngine();
        return INSTANCE;
    }

    private final Map<String, Room> rooms;
    private Room currentRoom;
    private int lastTickProcessed = 0;
    private GameCharacter controlled;

    // construtor privado conforme padrão Solitão
    private GameEngine() {
        rooms = new HashMap<>();
        loadGame();

        currentRoom = rooms.get("room0.txt");
        if (currentRoom == null && !rooms.isEmpty()) currentRoom = rooms.values().iterator().next();

        // associa room às instâncias (se já criadas durante readRoom)
        if (SmallFish.getInstance() != null) SmallFish.getInstance().setRoom(currentRoom);
        if (BigFish.getInstance() != null) BigFish.getInstance().setRoom(currentRoom);

        // default: começar a controlar BigFish se existir
        controlled = (BigFish.getInstance() != null) ? BigFish.getInstance() : SmallFish.getInstance();

        updateStatusMessage();
        updateGUI();
    }

    private void loadGame() {
        File[] files = new File("./rooms").listFiles();
        if (files == null) return;
        for (File f : files) {
            Room r = Room.readRoom(f, this);
            if (r != null) rooms.put(f.getName(), r);
        }
    }

    @Override
    public void update(Observed source) {
        if (ImageGUI.getInstance().wasKeyPressed()) {
            int k = ImageGUI.getInstance().keyPressed();

            if (k == KeyEvent.VK_SPACE) {
                toggleControlled();
                updateStatusMessage();
            } else {
                try {
                    Vector2D delta = pt.iscte.poo.utils.Direction.directionFor(k).asVector();
                    if (controlled != null) controlled.move(delta);
                } catch (Exception e) {
                    // tecla não mapeada -> ignora
                }
            }
        }

        int t = ImageGUI.getInstance().getTicks();
        while (lastTickProcessed < t) processTick();

        ImageGUI.getInstance().update();
    }

    private void processTick() {
        lastTickProcessed++;
    }

    public void updateGUI() {
        if (currentRoom != null) {
            ImageGUI.getInstance().clearImages();
            ImageGUI.getInstance().addImages(currentRoom.getObjects());
        }
    }

    private void toggleControlled() {
        GameCharacter big = BigFish.getInstance();
        GameCharacter small = SmallFish.getInstance();

        if (controlled == null) {
            controlled = (big != null) ? big : small;
            return;
        }

        if (controlled == big && small != null) controlled = small;
        else if (controlled == small && big != null) controlled = big;
        else controlled = (big != null) ? big : small;
    }

    private void updateStatusMessage() {
        try {
            String name = (controlled != null) ? controlled.getName() : "none";
            ImageGUI.getInstance().setStatusMessage("Controlling: " + name);
        } catch (Exception e) {
            // proteger em ambientes de teste
        }
    }

    public Room getCurrentRoom() { return currentRoom; }

    public void setCurrentRoom(Room r) {
        this.currentRoom = r;
        if (SmallFish.getInstance() != null) SmallFish.getInstance().setRoom(r);
        if (BigFish.getInstance() != null) BigFish.getInstance().setRoom(r);
        updateGUI();
    }
}
