package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.entities.User;

public interface FriendService {
    void sendRequest(User from, User to);
}
