package com.alphalaneous;

import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

public class User {

    public WebSocket webSocket;
    public String name;
    public int accountID;
    public int userID;
    public boolean isHost;
    public boolean isPauseLink;
    public String lobbyID;
    int requests = 0;

    public String getName() {
        return name;
    }

    public int getAccountID() {
        return accountID;
    }

    public int getUserID() {
        return userID;
    }

    public boolean isHost() {
        return isHost;
    }

    public User(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public void createLobby(){
        if(this.lobbyID == null) {
            this.isHost = true;
            this.lobbyID = Utils.generateLobbyCode();
            sendLobbyCreatedMessage();
        }
    }

    public static boolean isEqualLobby(String lobbyIDa, String lobbyIDb){
        if(lobbyIDa != null && lobbyIDb != null){
            return lobbyIDa.equals(lobbyIDb);
        }
        return false;
    }

    public String getLobbyID(){
        return lobbyID;
    }

    public int getLobbyUserCount(){

        int count = 0;

        for(User user : ClientHandler.getUsers()){
            if(ClientHandler.isValidUser(user)){
                if(this.lobbyID != null && isEqualLobby(user.lobbyID, this.lobbyID)){
                    count++;
                }
            }
        }
        return count;
    }

    public void propagateDeaths(){
        incrementDeathRequests();

        for(User user : ClientHandler.getUsers()){
            if(ClientHandler.isValidUser(user) && isEqualLobby(user.lobbyID, this.lobbyID) && this != user) {
                user.sendDeath();
            }
        }
    }

    public void propagatePause(){

        boolean isPauseLink = false;

        for(User user : ClientHandler.getUsers()){
            if(ClientHandler.isValidUser(user) && isEqualLobby(user.lobbyID, this.lobbyID) && user.isHost) {
                isPauseLink = user.isPauseLink;
                break;
            }
        }

        if(isPauseLink) {
            for (User user : ClientHandler.getUsers()) {
                if (ClientHandler.isValidUser(user) && isEqualLobby(user.lobbyID, this.lobbyID) && this != user) {
                    user.sendPause();
                }
            }
        }
    }

    public void propagateUnpause(){

        boolean isPauseLink = false;

        for(User user : ClientHandler.getUsers()){
            if(ClientHandler.isValidUser(user) && isEqualLobby(user.lobbyID, this.lobbyID) && user.isHost) {
                isPauseLink = user.isPauseLink;
                break;
            }
        }

        if(isPauseLink) {
            for (User user : ClientHandler.getUsers()) {
                if (ClientHandler.isValidUser(user) && isEqualLobby(user.lobbyID, this.lobbyID) && this != user) {
                    user.sendUnpause();
                }
            }
        }
    }

    public void disconnectFromLobby(){
        if(this.isHost){
            for(User user : ClientHandler.getUsers()){
                if(ClientHandler.isValidUser(user) && isEqualLobby(user.lobbyID, this.lobbyID) && !user.isHost) {
                    user.disconnectFromLobby();
                    user.sendDisconnectedFromLobbyMessage();
                }
            }
        }

        String tempLobbyID = lobbyID;

        this.isHost = false;
        this.lobbyID = null;

        sendLobbyMembersToEveryone(tempLobbyID);
    }

    public void kickUser(int accountID){
        if(this.isHost){
            for(User user : ClientHandler.getUsers()){
                if(ClientHandler.isValidUser(user) && user.accountID == accountID) {
                    user.sendKickedMessage();
                    user.disconnectFromLobby();
                }
            }
            sendLobbyMembersToEveryone(this.lobbyID);
        }
    }

    public void sendMessage(String message){
        if(webSocket != null && webSocket.isOpen()){
            webSocket.send(message);
        }
    }

    public void connectToLobby(String lobbyID){

        for(User user : ClientHandler.getUsers()){
            if(ClientHandler.isValidUser(user) && user.isHost && isEqualLobby(user.lobbyID, lobbyID)) {
                this.lobbyID = lobbyID;
                sendConnectedToLobbyMessage();
                sendLobbyMembersToEveryone(this.lobbyID);

                return;
            }
        }

        sendInvalidLobbyMessage();
    }

    public void connect(String name, int accountID, int userID){
        this.name = name;
        this.accountID = accountID;
        this.userID = userID;
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

    public JSONArray generateMemberList(){
        JSONArray members = new JSONArray();

        for(User user : ClientHandler.getUsers()){
            if(ClientHandler.isValidUser(user) && isEqualLobby(user.lobbyID, this.lobbyID)) {

                JSONObject memberData = new JSONObject();
                memberData.put("username", user.getName());
                memberData.put("account_id", user.getAccountID());
                memberData.put("user_id", user.getUserID());
                memberData.put("is_host", user.isHost);

                members.put(memberData);
            }
        }

        return members;
    }

    public void sendLobbyMembersToEveryone(String lobbyID){
        for(User user : ClientHandler.getUsers()){
            if(ClientHandler.isValidUser(user) && isEqualLobby(user.lobbyID, lobbyID)) {
                user.sendLobbyMembers();
            }
        }
    }

    public void sendLobbyMembers(){

        JSONObject object = new JSONObject();

        object.put("lobby", lobbyID);
        object.put("status", "member_list");

        JSONArray members = generateMemberList();

        object.put("members", members);

        sendMessage(object.toString(4));
    }

    public void sendDeath(){
        JSONObject object = new JSONObject();
        object.put("lobby", lobbyID);
        object.put("status", "death");

        sendMessage(object.toString(4));
    }

    public void sendPause(){
        JSONObject object = new JSONObject();
        object.put("lobby", lobbyID);
        object.put("status", "pause");

        sendMessage(object.toString(4));
    }

    public void sendUnpause(){
        JSONObject object = new JSONObject();
        object.put("lobby", lobbyID);
        object.put("status", "unpause");

        sendMessage(object.toString(4));
    }

    public void sendConnectedToLobbyMessage(){
        JSONObject object = new JSONObject();
        object.put("lobby", lobbyID);
        object.put("status", "connected");
        object.put("members", generateMemberList());
        sendMessage(object.toString(4));
    }

    public void sendInvalidLobbyMessage(){
        JSONObject object = new JSONObject();
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

    public void sendKickedMessage(){
        JSONObject object = new JSONObject();
        object.put("lobby", lobbyID);
        object.put("status", "kicked");

        sendMessage(object.toString(4));
    }

    public void sendLobbyCreatedMessage(){
        JSONObject object = new JSONObject();
        object.put("lobby", lobbyID);
        object.put("status", "created");
        object.put("members", generateMemberList());

        sendMessage(object.toString(4));
    }


}
