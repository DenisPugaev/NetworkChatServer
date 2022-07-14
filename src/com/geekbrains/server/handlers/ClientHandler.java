package com.geekbrains.server.handlers;


import com.geekbrains.server.MyServer;
import com.geekbrains.server.services.AuthenticationService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ClientHandler {
    private static final String AUTH_CMD_PREFIX = "/auth"; // + login + password
    private static final String AUTHOK_CMD_PREFIX = "/authok"; // + username
    private static final String AUTHERR_CMD_PREFIX = "/autherr"; // + error message
    private static final String CLIENT_MSG_CMD_PREFIX = "/cMsg"; // + msg
    private static final String SERVER_MSG_CMD_PREFIX = "/sMsg"; // + msg
    private static final String PRIVATE_MSG_CMD_PREFIX = "/pm"; // + username + msg
    private static final String STOP_SERVER_CMD_PREFIX = "/stop";
    private static final String END_CLIENT_CMD_PREFIX = "/end";

    private static final String USERS_UPDATE_PREFIX = "/updateUsers";
    private MyServer myServer;
    private Socket clientSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username = "";


    public ClientHandler(MyServer myServer, Socket socket) {

        this.myServer = myServer;
        clientSocket = socket;
        try {
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void handle() {

        new Thread(() -> {
            try {

                authentication();
                String userNames = myServer.getUserNames();
                myServer.broadcastServerMessage(this, USERS_UPDATE_PREFIX, userNames);
                readMessage();
            } catch (IOException e) {

                try {
                    myServer.broadcastServerMessage(this, "Пользователь " + username + " отключился от чата");
                    myServer.unSubscribe(this);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                e.printStackTrace();
            } catch (InterruptedException e){
                e.printStackTrace();
                System.out.println("Неавторизованный пользователь отключен по таймауту");
            }
        }).start();
    }

    public void authentication() throws IOException, InterruptedException {
        /*Засечь отсчет времени отсюда и остановить в цикле через 120 сек*/
        long startTime = System.currentTimeMillis();
        while (true) {

            String message = in.readUTF();


            if (message.startsWith(AUTH_CMD_PREFIX)) {
                boolean isSuccessAuth = processAuthentication(message);
                if (isSuccessAuth) {
                    break;
                }
            }
            if (!message.startsWith(AUTH_CMD_PREFIX)) {
                out.writeUTF(AUTHERR_CMD_PREFIX + " Неверная команда аутентификации");
                System.out.println("Неверная команда аутентификации");
            }
            long diff = System.currentTimeMillis()-startTime;
            if(diff> 12_000L){ // не сработал добавить в другое место!
                closeConnection();
            }
           throw new InterruptedException();
        }
    }


    public boolean processAuthentication(String message) throws IOException {
        String[] parts = message.split("\\s+");
        if (parts.length != 3) {
            out.writeUTF(AUTHERR_CMD_PREFIX + " Неверная команда аутентификации");
            System.out.println("Неверная команда аутентификации");
            return false;
        }

        String login = parts[1];
        String password = parts[2];

        AuthenticationService auth = myServer.getAuthenticationService();

        username = auth.getUsernameByLoginAndPassword(login, password);

        if (username != null) {
            if (myServer.isUsernameBusy(username)) {
                out.writeUTF(AUTHERR_CMD_PREFIX + " Логин уже используется");
                return false;
            }

            out.writeUTF(AUTHOK_CMD_PREFIX + " " + username);
            myServer.subscribe(this);

            System.out.println("Пользователь " + username + " подключился к чату");

            myServer.broadcastServerMessage(this, "Пользователь " + username + " подключился к чату");

            return true;
        } else {
            out.writeUTF(AUTHERR_CMD_PREFIX + " Неверная комбинация логина и пароля");
            return false;
        }
    }

    public void readMessage() throws IOException {

        while (true) {
            String message = in.readUTF();


            System.out.println("message | " + username + ": " + message);

            String typeMessage = message.split("\\s+")[0];
            if (!typeMessage.startsWith("/")) {
                System.out.println("Неверный запрос");
            }


            switch (typeMessage) {
                case STOP_SERVER_CMD_PREFIX -> myServer.stop();
                case END_CLIENT_CMD_PREFIX -> closeConnection();
                case CLIENT_MSG_CMD_PREFIX -> {
                    String[] messageParts = message.split("\\s+", 2);
                    myServer.broadcastMessage(this, messageParts[1]);
                }
                case PRIVATE_MSG_CMD_PREFIX -> {
                    String[] privateMessageParts = message.split("\\s+", 3);
                    String recipient = privateMessageParts[1];
                    String privateMessage = privateMessageParts[2];

                    myServer.sendPrivateMessage(this, recipient, privateMessage);
                }
                case SERVER_MSG_CMD_PREFIX -> {
//                    String[] messageParts = message.split("\\s+", 2);
//                    String usernameUpdate = messageParts[2];
//                    myServer.broadcastServerMessage( this,usernameUpdate);
//                    //??????
                }
                default -> System.out.println("Неверная команда");
            }

        }
    }

    public void closeConnection() throws IOException {
        clientSocket.close();
        System.out.println(username + " отключился");
    }

    public void sendServerMessage(String message) throws IOException {
        out.writeUTF(String.format("%s %s", SERVER_MSG_CMD_PREFIX, message));
    }

    public void sendServerMessage(String prefix, String message) throws IOException {
        out.writeUTF(String.format("%s %s", prefix, message));
    }

    public void sendMessage(String sender, String message, Boolean isPrivate) throws IOException {
        out.writeUTF(String.format("%s %s %s", isPrivate ?
                        PRIVATE_MSG_CMD_PREFIX
                        : CLIENT_MSG_CMD_PREFIX,
                sender, message));
    }

    public void sendMessage(String sender, String message) throws IOException {
        sendMessage(sender, message, false);
    }

    public String getUsername() {
        return username;
    }


    public void waitExit() throws IOException {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

        service.schedule(new Runnable() {
            @Override
            public void run() {

            }
        }, 120, TimeUnit.SECONDS);

    }
}


