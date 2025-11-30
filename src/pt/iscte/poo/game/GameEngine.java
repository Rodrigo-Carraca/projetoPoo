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
 * GameEngine - garante que, ao carregar/reset/next-level, as instâncias dos peixes
 * (singletons) ficam correctamente sincronizadas com a Room e com a GUI.
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

    private GameEngine() {
        rooms = new HashMap<>();
        loadGame();

        currentRoom = rooms.get("room0.txt");
        if (currentRoom == null && !rooms.isEmpty())
            currentRoom = rooms.values().iterator().next();

        // associar room às instâncias (defensivo)
        try { if (SmallFish.getInstance() != null) SmallFish.getInstance().setRoom(currentRoom); } catch (Throwable ignored) {}
        try { if (BigFish.getInstance() != null) BigFish.getInstance().setRoom(currentRoom); } catch (Throwable ignored) {}

        normalizeControlled();
        updateStatusMessage();
        updateGUI();
    }

    private void loadGame() {
        File[] files = new File("./rooms").listFiles();
        if (files == null) return;
        for (File f : files) {
            if (!f.isFile() || !f.getName().endsWith(".txt")) continue;
            Room r = Room.readRoom(f, this);
            if (r != null) rooms.put(f.getName(), r);
        }
    }

    @Override
    public void update(Observed source) {

        // input teclado
        if (ImageGUI.getInstance().wasKeyPressed()) {
            int k = ImageGUI.getInstance().keyPressed();

            if (k == KeyEvent.VK_SPACE) {
                toggleControlled();
                updateStatusMessage();
            } else {
                try {
                    Vector2D delta = pt.iscte.poo.utils.Direction.directionFor(k).asVector();
                    if (controlled != null && !controlled.isOut() && controlled.getRoom() != null) {
                        controlled.move(delta);
                    }

                    // verificar saída imediato e normalizar
                    checkExit(BigFish.getInstance());
                    checkExit(SmallFish.getInstance());
                    normalizeControlled();
                    updateStatusMessage();
                } catch (Exception ignored) {}
            }
        }

        // ticks / gravidade
        int t = ImageGUI.getInstance().getTicks();
        while (lastTickProcessed < t) processTick();

        // forçar update GUI
        ImageGUI.getInstance().update();

        // pós-gravidade: verificar saídas / normalizar
        try { checkExit(BigFish.getInstance()); } catch (Exception ignored) {}
        try { checkExit(SmallFish.getInstance()); } catch (Exception ignored) {}
        normalizeControlled();
        updateStatusMessage();

        // se algum peixe morreu -> reiniciar
        SmallFish sf = SmallFish.getInstance();
        BigFish bf = BigFish.getInstance();
        if ((sf != null && !sf.isAlive()) || (bf != null && !bf.isAlive())) {
            restartLevel();
            return;
        }

        // se ambos saíram -> next level
        checkLevelCompletion();
    }

    private void processTick() {
        lastTickProcessed++;
        if (currentRoom != null) currentRoom.applyGravity();
    }

    public void updateGUI() {
        if (currentRoom != null) {
            try {
                ImageGUI.getInstance().clearImages();
                ImageGUI.getInstance().addImages(currentRoom.getObjects());
            } catch (Exception ignored) {}
        } else {
            try { ImageGUI.getInstance().clearImages(); } catch (Exception ignored) {}
        }
    }

    private void toggleControlled() {
        GameCharacter big = BigFish.getInstance();
        GameCharacter small = SmallFish.getInstance();

        if (controlled == null) {
            if (big != null && !big.isOut() && big.getRoom() != null) controlled = big;
            else if (small != null && !small.isOut() && small.getRoom() != null) controlled = small;
            else controlled = null;
            return;
        }

        if (controlled == big) {
            if (small != null && !small.isOut() && small.getRoom() != null) controlled = small;
            else if (big != null && !big.isOut() && big.getRoom() != null) controlled = big;
            else controlled = null;
        } else {
            if (big != null && !big.isOut() && big.getRoom() != null) controlled = big;
            else if (small != null && !small.isOut() && small.getRoom() != null) controlled = small;
            else controlled = null;
        }
    }

    private void normalizeControlled() {
        GameCharacter big = BigFish.getInstance();
        GameCharacter small = SmallFish.getInstance();

        if (big != null && !big.isOut() && big.getRoom() != null) { controlled = big; return; }
        if (small != null && !small.isOut() && small.getRoom() != null) { controlled = small; return; }
        controlled = null;
    }

    private void updateStatusMessage() {
        try {
            String name = (controlled != null) ? controlled.getName() : "none";
            ImageGUI.getInstance().setStatusMessage("Controlling: " + name);
        } catch (Exception ignored) {}
    }

    /**
     * Marca peixe como "out" quando: sem room OR posição fora dos limites fixos 10x10.
     */
    private void checkExit(GameCharacter fish) {
        if (fish == null) return;
        if (fish.isOut()) return;

        if (fish.getRoom() == null) {
            fish.setOut(true);
            try { ImageGUI.getInstance().removeImage(fish); } catch (Exception ignored) {}
            return;
        }
        if (fish.getPosition() == null) {
            fish.setOut(true);
            try { ImageGUI.getInstance().removeImage(fish); } catch (Exception ignored) {}
            try { fish.setPosition(new pt.iscte.poo.utils.Point2D(-1, -1)); fish.setRoom(null); } catch (Throwable ignored) {}
            return;
        }
        int x = fish.getPosition().getX();
        int y = fish.getPosition().getY();
        boolean inside10x10 = (x >= 0 && x < 10 && y >= 0 && y < 10);
        if (!inside10x10) {
            fish.setOut(true);
            try { ImageGUI.getInstance().removeImage(fish); } catch (Exception ignored) {}
            try { fish.setPosition(new pt.iscte.poo.utils.Point2D(-1, -1)); fish.setRoom(null); } catch (Throwable ignored) {}
        }
    }

    private void checkLevelCompletion() {
        BigFish big = BigFish.getInstance();
        SmallFish small = SmallFish.getInstance();

        boolean bigOut = (big == null || big.isOut());
        boolean smallOut = (small == null || small.isOut());

        if (bigOut && smallOut) loadNextLevel();
    }

    /**
     * Carrega room{current+1}.txt. Garante criação/sincronização dos singletons dos peixes.
     */
    private void loadNextLevel() {
        // inferir nível actual
        int currentLevel = 0;
        if (currentRoom != null && currentRoom.getName() != null) {
            try {
                String digits = currentRoom.getName().replaceAll("\\D+", "");
                if (!digits.isEmpty()) currentLevel = Integer.parseInt(digits);
            } catch (Exception ignored) {}
        }
        int nextLevel = currentLevel + 1;
        String nextName = "room" + nextLevel + ".txt";

        // reset singletons antes de ler a nova room
        try { BigFish.resetInstance(); } catch (Throwable ignored) {}
        try { SmallFish.resetInstance(); } catch (Throwable ignored) {}

        // tentar obter da cache ou ler do disco
        Room next = rooms.get(nextName);
        if (next == null) {
            File f = new File("./rooms/" + nextName);
            if (!f.exists()) return;
            next = Room.readRoom(f, this);
            if (next == null) return;
            rooms.put(nextName, next);
        }

        currentRoom = next;

        // -- importante: se a room "local" tem peixes, usar as posições para criar os singletons
        try {
            GameCharacter bfLocal = currentRoom.getBigFish();
            if (bfLocal != null) {
                // força criação do singleton apontando para a mesma posição e room
                BigFish.getInstance(bfLocal.getPosition(), currentRoom);
            }
        } catch (Throwable ignored) {}

        try {
            GameCharacter sfLocal = currentRoom.getSmallFish();
            if (sfLocal != null) {
                SmallFish.getInstance(sfLocal.getPosition(), currentRoom);
            }
        } catch (Throwable ignored) {}

        // garantir que singletons (se existirem) têm a room correcta e out=false
        try { if (BigFish.getInstance() != null) { BigFish.getInstance().setRoom(currentRoom); BigFish.getInstance().setOut(false); } } catch (Throwable ignored) {}
        try { if (SmallFish.getInstance() != null) { SmallFish.getInstance().setRoom(currentRoom); SmallFish.getInstance().setOut(false); } } catch (Throwable ignored) {}

        // reconstruir GUI imageTiles a partir da room atual (limpar + adicionar)
        try {
            ImageGUI.getInstance().clearImages();
            ImageGUI.getInstance().addImages(currentRoom.getObjects());
            ImageGUI.getInstance().update();
        } catch (Exception ignored) {}

        normalizeControlled();
        updateStatusMessage();
    }

    /**
     * Recarrega a room actual (quando um peixe morre). Reset singletons e relê do disco.
     */
    private void restartLevel() {
        String roomName = (currentRoom != null && currentRoom.getName() != null) ? currentRoom.getName() : "room0.txt";

        try { BigFish.resetInstance(); } catch (Throwable ignored) {}
        try { SmallFish.resetInstance(); } catch (Throwable ignored) {}

        File f = new File("./rooms/" + roomName);
        if (!f.exists()) return;

        Room r = Room.readRoom(f, this);
        if (r == null) return;

        rooms.put(roomName, r);
        currentRoom = r;

        // sincronizar singletons usando posições da room lida
        try {
            GameCharacter bfLocal = currentRoom.getBigFish();
            if (bfLocal != null) BigFish.getInstance(bfLocal.getPosition(), currentRoom);
        } catch (Throwable ignored) {}
        try {
            GameCharacter sfLocal = currentRoom.getSmallFish();
            if (sfLocal != null) SmallFish.getInstance(sfLocal.getPosition(), currentRoom);
        } catch (Throwable ignored) {}

        try {
            ImageGUI.getInstance().clearImages();
            ImageGUI.getInstance().addImages(currentRoom.getObjects());
            ImageGUI.getInstance().update();
        } catch (Exception ignored) {}

        normalizeControlled();
        updateStatusMessage();
    }

    public Room getCurrentRoom() { return currentRoom; }

    public void setCurrentRoom(Room r) {
        this.currentRoom = r;
        try { if (SmallFish.getInstance() != null) SmallFish.getInstance().setRoom(r); } catch (Throwable ignored) {}
        try { if (BigFish.getInstance() != null) BigFish.getInstance().setRoom(r); } catch (Throwable ignored) {}
        normalizeControlled();
        updateGUI();
    }
}
