package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.dtos.NotificationDto;
import com.codeduelz.codeduelz.entities.NotificationType;
import com.codeduelz.codeduelz.entities.User;

import java.util.List;

public interface NotificationService {
    void create(User recipient, NotificationType type, String message, String fromUsername, Long referenceId);
    List<NotificationDto> getNotifications(User user);
    long getUnreadCount(User user);
    void markAsRead(Long notificationId, User user);
    void markAllAsRead(User user);
}
