package com.geekbrains.server.services;

public interface AuthenticationService{
    String getUsernameByLoginAndPassword(String login, String password);
}
