package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.entities.User;
import com.codeduelz.codeduelz.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class UserService {
    @Autowired
    private PasswordEncoder passwordEncoder;
    private UserRepo userRepo;
    public User register(User user){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepo.save(user);
    }
}
