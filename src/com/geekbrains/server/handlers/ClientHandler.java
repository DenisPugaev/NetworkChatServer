package com.geekbrains.server.handlers;

import com.geekbrains.server.MyServer;
import com.geekbrains.server.services.AuthenticationService;
import org.apache.log4j.Logger;



import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.*;


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
    private final MyServer myServer;
    private final Socket clientSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username = "Неавторизованный пользователь";
    private AuthenticationService auth;
    private String login;
    private String userNames;

    private static Logger log = Logger.getLogger("file");



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
        Callable<Boolean> authCaller = () -> {
            try {
                authentication();
                userNames = myServer.getUserNames();
                myServer.broadcastServerMessage(ClientHandler.this, USERS_UPDATE_PREFIX, userNames);
            } catch (IOException e) {
                try {
                    myServer.broadcastServerMessage(ClientHandler.this, "Пользователь " + username + " отключился от чата");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                log.error("Ошибка аутентификации!");
                e.printStackTrace();
            }
            return true;
        };
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> future = executor.submit(authCaller);
        try {
            future.get(120, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            future.cancel(true);
            try {
                closeConnection();
            } catch (IOException ex) {
                log.error("Ошибка при завершении соединения!");
            }
        }
        executor.shutdownNow();
        if (username == null) return;
        Runnable workRunner = () -> {
            try {
                readMessage();
            } catch (IOException e) {
                try {
                    myServer.broadcastServerMessage(ClientHandler.this, "Пользователь " + username + " отключился от чата");
                    log.info("Пользователь " + username + " отключился от чата");
                    myServer.unSubscribe(ClientHandler.this);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                e.printStackTrace();
            }
        };
        new Thread(workRunner).start();
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
                log.info("Неверная команда аутентификации");
            }
        }
    }


    public boolean processAuthentication(String message) throws IOException {
        String[] parts = message.split("\\s+");
        if (parts.length != 3) {
            out.writeUTF(AUTHERR_CMD_PREFIX + " Неверная команда аутентификации");
            log.info("Неверная команда аутентификации");
            return false;
        }

        login = parts[1];
        String password = parts[2];

        auth = myServer.getAuthenticationService();
        try {
            username = auth.getUsernameByLoginAndPassword(login, password);
            log.info("Имя пользователя которое пришло в CLIENT HANDLER -" + username);

        } catch (SQLException e) {
            log.error("Ошибка получения имени пользователя!");
            e.printStackTrace();
        }

        if (username != null) {
            if (myServer.isUsernameBusy(username)) {
                out.writeUTF(AUTHERR_CMD_PREFIX + " Логин уже используется");
                return false;
            }

            out.writeUTF(AUTHOK_CMD_PREFIX + " " + username);
            myServer.subscribe(this);

            System.out.println("Пользователь " + username + " подключился к чату");
            log.info("Пользователь " + username + " подключился к чату");

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
//            log.info("Сообщение с клиента: " + message);

            System.out.println("message | " + username + ": " + message);

            String typeMessage = message.split("\\s+")[0];
            if (!typeMessage.startsWith("/")) {
                log.info("Неверный запрос");
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
                    String[] messageParts = message.split("\\s+", 2);
                    String newNick = messageParts[1];
                    log.info("Новый ник - " + newNick);
                    this.username = newNick;
//                    log.info("new username - " + this.username);
                    try {
                        auth.updateUsernameByLogin(login, username);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        log.error("Ошибка подключения к БД!");
                    }
                }
                default -> log.info("Неверная команда");
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

}


