package com.codeduelz.codeduelz.ServiceImpl;

import com.codeduelz.codeduelz.entities.Friend;
import com.codeduelz.codeduelz.entities.User;
import com.codeduelz.codeduelz.repo.FriendRepo;
import com.codeduelz.codeduelz.services.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FriendServiceImpl implements FriendService {
    @Autowired
    private FriendRepo friendRepo;

    @Override
    public void sendRequest(User from, User to) {
        if (from.getUserId().equals(to.getUserId())) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }
        boolean exists = friendRepo.existsByUserAndFriendUser(from, to);
        if (exists) {
            throw new IllegalStateException("Friend request already exists");
        }
        Friend request = new Friend();
        request.setUser(from);
        request.setFriendUser(to);
        request.setStatus("PENDING");
        Friend reverse = new Friend();
        reverse.setUser(to);
        reverse.setFriendUser(from);
        reverse.setStatus("PENDING");
        friendRepo.save(request);
        friendRepo.save(reverse);
    }
}
