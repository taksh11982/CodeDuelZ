package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.dtos.ExternalStatsDto;

public interface ExternalStatsService {
    ExternalStatsDto getUserStats(String leetcodeUsername, String codeforcesUsername, String codechefUsername);
}
