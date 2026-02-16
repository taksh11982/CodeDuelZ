package com.codeduelz.codeduelz.services;

public interface MatchmakingService {
    void joinQueue(String username, String difficulty);
    void leaveQueue(String username);
    void submitCode(String username, Long matchId, String code, String language);
}
