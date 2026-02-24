package com.codeduelz.codeduelz.controller;

import com.codeduelz.codeduelz.entities.*;
import com.codeduelz.codeduelz.repo.*;
import com.codeduelz.codeduelz.services.LeetCodeProblemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Handles the friend-challenge flow over WebSocket.
 *
 * Send a challenge: /app/challenge/send { fromUsername, toUsername }
 * Respond: /app/challenge/respond { fromUsername, toUsername, accepted,
 * difficulty }
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChallengeController {

    private final SimpMessagingTemplate messaging;
    private final UserRepo userRepo;
    private final MatchRepo matchRepo;
    private final TestCaseRepo testCaseRepo;
    private final LeetCodeProblemService leetCodeProblemService;

    // ── Send challenge invite ────────────────────────────────────────────────

    @MessageMapping("/challenge/send")
    public void sendChallenge(@Payload Map<String, String> payload) {
        String fromUsername = payload.get("fromUsername");
        String toUsername = payload.get("toUsername");

        if (fromUsername == null || toUsername == null)
            return;

        log.info("Challenge: {} → {}", fromUsername, toUsername);

        Map<String, Object> invite = new HashMap<>();
        invite.put("type", "CHALLENGE_REQUEST");
        invite.put("fromUsername", fromUsername);

        // Deliver to the challenged user's personal challenge channel
        messaging.convertAndSend("/topic/user/" + toUsername + "/challenge", invite);
    }

    // ── Respond to challenge ─────────────────────────────────────────────────

    @MessageMapping("/challenge/respond")
    @Transactional
    public void respondChallenge(@Payload Map<String, String> payload) {
        String fromUsername = payload.get("fromUsername"); // original challenger
        String toUsername = payload.get("toUsername"); // responder (challenged user)
        boolean accepted = Boolean.parseBoolean(payload.get("accepted"));
        String difficulty = payload.getOrDefault("difficulty", "MEDIUM").toUpperCase();

        if (fromUsername == null || toUsername == null)
            return;

        if (!accepted) {
            log.info("Challenge declined: {} declined {}'s challenge", toUsername, fromUsername);

            Map<String, Object> declined = new HashMap<>();
            declined.put("type", "CHALLENGE_DECLINED");
            declined.put("byUsername", toUsername);

            // Notify the original challenger
            messaging.convertAndSend("/topic/user/" + fromUsername + "/challenge", declined);
            return;
        }

        log.info("Challenge accepted: {} vs {} at {}", fromUsername, toUsername, difficulty);

        User challenger = userRepo.findByUserName(fromUsername).orElse(null);
        User challenged = userRepo.findByUserName(toUsername).orElse(null);

        if (challenger == null || challenged == null) {
            log.error("Could not find users for challenge: {} vs {}", fromUsername, toUsername);
            return;
        }

        // Pick a random problem at the chosen difficulty
        Difficulty diff;
        try {
            diff = Difficulty.valueOf(difficulty);
        } catch (IllegalArgumentException e) {
            diff = Difficulty.MEDIUM;
        }

        Map<String, Object> problemData = leetCodeProblemService.getRandomProblemData(diff);
        Problem problem = (Problem) problemData.get("problem");

        // Create the match
        Match match = new Match();
        match.setPlayer1(challenger);
        match.setPlayer2(challenged);
        match.setProblem(problem);
        match.setStatus(MatchStatus.ONGOING);
        match.setStartTime(LocalDateTime.now());
        match.setTimeLimitSeconds(900);
        Match saved = matchRepo.save(match);

        // Build problem payload (same structure as normal matchmaking)
        Map<String, Object> problemPayload = new HashMap<>();
        problemPayload.put("title", problem.getTitle());
        problemPayload.put("description", problem.getDescription());
        problemPayload.put("difficulty", problem.getDifficulty() != null ? problem.getDifficulty().name() : "MEDIUM");
        problemPayload.put("url", "https://leetcode.com/problems/" + problem.getProblemSlug() + "/");
        problemPayload.put("examples", problemData.get("examples"));
        problemPayload.put("constraints", problemData.get("constraints"));
        problemPayload.put("codeSnippets", problemData.get("codeSnippets"));

        List<TestCase> testCases = testCaseRepo.findByProblem(problem);
        List<Map<String, String>> testCaseData = new ArrayList<>();
        for (TestCase tc : testCases) {
            Map<String, String> tcMap = new HashMap<>();
            tcMap.put("input", tc.getInput());
            tcMap.put("expectedOutput", tc.getExpectedOutput());
            testCaseData.add(tcMap);
        }
        problemPayload.put("testCases", testCaseData);

        Map<String, Object> matchMsg = new HashMap<>();
        matchMsg.put("matchId", saved.getMatchId());
        matchMsg.put("problem", problemPayload);
        matchMsg.put("timeLimitSeconds", 900);
        matchMsg.put("startTimeMs", System.currentTimeMillis());
        matchMsg.put("player1", Map.of("name", fromUsername));
        matchMsg.put("player2", Map.of("name", toUsername));

        // Deliver to both players on the same topic used by normal matchmaking
        messaging.convertAndSend("/topic/user/" + fromUsername, matchMsg);
        messaging.convertAndSend("/topic/user/" + toUsername, matchMsg);
    }
}
