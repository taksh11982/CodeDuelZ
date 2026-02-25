package com.codeduelz.codeduelz.controller;

import com.codeduelz.codeduelz.dtos.NotificationDto;
import com.codeduelz.codeduelz.entities.User;
import com.codeduelz.codeduelz.repo.UserRepo;
import com.codeduelz.codeduelz.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final UserRepo userRepo;

    private User resolveUser(UserDetails principal) {
        return userRepo.findByUserName(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public List<NotificationDto> getNotifications(@AuthenticationPrincipal UserDetails principal) {
        return notificationService.getNotifications(resolveUser(principal));
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(@AuthenticationPrincipal UserDetails principal) {
        return Map.of("count", notificationService.getUnreadCount(resolveUser(principal)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        notificationService.markAsRead(id, resolveUser(principal));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserDetails principal) {
        notificationService.markAllAsRead(resolveUser(principal));
        return ResponseEntity.ok().build();
    }
}
