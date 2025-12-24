package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.dtos.UserDto;
import com.codeduelz.codeduelz.entities.User;

public interface UserService {
    User register(User user);
    User findByEmail(String email);
    UserDto getUserProfile(Long userId);
}
