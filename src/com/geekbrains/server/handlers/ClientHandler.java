package com.geekbrains.server.handlers;


import com.geekbrains.server.MyServer;
import com.geekbrains.server.services.AuthenticationService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class ClientHandler {
    private static final String AUTH_CMD_PREFIX = "/auth"; // + login + password
    private static final String AUTHOK_CMD_PREFIX = "/authok"; // + username
    private static final String AUTHERR_CMD_PREFIX = "/autherr"; // + error message
    private static final String CLIENT_MSG_CMD_PREFIX = "/cMsg"; // + msg
    private static final String SERVER_MSG_CMD_PREFIX = "/sMsg"; // + msg
    private static final String PRIVATE_MSG_CMD_PREFIX = "/pm"; // + username + msg
    private static final String STOP_SERVER_CMD_PREFIX = "/stop";
    private static final String END_CLIENT_CMD_PREFIX = "/end";
    private MyServer myServer;
    private Socket clientSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    public ClientHandler(MyServer myServer, Socket socket) {

        this.myServer = myServer;
        clientSocket = socket;
    }


    public void handle() throws IOException {
        in = new DataInputStream(clientSocket.getInputStream());
        out = new DataOutputStream(clientSocket.getOutputStream());

        new Thread(() -> {
            try {
                authentication();
                readMessage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void authentication() throws IOException {
        while (true) {
            String message = in.readUTF();

            if (message.startsWith(AUTH_CMD_PREFIX)) {
                boolean isSuccessAuth = processAuthentication(message);
                if (isSuccessAuth) {
                    break;
                }
            } else {
                out.writeUTF(AUTHERR_CMD_PREFIX + " Неверная команда аутентификации");
                System.out.println("Неверная команда аутентификации");
            }

        }
    }

    private boolean processAuthentication(String message) throws IOException {
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

            out.writeUTF(AUTHOK_CMD_PREFIX + " " + username + " авторизировался!");
            myServer.subscribe(this);
            System.out.println("Пользователь " + username + " подключился к чату");
            return true;
        } else {
            out.writeUTF(AUTHERR_CMD_PREFIX + " Неправильный логин и пароль");
            return false;
        }
    }

    private void readMessage() throws IOException {
        while (true) {

            String message = in.readUTF();
            System.out.println(username + " написал: " + message);
            String typeMessage = message.split("\\s+")[0];

            switch (typeMessage) {
                case STOP_SERVER_CMD_PREFIX -> myServer.stop();
                case END_CLIENT_CMD_PREFIX -> closeConnection();
                case PRIVATE_MSG_CMD_PREFIX -> privateMessage(message);

                default -> myServer.broadcastMessage(this, message);
            }


        }
    }

    private synchronized void privateMessage(String messageAndNick) throws IOException {
        String fullMessage = messageAndNick.trim();
        int indexOfFirstSpace = fullMessage.indexOf(" ");
        String prefix = fullMessage.substring(0, indexOfFirstSpace);
        int indexOfSecondSpace = fullMessage.indexOf(" ", indexOfFirstSpace + 1);
        String nickTo = fullMessage.substring(indexOfFirstSpace + 1, indexOfSecondSpace);
        String onlyMessage = fullMessage.substring(indexOfSecondSpace + 1);
        myServer.sendPrivateMessage(this, onlyMessage, nickTo);
    }


    private void closeConnection() throws IOException {
        clientSocket.close();
        System.out.println(username + " отключился");
        myServer.unSubscribe(this);
    }

    public synchronized void sendMessage(String sender, String message) throws IOException {
        out.writeUTF(sender + " " + message);
    }

    public synchronized void sendPrivateMessage(String message) throws IOException {
        out.writeUTF(message);
    }

    public String getUsername() {
        return username;
    }
}
