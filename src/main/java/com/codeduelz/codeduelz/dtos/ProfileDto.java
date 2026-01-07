package com.codeduelz.codeduelz.dtos;

import com.codeduelz.codeduelz.entities.User;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {
    private Long profileId;
    private User user;
    private String userName;
    private String email;
    private Integer rating=1000;
    private Integer losses=0;
    private Integer totalMatches=0;
    private Integer wins=0;
    private String bio;
    private String avatar;
    private String leetcodeUsername;
    private String codechefUsername;
    private String codeforcesHandle;
}
