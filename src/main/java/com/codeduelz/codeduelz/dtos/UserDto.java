package com.codeduelz.codeduelz.dtos;

import com.codeduelz.codeduelz.entities.AuthProvider;
import com.codeduelz.codeduelz.entities.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long userId;
    private String userName;
    private String email;
    private String password;
    private int rating = 0;
    private Role role;
    private AuthProvider provider;
    private LocalDateTime createdAt;
}
