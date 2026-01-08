package com.codeduelz.codeduelz.controller;

import com.codeduelz.codeduelz.dtos.ProfileDto;
import com.codeduelz.codeduelz.dtos.PublicProfileDto;
import com.codeduelz.codeduelz.dtos.UpdateProfileDto;
import com.codeduelz.codeduelz.entities.User;
import com.codeduelz.codeduelz.services.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
public class ProfileController {
    @Autowired
    private ProfileService profileService;
    @GetMapping
    public ProfileDto getProfile(@AuthenticationPrincipal User user){
        return profileService.getProfile(user);
    }
    @PutMapping
    public ProfileDto updateProfile(@AuthenticationPrincipal User user,@RequestBody UpdateProfileDto dto){
        return profileService.updateProfile(user,dto);
    }
    @GetMapping("/{userId}")
    public PublicProfileDto  getPublicProfile(@PathVariable Long userId){
        return  profileService.getPublicProfile(userId);
    }
}
