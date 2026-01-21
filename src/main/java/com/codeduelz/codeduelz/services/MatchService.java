package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.dtos.*;
import com.codeduelz.codeduelz.entities.User;

import java.util.List;

public interface MatchService {
    MatchDto createMatch(User user, CreateMatchDto dto);
    void completeMatch(Long matchId, MatchResultDto dto);
    List<MatchHistoryDto> getMatchHistory(User user);
    MatchProblemDto getMatchProblem(Long matchId);
}
