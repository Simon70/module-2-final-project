package ss.project.client;

import lombok.Getter;
import ss.project.server.Room;
import ss.project.shared.NetworkPlayer;
import ss.project.shared.Protocol;
import ss.project.shared.computerplayer.ComputerPlayer;
import ss.project.shared.exceptions.ProtocolException;
import ss.project.shared.game.ClientEngine;
import ss.project.shared.game.Player;
import ss.project.shared.model.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Network extends Thread {
    private Player ownedPlayer;
    private Controller controller;
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private boolean closed;
    @Getter
    private ServerInfo serverInfo;
    @Getter
    private boolean ready;
    private ClientEngine engine;

    public Network(Connection connection)
            throws IOException {
        this.controller = Controller.getController();
        socket = new Socket(connection.getAddress(), connection.getPort());
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        closed = false;
        ready = false;
        this.setName("ServerInputReader");
    }

    public ServerInfo ping() {
        try {
            String line = in.readLine();
            out.close();
            in.close();
            socket.close();
            return ServerInfo.fromString(line, socket.getInetAddress(), socket.getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Player getOwnedPlayer() {
        if (ownedPlayer == null) {
            ownedPlayer = createPlayer();
        }
        return ownedPlayer;
    }

    private Player createPlayer() {
        Class<? extends Player> playerType = ClientConfig.getInstance().playerTypes.get(ClientConfig.getInstance().playerType);
        if (playerType != null) {
            try {
                Player player = (Player) playerType.newInstance();
                if (player instanceof ComputerPlayer) {
                    ComputerPlayer computerPlayer = (ComputerPlayer) player;
                    computerPlayer.setSmartness(ClientConfig.getInstance().playerSmartness);
                }
                return player;
            } catch (InstantiationException | IllegalAccessException e) {
                return new HumanPlayer();
            }
        } else {
            return new HumanPlayer();
        }
    }

    public void run() {
        String line;
        while (!closed) {
            try {
                // read server capabilities
                line = in.readLine();
                // parse them
                serverInfo = ServerInfo.fromString(line, socket.getInetAddress(), socket.getPort());
                if (!serverInfo.getStatus().equals(ServerInfo.Status.ONLINE)) {
                    // something fucked up!
                    shutdown();
                    return;
                }
                controller.setConnected(true);

                // send your own capabilities
                ClientConfig config = ClientConfig.getInstance();
                sendMessage(getCapabilityString(
                        config.maxPlayers,
                        config.playerName,
                        config.roomSupport,
                        config.maxDimensionX,
                        config.maxDimensionY,
                        config.maxDimensionZ,
                        config.maxWinLength,
                        Controller.getController().isDoGui(),
                        config.autoRefresh));

                // await list of rooms
                if (serverInfo.isRoomSupport()) {
                    try {
                        line = in.readLine();
                        controller.setRooms(Room.parseRoomListString(line));
                    } catch (ProtocolException e) {
                        controller.showError("Expected Rooms", e.getStackTrace());
                        Controller.getController().showError("Error in getting roomlist.", e.getStackTrace());
                        sendError(4);
                        return;
                    }
                }
                ready = true;

                while (!closed) {
                    line = in.readLine();
                    if (line == null) {
                        shutdown();
                    }
                    interpretLine(line);
                }
            } catch (IOException e) {
                //This is only an error if the network is not closed.
                if (!closed) {
                    Controller.getController().showError("Error in connection.", e.getStackTrace());
                    shutdown();
                }
            }
        }
    }

    private void interpretLine(String line) {
        if (line == null) {
            return;
        }
        System.out.println("Received: " + line);
        String[] parts = line.split(" ");
        if (Protocol.Server.ASSIGNID.equals(parts[0])) {
            getOwnedPlayer().setId(Integer.parseInt(parts[1]));
            getOwnedPlayer().setName(ClientConfig.getInstance().playerName);
            controller.switchTo(Controller.Panel.MULTI_PLAYER_ROOM);
        } else if (Protocol.Server.NOTIFYMESSAGE.equals(parts[0])) {
            controller.addMessage(ChatMessage.fromString(line));
        } else if (Protocol.Server.STARTGAME.equals(parts[0])) {
            List<Player> players = new ArrayList<>();
            for (int i = 2; i < parts.length; i++) {
                NetworkPlayer np = NetworkPlayer.fromString(parts[i]);
                if (np.getId() == getOwnedPlayer().getId()) {
                    players.add(getOwnedPlayer());
                } else {
                    players.add(np);
                }
            }
            engine = new ClientEngine(GameParameters.fromString(parts[1]), players, this, getOwnedPlayer().getId());
            controller.setEngine(engine);
            controller.startGame();
            controller.switchTo(Controller.Panel.GAME);
        } else if (Protocol.Server.TURNOFPLAYER.equals(parts[0])) {
            engine.setTurn(Integer.parseInt(parts[1]));
        } else if (Protocol.Server.NOTIFYMOVE.equals(parts[0])) {
            engine.notifyMove(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } else if (Protocol.Server.NOTIFYEND.equals(parts[0])) {
            try {
                engine.notifyEnd(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            } catch (NumberFormatException e) {
                sendMessage(Protocol.createMessage(Protocol.Server.ERROR, 4));
            }
        } else if (Protocol.Server.SENDLISTROOMS.equals(parts[0])) {
            try {
                controller.setRooms(Room.parseRoomListString(line));
            } catch (ProtocolException e) {
                Controller.getController().showError("Error in roomlist format: " + line, e.getStackTrace());
            }
        } else if (Protocol.Server.SENDLEADERBOARD.equals(parts[0])) {
            try {
                List<LeaderboardEntry> leaderboardEntries = new ArrayList<>();
                for (int i = 0; i < parts.length - 1; i++) {
                    leaderboardEntries.add(LeaderboardEntry.fromString(parts[i + 1]));
                }
                controller.setLeaderBoard(leaderboardEntries);
            } catch (ProtocolException e) {
                Controller.getController().showError("Error in leaderboard format: " + line, e.getStackTrace());
            }
        } else if (Protocol.Server.ROOMCREATED.equals(parts[0])) {
            controller.refreshRoomList();
            controller.switchTo(Controller.Panel.MULTI_PLAYER_LOBBY);
        } else {
            Controller.getController().showError(line);
        }
    }

    /**
     * send a message to a ClientHandler.
     */
    public void sendMessage(String msg) {
        try {
            System.err.println(Thread.currentThread().getName() + "\nSent message: " + msg);
            out.write(msg);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            Controller.getController().showError("Tried to send message: " + msg
                    + " but that didn't work...", e.getStackTrace());
            shutdown();
        }
    }

    /**
     * close the socket connection.
     */
    public void shutdown() {
        controller.setConnected(false);
        try {
            socket.close();
        } catch (IOException e) {
            Controller.getController().showError("Failed to shutdown connection.", e.getStackTrace());
        }
        closed = true;
    }

    private String getCapabilityString(int maxPlayers, String name,
                                       boolean roomSupport, int maxX, int maxY, int maxZ, int winLength, boolean chat,
                                       boolean autoRefresh) {
        return Protocol.createMessage(Protocol.Client.SENDCAPABILITIES, maxPlayers, name,
                roomSupport, maxX, maxY, maxZ, winLength, chat, autoRefresh);
    }

    private void sendError(int errorcode) {
        sendMessage(Protocol.createMessage(Protocol.Server.ERROR, errorcode));
    }

}
