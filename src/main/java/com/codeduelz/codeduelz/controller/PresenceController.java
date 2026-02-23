package com.codeduelz.codeduelz.controller;

import com.codeduelz.codeduelz.config.WebSocketPresenceEventListener;
import com.codeduelz.codeduelz.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Handles WebSocket presence messages.
 * When a client connects it sends /app/user/online with { "username": "..." }
 * to mark themselves as online.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PresenceController {

    private final UserRepo userRepo;

    @MessageMapping("/user/online")
    @Transactional
    public void userOnline(@Payload Map<String, String> payload,
            @Header("simpSessionId") String sessionId) {
        String username = payload.get("username");
        if (username == null || username.isBlank())
            return;

        // Track session -> username mapping so disconnect event can look it up
        WebSocketPresenceEventListener.sessionUserMap.put(sessionId, username);

        log.info("User {} connected (session {}), marking online", username, sessionId);

        userRepo.findByUserName(username).ifPresent(user -> {
            user.setIsOnline(true);
            user.setLastSeen(LocalDateTime.now());
            userRepo.save(user);
        });
    }
}
