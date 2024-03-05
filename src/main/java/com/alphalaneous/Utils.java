package com.alphalaneous;

import org.json.JSONObject;

import java.util.Random;

public class Utils {
    private static final Random random = new Random();
    private static final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

    public static String generateLobbyCode() {
        StringBuilder str = new StringBuilder();
        while (str.length() < 6) {
            int index = (int) (random.nextFloat() * characters.length());
            str.append(characters.charAt(index));
        }

        //check for collisions
        for(User user : ClientHandler.getUsers()){
            if(ClientHandler.isValidUser(user) && user.isHost && user.lobbyID.contentEquals(str)) {
                return generateLobbyCode();
            }
        }
        return str.toString();
    }

    public static void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    public static JSONObject tryJsonObject(String s){
        try{
            return new JSONObject(s);
        }
        catch (Exception e){
            return null;
        }
    }
}
