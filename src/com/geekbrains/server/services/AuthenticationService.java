package com.geekbrains.server.services;

import java.sql.SQLException;

public interface AuthenticationService {
    String getUsernameByLoginAndPassword(String login, String password) throws SQLException;

    void updateUsernameByLogin(String login, String newUsername) throws SQLException;
}
