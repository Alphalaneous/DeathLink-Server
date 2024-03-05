package com.alphalaneous;

import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

public class User {

    public WebSocket webSocket;
    public String name;
    public boolean isHost;
    public String lobbyID;
    int requests = 0;

    public String getName() {
        return name;
    }

    public boolean isHost() {
        return isHost;
    }

    public User(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public void createLobby(){
        if(this.lobbyID != null) {
            this.isHost = true;
            this.lobbyID = Utils.generateLobbyCode();
            sendLobbyCreatedMessage();
        }
    }

    public String getLobbyID(){
        return lobbyID;
    }

    public int getLobbyUserCount(){
        return (int) ClientHandler.getUsers().stream().filter(user -> ClientHandler.isValidUser(user) && user.lobbyID.equals(this.lobbyID)).count();
    }

    public void propagateDeaths(){
        incrementDeathRequests();

        ClientHandler.getUsers().forEach(user -> {
            if(ClientHandler.isValidUser(user) && user.lobbyID.equals(this.lobbyID)) {
                user.sendDeath();
            }
        });
    }

    public void disconnectFromLobby(){
        if(this.isHost){
            ClientHandler.getUsers().forEach(user -> {
                if(ClientHandler.isValidUser(user) && user.lobbyID.equals(this.lobbyID)) {
                    user.disconnectFromLobby();
                    user.sendDisconnectedFromLobbyMessage();
                }
            });
        }

        this.isHost = false;
        this.lobbyID = null;
    }

    public void sendMessage(String message){
        if(webSocket != null && webSocket.isOpen()){
            webSocket.send(message);
        }
    }

    public void connectToLobby(String lobbyID){

        for(User user : ClientHandler.getUsers()){
            if(ClientHandler.isValidUser(user) && user.isHost && user.lobbyID.equals(this.lobbyID)) {
                this.lobbyID = lobbyID;
                sendConnectedToLobbyMessage();
                sendLobbyMembers();

                ClientHandler.getUsers().forEach(user1 -> {
                    if(ClientHandler.isValidUser(user) && user1.lobbyID.equals(this.lobbyID)) {
                        user1.sendLobbyMembers();
                    }
                });
                return;
            }
        }

        sendInvalidLobbyMessage();
    }

    public void connect(String name){
        this.name = name;
    }

    public void disconnect(){
        if(webSocket != null) {
            webSocket.close();
        }
        ClientHandler.getUsers().remove(this);
    }

    public void incrementDeathRequests(){
        requests++;
        new Thread(() -> {
            Utils.sleep(1000);
            requests--;
        }).start();
        if(requests > 20){
            System.out.println(getName() + " reached 20 requests in a second");
            if(ClientHandler.isValidUser(this)) {
                sendRateLimitedMessage();
                disconnect();
            }
            requests = 0;
        }
    }

    public void sendLobbyMembers(){

        JSONObject object = new JSONObject();

        object.put("lobby", lobbyID);
        object.put("status", "member_list");

        JSONArray members = new JSONArray();

        for(User user : ClientHandler.getUsers()){
            if(ClientHandler.isValidUser(user) && user.lobbyID.equals(this.lobbyID)) {
                members.put(user.getName());
            }
        }

        object.put("members", members);

        sendMessage(object.toString(4));
    }

    public void sendDeath(){
        JSONObject object = new JSONObject();
        object.put("lobby", lobbyID);
        object.put("status", "death");

        sendMessage(object.toString(4));
    }

    public void sendConnectedToLobbyMessage(){
        JSONObject object = new JSONObject();
        object.put("lobby", lobbyID);
        object.put("status", "connected");

        sendMessage(object.toString(4));
    }

    public void sendInvalidLobbyMessage(){
        JSONObject object = new JSONObject();
        object.put("lobby", lobbyID);
        object.put("status", "invalid");

        sendMessage(object.toString(4));
    }

    public void sendRateLimitedMessage(){
        JSONObject object = new JSONObject();
        object.put("lobby", lobbyID);
        object.put("status", "rate_limit");

        sendMessage(object.toString(4));
    }

    public void sendDisconnectedFromLobbyMessage(){
        JSONObject object = new JSONObject();
        object.put("lobby", lobbyID);
        object.put("status", "disconnected");

        sendMessage(object.toString(4));
    }

    public void sendLobbyCreatedMessage(){
        JSONObject object = new JSONObject();
        object.put("lobby", lobbyID);
        object.put("status", "created");

        sendMessage(object.toString(4));
    }


}
