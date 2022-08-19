package com.geekbrains.server.services.impl;


import com.geekbrains.server.services.AuthenticationService;

import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;


public class SqlAuthenticationServiceImpl implements AuthenticationService {
    private static Logger log = Logger.getLogger("file");

    private static Connection connection;
    private static Statement stmt;

    private static void connection() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");

        connection = DriverManager.getConnection("jdbc:sqlite:src/resources/db/chatusers.db");

        stmt = connection.createStatement();
    }

    private static void disconnect() throws SQLException {
        connection.close();
    }

    public String getUsernameByLoginAndPassword(String login, String password) throws SQLException {
        try {
            connection();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            log.error("Ошибка подключения к БД!");
        }


        ResultSet rs = null;
        try {
            rs = stmt.executeQuery(String.format("SELECT * from chatusers WHERE login = '%s'", login));
        } catch (SQLException e) {
            e.printStackTrace();
            log.error("Ошибка запроса к БД!");
        }

        try {
            if (rs.isClosed()) {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String usernameDB = rs.getString("username");
        String passwordDB = rs.getString("password");
        log.info("Значение получено из ДБ - " + usernameDB);

        disconnect();

        return ((passwordDB != null) && (passwordDB.equals(password))) ? usernameDB : null;


    }

    public void updateUsernameByLogin(String login, String newUsername) throws SQLException {
        try {
            connection();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            log.error("Ошибка подключения к БД!");
        }

        stmt.executeUpdate(String.format("UPDATE chatusers SET username = '%s' WHERE login = '%s'", newUsername, login));

        disconnect();
    }

}
