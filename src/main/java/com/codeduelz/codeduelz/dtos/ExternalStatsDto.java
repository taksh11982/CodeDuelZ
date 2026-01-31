package com.codeduelz.codeduelz.dtos;

import lombok.Data;
import java.util.Map;

@Data
public class ExternalStatsDto {
    private LeetCodeStats leetCode;
    private CodeforcesStats codeforces;
    private CodeChefStats codeChef;

    @Data
    public static class LeetCodeStats {
        private int totalSolved;
        private int easySolved;
        private int mediumSolved;
        private int hardSolved;
        private int ranking;
    }

    @Data
    public static class CodeforcesStats {
        private int rating;
        private int maxRating;
        private String rank;
        private int contribution;
    }

    @Data
    public static class CodeChefStats {
        private int currentRating;
        private int highestRating;
        private String stars;
        private int globalRank;
        private int countryRank;
    }
}
