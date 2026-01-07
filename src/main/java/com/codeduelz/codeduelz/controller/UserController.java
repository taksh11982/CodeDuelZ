package com.codeduelz.codeduelz.controller;

import com.codeduelz.codeduelz.entities.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {
    @GetMapping("/profile")
    public User profile(@AuthenticationPrincipal User user){
        return user;
    }
}
