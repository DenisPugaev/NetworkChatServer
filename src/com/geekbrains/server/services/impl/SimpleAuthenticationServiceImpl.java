package com.geekbrains.server.services.impl;

import com.geekbrains.server.models.User;
import com.geekbrains.server.services.AuthenticationService;


import java.util.List;

public class SimpleAuthenticationServiceImpl implements AuthenticationService {

    private static final List<User> clients = List.of(
            new User("a", "1111", "mrDuck"),
            new User("b", "2222", "Cat"),
            new User("c", "3333", "Гендальф_Серый"),
            new User("d", "4444", "Super_Mario"),
            new User("e", "5555", "Bender"),
            new User("f", "6666", "Super_Sonic")
    );

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) {
        for (User client : clients) {
            if (client.getLogin().equals(login) && client.getPassword().equals(password)) {
                return client.getUsername();
            }
        }
        return null;
    }
}