import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
// Created by DenisPugaev
public class Server {
    private static final int SERVER_PORT = 8186;
    private static DataInputStream in;
    private static DataOutputStream out;
    private static Socket clientSocket;
    private static String message;

    public Server() {
    }

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);

            try {
                while (true) {
                    System.out.println("Ожидание сервера...");
                    clientSocket = serverSocket.accept();
                    System.out.println("Подключение установлено!");
                    in = new DataInputStream(clientSocket.getInputStream());
                    out = new DataOutputStream(clientSocket.getOutputStream());

                    Thread trIn = new Thread(() -> {
                        try {
                            while (true) {
                                message = in.readUTF();
                                System.out.println("Клиент: " + message);
                                if (message.equals("/end")) {
                                    System.out.println("Сервер остановлен");
                                    System.exit(0);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    trIn.setDaemon(true);
                    trIn.start();

                    outMessage();
                }
            } catch (IOException var14) {
                var14.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException var13) {
                    var13.printStackTrace();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static void outMessage() {
        Scanner console = new Scanner(System.in);
        System.out.println("Введите сообщение:");
        try {
            while (true) {
                message = console.next();
                out.writeUTF(message);
                System.out.println();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
