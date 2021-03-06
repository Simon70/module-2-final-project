package ss.project.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ss.project.client.Controller;
import ss.project.client.HumanPlayer;
import ss.project.client.ui.GameDisplay;
import ss.project.shared.Protocol;
import ss.project.shared.computerplayer.LinearComputerPlayer;
import ss.project.shared.computerplayer.RandomComputerPlayer;
import ss.project.shared.game.Engine;
import ss.project.shared.game.Player;
import ss.project.shared.game.Vector2;
import ss.project.shared.game.Vector3;
import ss.project.shared.model.GameParameters;

import java.util.Arrays;

/**
 * Created by fw on 27/01/2017.
 */
public class EngineTest {
    private Engine engine;
    private Player linear;
    private Player random;

    @Before
    public void setUp() throws Exception {
        Controller.getController().start(false);
        linear = new LinearComputerPlayer("linear");
        linear.setId(4);
        random = new RandomComputerPlayer("random");
        random.setId(7);
        engine = new Engine(new GameParameters(4, 4, 4, 4), Arrays.asList(linear, random), false);
        engine.setUI(new GameDisplay() {
            @Override
            public void startTurn(Object waiter, HumanPlayer humanPlayer) {

            }

            @Override
            public void update() {

            }

            @Override
            public void showHint(int x, int y, int z) {

            }

            @Override
            public void removeHint(int x, int y, int z) {

            }

            @Override
            public void setCurrentPlayer(Player player) {

            }
        });
        Controller.getController().setEngine(engine);
        Controller.getController().switchTo(Controller.Panel.GAME);
    }

    @Test
    public void getWorld() throws Exception {
        Assert.assertEquals("World should be properly made.", new Vector3(4, 4, 4), engine.getWorld().getSize());
    }

    @Test
    public void addGameItem() throws Exception {
        Assert.assertTrue("It should be possible to place at 0,0,0", engine.addGameItem(Vector2.ZERO, linear));
        Assert.assertTrue("It should be possible to place at 0,0,1", engine.addGameItem(Vector2.ZERO, linear));
        Assert.assertTrue("It should be possible to place at 0,0,2", engine.addGameItem(Vector2.ZERO, linear));
        Assert.assertTrue("It should be possible to place at 0,0,3", engine.addGameItem(Vector2.ZERO, random));
        Assert.assertFalse("It should not be possible to place at 0,0,4", engine.addGameItem(Vector2.ZERO, linear));

        Assert.assertTrue("It should be possible to place at 1,0,0", engine.addGameItem(new Vector2(1, 0), random));
        Assert.assertTrue("It should be possible to place at 1,0,1", engine.addGameItem(new Vector2(1, 0), random));
        Assert.assertTrue("It should be possible to place at 1,0,2", engine.addGameItem(new Vector2(1, 0), random));
        Assert.assertTrue("It should be possible to place at 1,0,3", engine.addGameItem(new Vector2(1, 0), random));
        Assert.assertEquals("Random should have won the game", Protocol.WinReason.WINLENGTHACHIEVED, engine.getWinReason());
    }

    @Test
    public void getPlayer() throws Exception {
        Assert.assertEquals("ID 7 should be assigned to random.", random, engine.getPlayer(7));
        Assert.assertEquals("ID 4 should be assigned to linear.", linear, engine.getPlayer(4));
        Assert.assertNull("ID 1 should not be assigned to anything.", engine.getPlayer(1));
    }

    @Test
    public void getPlayerCount() throws Exception {
        Assert.assertEquals("The engine should consists of 2 players.", 2, engine.getPlayerCount());
    }

    @Test
    public void getOtherPlayer() throws Exception {
        Assert.assertEquals("Asking for the other player from linear should give random.", random, engine.getOtherPlayer(linear));
        Assert.assertEquals("Asking for the other player from random should give linear.", linear, engine.getOtherPlayer(random));
    }

    @Test
    public void startGame() throws Exception {
        engine.startGame();
        Assert.assertFalse("The game should have finished.", engine.isGameRunning());
    }

    @Test
    public void finishGame() throws Exception {
        engine.finishGame(Protocol.WinReason.BOARDISFULL, -1);
        Assert.assertEquals("Reason should be set to boardisfull.", Protocol.WinReason.BOARDISFULL, engine.getWinReason());
        Assert.assertFalse("Game should have stopped running.", engine.isGameRunning());
    }

    @Test
    public void getWinReason() throws Exception {

    }

    @Test
    public void getWinner() throws Exception {
        Assert.assertTrue("It should be possible to place at 1,0,0", engine.addGameItem(Vector2.ZERO, random));
        Assert.assertTrue("It should be possible to place at 1,0,1", engine.addGameItem(Vector2.ZERO, random));
        Assert.assertTrue("It should be possible to place at 1,0,2", engine.addGameItem(Vector2.ZERO, random));
        Assert.assertTrue("It should be possible to place at 1,0,3", engine.addGameItem(Vector2.ZERO, random));
        Assert.assertEquals("random should have won the game.", 7, engine.getWinner());
    }

}