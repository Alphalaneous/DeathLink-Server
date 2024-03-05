package com.alphalaneous;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) {
        try {
            ClientHandler clientHandler = new ClientHandler();
            clientHandler.start();

            BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in));
            main: while (true) {
                String in = systemIn.readLine().toLowerCase();

                switch (in){

                    case "exit": {
                        clientHandler.stop(1000);
                        break main;
                    }

                    case "list_users": {
                        ClientHandler.getUsers().forEach(user -> {
                            if(ClientHandler.isValidUser(user)) {
                                System.out.println("Name: " + user.getName() + " | Host: " + user.isHost() + " | Lobby: " + user.getLobbyID() + " | Count: " + user.getLobbyUserCount());
                            }
                        });
                        break;
                    }

                    case "list_lobbies": {
                        ClientHandler.getUsers().forEach(user -> {

                            if(ClientHandler.isValidUser(user) && user.isHost()){
                                System.out.println("Lobby: " + user.getLobbyID() + " | Count: " + user.getLobbyUserCount());
                                System.out.print("    Users: [");

                                StringBuilder users = new StringBuilder();

                                ClientHandler.getUsers().forEach(user1 -> {
                                    if(ClientHandler.isValidUser(user1) && user.getLobbyID().equals(user1.getLobbyID())){
                                        users.append(user1.getName()).append(", ");
                                    }
                                });

                                String usersFinal = users.substring(0, users.length()-2);

                                System.out.print(usersFinal + "]");
                            }
                        });
                        break;
                    }
                }
            }
        }
        catch (Error | Exception e){
            System.out.println("Retrying...");
            Utils.sleep(5000);
            main(args);
        }
    }
}