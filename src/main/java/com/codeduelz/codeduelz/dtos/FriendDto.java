package com.codeduelz.codeduelz.dtos;

import com.codeduelz.codeduelz.entities.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FriendDto {
    private Long id;
    private User user;
    private User friendUser;
    private String Status;
}
