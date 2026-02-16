package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.dtos.FriendDto;
import com.codeduelz.codeduelz.dtos.FriendRequestDto;
import com.codeduelz.codeduelz.entities.User;

import java.util.List;

public interface FriendService {
    void sendRequest(User from, User to);

    void acceptFriendRequest(Long requestId, User user);

    void rejectFriendRequest(Long requestId, User user);

    List<FriendDto> getFriends(User user);

    List<FriendRequestDto> getPendingRequests(User user);

    void removeFriend(Long friendId, User user);
}
