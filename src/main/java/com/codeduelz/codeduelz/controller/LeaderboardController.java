package com.codeduelz.codeduelz.controller;

import com.codeduelz.codeduelz.dtos.LeaderboardDto;
import com.codeduelz.codeduelz.services.ProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {
    private final ProfileService profileService;
    public LeaderboardController(ProfileService profileService) {
        this.profileService = profileService;
    }
    @GetMapping
    public List<LeaderboardDto> getLeaderboard() {
        return profileService.getLeaderboard();
    }
}
