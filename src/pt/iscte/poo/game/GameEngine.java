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

    private GameEngine() {
        rooms = new HashMap<>();
        loadGame();

        currentRoom = rooms.get("room0.txt");
        if (currentRoom == null && !rooms.isEmpty())
            currentRoom = rooms.values().iterator().next();

        // associar room às instâncias existentes
        if (SmallFish.getInstance() != null)
            SmallFish.getInstance().setRoom(currentRoom);
        if (BigFish.getInstance() != null)
            BigFish.getInstance().setRoom(currentRoom);

        // personagem controlada por padrão
        controlled = (BigFish.getInstance() != null)
                ? BigFish.getInstance()
                : SmallFish.getInstance();

        updateStatusMessage();
        updateGUI();
    }

    private void loadGame() {
        File[] files = new File("./rooms").listFiles();
        if (files == null)
            return;
        for (File f : files) {
            Room r = Room.readRoom(f, this);
            if (r != null)
                rooms.put(f.getName(), r);
        }
    }

    @Override
    public void update(Observed source) {

        // controlo
        if (ImageGUI.getInstance().wasKeyPressed()) {
            int k = ImageGUI.getInstance().keyPressed();

            if (k == KeyEvent.VK_SPACE) {
                toggleControlled();
                updateStatusMessage();
            } else {
                try {
                    Vector2D delta = pt.iscte.poo.utils.Direction.directionFor(k).asVector();
                    if (controlled != null)
                        controlled.move(delta);
                } catch (Exception ignored) {
                }
            }
        }

        // ticks / gravidade
        int t = ImageGUI.getInstance().getTicks();
        while (lastTickProcessed < t)
            processTick();

        ImageGUI.getInstance().update();

        // verificar morte de peixes
        SmallFish sf = SmallFish.getInstance();
        BigFish bf = BigFish.getInstance();

        if ((sf != null && !sf.isAlive()) || (bf != null && !bf.isAlive())) {
            restartLevel();
        }
    }

    private void processTick() {
        lastTickProcessed++;
        if (currentRoom != null)
            currentRoom.applyGravity();
    }

    public void updateGUI() {
        if (currentRoom != null) {
            try {
                ImageGUI.getInstance().clearImages();
                ImageGUI.getInstance().addImages(currentRoom.getObjects());
            } catch (Exception ignored) {
            }
        }
    }

    private void toggleControlled() {
        GameCharacter big = BigFish.getInstance();
        GameCharacter small = SmallFish.getInstance();

        if (controlled == null) {
            controlled = (big != null) ? big : small;
            return;
        }

        if (controlled == big && small != null)
            controlled = small;
        else if (controlled == small && big != null)
            controlled = big;
        else
            controlled = (big != null) ? big : small;
    }

    private void updateStatusMessage() {
        try {
            String name = (controlled != null) ? controlled.getName() : "none";
            ImageGUI.getInstance().setStatusMessage("Controlling: " + name);
        } catch (Exception ignored) {
        }
    }

    /** Reinicia o nível quando um peixe morre */
    private void restartLevel() {

        String roomName = (currentRoom != null && currentRoom.getName() != null)
                ? currentRoom.getName()
                : "room0.txt";

        // reset singletons
        try {
            BigFish.resetInstance();
        } catch (Throwable ignored) {
        }
        try {
            SmallFish.resetInstance();
        } catch (Throwable ignored) {
        }

        // reler a sala do ficheiro
        File f = new File("./rooms/" + roomName);
        if (!f.exists())
            return;

        Room r = Room.readRoom(f, this);
        if (r != null) {
            rooms.put(roomName, r);
            currentRoom = r;

            if (SmallFish.getInstance() != null)
                SmallFish.getInstance().setRoom(r);
            if (BigFish.getInstance() != null)
                BigFish.getInstance().setRoom(r);

            controlled = (BigFish.getInstance() != null)
                    ? BigFish.getInstance()
                    : SmallFish.getInstance();

            updateStatusMessage();
            updateGUI();
        }
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(Room r) {
        this.currentRoom = r;

        if (SmallFish.getInstance() != null)
            SmallFish.getInstance().setRoom(r);
        if (BigFish.getInstance() != null)
            BigFish.getInstance().setRoom(r);

        updateGUI();
    }
}
