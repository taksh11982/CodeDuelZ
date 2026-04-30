package com.codeduelz.codeduelz.controller;

import com.codeduelz.codeduelz.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/public/users")
@RequiredArgsConstructor
public class SearchController {

    private final UserRepo userRepo;

    /**
     * Search for users by username (public endpoint, no auth required).
     * Returns at most 15 results to prevent data scraping.
     */
    @GetMapping("/search")
    public List<Map<String, Object>> searchUsers(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) {
            return List.of();
        }

        return userRepo.searchByUsername(q.trim()).stream()
                .limit(15)
                .map(u -> Map.<String, Object>of(
                        "userId", u.getUserId(),
                        "userName", u.getUsername(),
                        "isOnline", Boolean.TRUE.equals(u.getIsOnline())
                ))
                .collect(Collectors.toList());
    }
}
