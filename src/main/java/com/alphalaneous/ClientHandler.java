package com.alphalaneous;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientHandler extends WebSocketServer {

    private static final int portNumber = 7438;
    private static final ConcurrentLinkedQueue<User> users = new ConcurrentLinkedQueue<>();

    public ClientHandler() {
        super(new InetSocketAddress(portNumber));
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        users.add(new User(webSocket));
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        User user = getUser(webSocket);
        if(user != null) {
            users.remove(user);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        try {
            JSONObject object = Utils.tryJsonObject(s);

            if (object == null) return;

            String status = object.getString("status");

            User user = getUser(webSocket);
            if (!isValidUser(user)) return;

            switch (status) {

                case "connect": {
                    user.connect(object.getString("username"), object.getInt("account_id"), object.getInt("user_id"));
                    break;
                }
                case "connect_to_lobby": {
                    user.connectToLobby(object.getString("lobby_id"));
                    break;
                }
                case "host_lobby": {
                    user.createLobby();
                    break;
                }
                case "disconnect_lobby": {
                    user.disconnectFromLobby();
                    break;
                }
                case "toggle_pause_link": {
                    if(user.isHost){
                        user.isPauseLink = object.getBoolean("pause_link");
                    }
                    break;
                }
                case "kick": {
                    user.kickUser(object.getInt("account_id"));
                    break;
                }
                case "death": {
                    user.propagateDeaths();
                    break;
                }
                case "pause": {
                    user.propagatePause();
                    break;
                }
                case "unpause": {
                    user.propagateUnpause();
                    break;
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static boolean isValidUser(User user){
        return user != null
                && user.webSocket != null
                && user.webSocket.isOpen();
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        try {
            if (e instanceof BindException) {
                System.out.println("Retrying...");
                Thread.sleep(5000);
                Main.main(new String[]{});
            }

            User user = getUser(webSocket);
            if(isValidUser(user)){
                user.disconnect();
            }
        }
        catch (Exception f){
            f.printStackTrace();
        }
        e.printStackTrace();
    }

    @Override
    public void onStart() {
    }

    public static ConcurrentLinkedQueue<User> getUsers(){
        return users;
    }

    public static User getUser(WebSocket socket){
        for(User user : users){
            if(isValidUser(user) && user.webSocket.equals(socket)){
                return user;
            }
        }
        return null;
    }
}
