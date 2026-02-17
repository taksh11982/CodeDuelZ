package com.codeduelz.codeduelz.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublicProfileDto {
    private Long userId;
    private String userName;
    private Integer rating;
    private Integer totalMatches;
    private Integer wins;
    private Integer losses;
    private String bio;
    private String avatar;
    private String leetcodeUsername;
    private String codechefUsername;
    private String codeforcesHandle;

    // Calculated field: draws = total - wins - losses
    public Integer getDraws() {
        if (totalMatches == null || wins == null || losses == null) {
            return 0;
        }
        return totalMatches - wins - losses;
    }
}
