package com.geekbrains.server;
//Created by DenisPugaev

import com.geekbrains.server.handlers.ClientHandler;
import com.geekbrains.server.services.AuthenticationService;
import com.geekbrains.server.services.impl.SqlAuthenticationServiceImpl;
import lombok.extern.slf4j.Slf4j;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

@Slf4j
public class MyServer {

    private final ServerSocket serverSocket;
    private final AuthenticationService authenticationService;
    private final ArrayList<ClientHandler> clients;

    public MyServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
//        authenticationService = new SimpleAuthenticationServiceImpl();
        authenticationService = new SqlAuthenticationServiceImpl();

        clients = new ArrayList<>();
    }


    public void start() {
        System.out.println("СЕРВЕР СТАРТОВАЛ!");
        System.out.println("________________");


        try {
            while (true) {
                waitAndProcessNewClientConnection();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void waitAndProcessNewClientConnection() throws IOException {
        System.out.println("Идет ожидание клиента...");
        Socket socket = serverSocket.accept();
        System.out.println("Клиент произвел подключение!");

        processClientConnection(socket);
    }

    private void processClientConnection(Socket socket) {
        ClientHandler handler = new ClientHandler(this, socket);
        handler.handle();
    }

    public synchronized void subscribe(ClientHandler handler) {
        clients.add(handler);

    }


    public synchronized void unSubscribe(ClientHandler handler) throws IOException {
        clients.remove(handler);
    }

    public AuthenticationService getAuthenticationService() {

        return authenticationService;
    }

    public boolean isUsernameBusy(String username) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public void stop() {
        System.out.println("------------------");
        System.out.println("------------------");
        System.out.println("СЕРВЕР ЗАВЕРШИЛ РАБОТУ!");
        System.exit(0);
    }


    public void broadcastMessage(ClientHandler sender, String message) throws IOException {
        for (ClientHandler client : clients) {
          /*  if (client == sender) {
                continue;
            }*/
            client.sendMessage(sender.getUsername(), message);

        }
    }

    public synchronized void sendPrivateMessage(ClientHandler sender, String recipient, String privateMessage) throws IOException {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(recipient)) {
                client.sendMessage(sender.getUsername(), privateMessage, true);
            }
        }
    }

    public synchronized void broadcastServerMessage(ClientHandler sender, String message) throws IOException {
        for (ClientHandler client : clients) {
            if (client == sender) {
                continue;
            }
            client.sendServerMessage(message);
        }
    }

    public synchronized void broadcastServerMessage(ClientHandler sender, String prefix, String message) throws IOException {
        for (ClientHandler client : clients) {
            client.sendServerMessage(prefix, message);
            System.out.println(prefix + " " + message);
        }
    }

    public synchronized String getUserNames() {
        StringBuilder builder = new StringBuilder();
        for (ClientHandler client : clients) {
            builder.append(client.getUsername());
            builder.append(" ");
        }
        return builder.toString();
    }

}

