package com.codeduelz.codeduelz.controller;

import com.codeduelz.codeduelz.dtos.ExternalStatsDto;
import com.codeduelz.codeduelz.dtos.ProfileDto;
import com.codeduelz.codeduelz.entities.User;
import com.codeduelz.codeduelz.services.ExternalStatsService;
import com.codeduelz.codeduelz.services.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/external-stats")
@RequiredArgsConstructor
public class StatsController {
    private final ExternalStatsService statsService;
    private final ProfileService profileService;

    @GetMapping
    public ExternalStatsDto getStats(@AuthenticationPrincipal User user) {
        ProfileDto p = profileService.getProfile(user);
        return statsService.getUserStats(p.getLeetcodeUsername(), p.getCodeforcesHandle(), p.getCodechefUsername());
    }
}
