package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.dtos.UserDto;
import com.codeduelz.codeduelz.entities.User;
import com.google.firebase.auth.FirebaseToken;

public interface UserService {
    User register(User user);
    User findByEmail(String email);
    UserDto getUserProfile(Long userId);
    User findOrCreateFirebaseUser(FirebaseToken token);
}
