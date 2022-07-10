package com.geekbrains.server;

import com.geekbrains.server.handlers.ClientHandler;
import com.geekbrains.server.services.AuthenticationService;
import com.geekbrains.server.services.impl.SimpleAuthenticationServiceImpl;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class MyServer {

    private final ServerSocket serverSocket;
    private final AuthenticationService authenticationService;
    private final ArrayList<ClientHandler> clients;

    public MyServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        authenticationService = new SimpleAuthenticationServiceImpl();
        clients = new ArrayList<>();
    }


    public void start() {
        System.out.println("СЕРЕР СТАРТОВАЛ!");
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

    private void processClientConnection(Socket socket) throws IOException {
        ClientHandler handler = new ClientHandler(this, socket);
        handler.handle();
    }

    public synchronized void subscribe(ClientHandler handler) {
        clients.add(handler);
    }

    public synchronized void unSubscribe(ClientHandler handler) {
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
            if (client == sender) {
                continue;
            }
            client.sendMessage(sender.getUsername(), message);
        }
    }

    public synchronized void sendPrivateMessage(ClientHandler sender, String message, String nickTo) throws IOException {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(nickTo)) {
                c.sendPrivateMessage("От: " + sender.getUsername() + " Сообщение: " + message);
                sender.sendPrivateMessage("Пользователю: " + nickTo + " Сообщение: " + message);
                return;
            }
        }
        sender.sendMessage("Невозможно отправить сообщение пользователю: ", nickTo);
    }
}

