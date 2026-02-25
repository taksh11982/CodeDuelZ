package com.codeduelz.codeduelz.ServiceImpl;

import com.codeduelz.codeduelz.dtos.NotificationDto;
import com.codeduelz.codeduelz.entities.Notification;
import com.codeduelz.codeduelz.entities.NotificationType;
import com.codeduelz.codeduelz.entities.User;
import com.codeduelz.codeduelz.repo.NotificationRepo;
import com.codeduelz.codeduelz.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepo notificationRepo;
    private final SimpMessagingTemplate messaging;

    @Override
    public void create(User recipient, NotificationType type, String message, String fromUsername, Long referenceId) {
        Notification n = new Notification();
        n.setUser(recipient);
        n.setType(type);
        n.setMessage(message);
        n.setFromUsername(fromUsername);
        n.setReferenceId(referenceId);
        notificationRepo.save(n);

        // Push real-time update via WebSocket
        // Skip for challenge types — they already have their own dedicated popup channel (/challenge)
        if (type != NotificationType.CHALLENGE_RECEIVED && type != NotificationType.CHALLENGE_DECLINED) {
            messaging.convertAndSend("/topic/user/" + recipient.getUsername() + "/notifications",
                    Map.of("type", type.name(), "message", message, "fromUsername", fromUsername != null ? fromUsername : "",
                            "unreadCount", notificationRepo.countByUserAndIsReadFalse(recipient)));
        }
    }

    @Override
    public List<NotificationDto> getNotifications(User user) {
        return notificationRepo.findTop50ByUserOrderByCreatedAtDesc(user).stream()
                .map(n -> new NotificationDto(n.getId(), n.getType().name(), n.getMessage(),
                        n.getFromUsername(), n.getReferenceId(), n.getIsRead(), n.getCreatedAt()))
                .toList();
    }

    @Override
    public long getUnreadCount(User user) {
        return notificationRepo.countByUserAndIsReadFalse(user);
    }

    @Override
    public void markAsRead(Long notificationId, User user) {
        Notification n = notificationRepo.findById(notificationId).orElse(null);
        if (n != null && n.getUser().getUserId().equals(user.getUserId())) {
            n.setIsRead(true);
            notificationRepo.save(n);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(User user) {
        notificationRepo.markAllReadForUser(user);
    }
}
