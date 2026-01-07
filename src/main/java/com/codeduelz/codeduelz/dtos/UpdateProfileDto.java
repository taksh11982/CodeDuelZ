package com.codeduelz.codeduelz.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileDto {
    private String bio;
    private String avatar;
    private String leetcodeUsername;
    private String codechefUsername;
    private String codeforcesHandle;
}
