package com.geekbrains.server.services.impl;

import com.geekbrains.server.models.User;
import com.geekbrains.server.services.AuthenticationService;


import java.util.List;

public class SimpleAuthenticationServiceImpl implements AuthenticationService {

    private static final List<User> clients = List.of(
            new User("duck", "1", "mrDuck"),
            new User("cat", "1", "Martin_Shatun"),
            new User("gus", "1", "Важный_Гусь"),
            new User("perec", "1", "Perchik"),
            new User("bender", "1", "Bender"),
            new User("pomidor", "1", "DonPomidor")
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

    @Override
    public void updateUsernameByLogin(String login, String newUsername) {
    }
}