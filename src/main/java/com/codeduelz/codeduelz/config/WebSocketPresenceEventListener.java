package com.codeduelz.codeduelz.config;

import com.codeduelz.codeduelz.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for WebSocket session events to track user presence.
 * Marks users offline when their WebSocket session disconnects.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketPresenceEventListener {

    // Maps sessionId -> username so we know who disconnected
    public static final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    private final UserRepo userRepo;

    @EventListener
    @Transactional
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        String username = sessionUserMap.remove(sessionId);
        if (username != null) {
            log.info("User {} disconnected (session {}), marking offline", username, sessionId);
            userRepo.findByUserName(username).ifPresent(user -> {
                user.setIsOnline(false);
                user.setLastSeen(LocalDateTime.now());
                userRepo.save(user);
            });
        }
    }
}
