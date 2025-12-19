package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.entities.Friend;
import com.codeduelz.codeduelz.entities.User;
import com.codeduelz.codeduelz.repo.FriendRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FriendService  {
    @Autowired
    private FriendRepo friendRepo;
    private void sendRequest(User from, User to){
        if(!friendRepo.existsByUserAndFriendUser(from,to)){
            Friend f= new Friend();
            f.setUser(from);
            f.setFriendUser(to);
            f.setStatus("PENDING");
            friendRepo.save(f);
        }
    }
}
