package com.codeduelz.codeduelz.controller;

import com.codeduelz.codeduelz.services.MatchmakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class MatchmakingController {
    private final MatchmakingService matchmakingService;

    @MessageMapping("/queue/join")
    public void joinQueue(@Payload Map<String, String> payload) {
        String username = payload.get("username");
        String difficulty = payload.getOrDefault("difficulty", "EASY");
        matchmakingService.joinQueue(username, difficulty);
    }

    @MessageMapping("/queue/leave")
    public void leaveQueue(@Payload Map<String, String> payload) {
        String username = payload.get("username");
        matchmakingService.leaveQueue(username);
    }

    @MessageMapping("/match/run")
    public void runCode(@Payload Map<String, Object> payload) {
        String username = (String) payload.get("username");
        Long matchId = Long.parseLong(payload.get("matchId").toString());
        String code = (String) payload.get("code");
        String language = (String) payload.get("language");
        matchmakingService.runCode(username, matchId, code, language);
    }

    @MessageMapping("/match/submit")
    public void submitCode(@Payload Map<String, Object> payload) {
        String username = (String) payload.get("username");
        Long matchId = Long.parseLong(payload.get("matchId").toString());
        String code = (String) payload.get("code");
        String language = (String) payload.get("language");
        matchmakingService.submitCode(username, matchId, code, language);
    }
}
