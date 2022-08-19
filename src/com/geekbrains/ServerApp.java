package com.geekbrains;


import com.geekbrains.server.MyServer;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class ServerApp {

    private static final int DEFAULT_PORT = 8888;
    private static int port;

    private static String configsFile = "src/resources/configs/application-dev.properties";


    public static void main(String[] args) {
        Properties properties = new Properties();
        try {
            properties.load(new FileReader(configsFile));
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            port = Integer.parseInt(properties.getProperty("server.port"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            System.out.println("Ошибка подключения к сереру!");
//            port = DEFAULT_PORT;
        }

        try {
            new MyServer(port).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
