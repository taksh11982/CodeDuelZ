package com.codeduelz.codeduelz.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendDto {
    private Long friendId;
    private Long userId;
    private String username;
    private String email;
    private Boolean isOnline;
}
