package com.codeduelz.codeduelz.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String type;
    private String message;
    private String fromUsername;
    private Long referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
