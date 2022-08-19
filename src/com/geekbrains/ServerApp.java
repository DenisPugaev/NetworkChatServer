package com.geekbrains;


import com.geekbrains.server.MyServer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class ServerApp {

    private static final int DEFAULT_PORT = 8888;
    private static int port;

    private static String configsFile = "src/resources/configs/application-dev.properties";

    private static Logger log = Logger.getLogger("file");


    public static void main(String[] args) {


        PropertyConfigurator.configure("src/resources/logs/config/log4j.properties");


        Properties properties = new Properties();
        try {
            properties.load(new FileReader(configsFile));
        } catch (IOException e) {
            log.error("Ошибка получения файла с настройками!");

        }


        try {
            port = Integer.parseInt(properties.getProperty("server.port"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            log.error("Ошибка подключения к серверу!");
        }

        try {
            new MyServer(port).start();
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Ошибка запуска сервера!");
        }
    }
}
