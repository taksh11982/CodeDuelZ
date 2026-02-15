package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.entities.*;
import com.codeduelz.codeduelz.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class MatchmakingService {
    private final SimpMessagingTemplate messaging;
    private final UserRepo userRepo;
    private final MatchRepo matchRepo;
    private final LeetCodeProblemService leetCodeProblemService;
    private final ProfileRepo profileRepo;

    // Queue per difficulty: difficulty -> list of waiting usernames
    private final Map<String, ConcurrentLinkedQueue<String>> queues = new ConcurrentHashMap<>();
    // Track which match each user is in: username -> matchId
    private final Map<String, Long> userToMatch = new ConcurrentHashMap<>();

    public void joinQueue(String username, String difficulty) {
        System.out.println("JOIN QUEUE: " + username + " for " + difficulty);
        ConcurrentLinkedQueue<String> queue = queues.computeIfAbsent(difficulty, k -> new ConcurrentLinkedQueue<>());
        if (queue.contains(username)) return;
        queue.add(username);
        System.out.println("QUEUE SIZE for " + difficulty + ": " + queue.size());
        tryMatch(difficulty);
    }

    public void leaveQueue(String username) {
        queues.values().forEach(q -> q.remove(username));
    }

    private void tryMatch(String difficulty) {
        ConcurrentLinkedQueue<String> queue = queues.get(difficulty);
        if (queue == null || queue.size() < 2) {
            System.out.println("NOT ENOUGH IN QUEUE: " + (queue == null ? 0 : queue.size()));
            return;
        }

        String username1 = queue.poll();
        String username2 = queue.poll();
        System.out.println("MATCHING: " + username1 + " vs " + username2);
        if (username1 == null || username2 == null) return;

        User player1 = userRepo.findByUserName(username1).orElse(null);
        User player2 = userRepo.findByUserName(username2).orElse(null);
        System.out.println("FOUND USERS: p1=" + (player1 != null) + ", p2=" + (player2 != null));
        if (player1 == null || player2 == null) return;

        // Pick a random LeetCode problem from local JSON files
        Difficulty diff = Difficulty.valueOf(difficulty);
        Map<String, Object> problemData = leetCodeProblemService.getRandomProblemData(diff);
        Problem problem = (Problem) problemData.get("problem");

        Match match = new Match();
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        match.setProblem(problem);
        match.setStatus(MatchStatus.ONGOING);
        match.setStartTime(LocalDateTime.now());
        match.setTimeLimitSeconds(900);
        Match saved = matchRepo.save(match);

        userToMatch.put(username1, saved.getMatchId());
        userToMatch.put(username2, saved.getMatchId());

        // Send structured problem data to both players
        Map<String, Object> problemPayload = new HashMap<>();
        problemPayload.put("title", problem.getTitle());
        problemPayload.put("description", problem.getDescription());
        problemPayload.put("difficulty", problem.getDifficulty() != null ? problem.getDifficulty().name() : "MEDIUM");
        problemPayload.put("url", "https://leetcode.com/problems/" + problem.getProblemSlug() + "/");
        problemPayload.put("examples", problemData.get("examples"));
        problemPayload.put("constraints", problemData.get("constraints"));
        problemPayload.put("codeSnippets", problemData.get("codeSnippets"));

        Map<String, Object> matchData = new HashMap<>();
        matchData.put("matchId", saved.getMatchId());
        matchData.put("problem", problemPayload);
        matchData.put("timeLimitSeconds", 900);
        matchData.put("startTimeMs", System.currentTimeMillis());
        matchData.put("player1", Map.of("name", username1));
        matchData.put("player2", Map.of("name", username2));

        System.out.println("SENDING MATCH: " + matchData);
        messaging.convertAndSend("/topic/user/" + username1, matchData);
        messaging.convertAndSend("/topic/user/" + username2, matchData);
    }

    public void submitCode(String username, Long matchId, String code, String language) {
        Match match = matchRepo.findById(matchId).orElse(null);
        if (match == null || match.getStatus() == MatchStatus.COMPLETED) return;

        User user = userRepo.findByUserName(username).orElse(null);
        if (user == null) return;

        // First to submit wins (mock judge)
        match.setWinnerId(user.getUserId());
        match.setStatus(MatchStatus.COMPLETED);
        match.setEndTime(LocalDateTime.now());
        
        boolean isPlayer1 = match.getPlayer1().getUserId().equals(user.getUserId());
        match.setPlayer1RatingChange(isPlayer1 ? 25 : -15);
        match.setPlayer2RatingChange(isPlayer1 ? -15 : 25);
        matchRepo.save(match);

        updateRatings(match);

        // Notify both players of result
        Map<String, Object> result = Map.of(
            "matchId", matchId,
            "winnerId", user.getUserId(),
            "winnerName", username
        );
        messaging.convertAndSend("/topic/match/" + matchId, result);

        userToMatch.remove(match.getPlayer1().getUsername());
        userToMatch.remove(match.getPlayer2().getUsername());
    }

    private void updateRatings(Match match) {
        var p1Profile = profileRepo.findByUser(match.getPlayer1()).orElse(null);
        var p2Profile = profileRepo.findByUser(match.getPlayer2()).orElse(null);
        
        if (p1Profile != null) {
            p1Profile.setRating(p1Profile.getRating() + match.getPlayer1RatingChange());
            p1Profile.setTotalMatches(p1Profile.getTotalMatches() + 1);
            if (match.getWinnerId().equals(match.getPlayer1().getUserId())) {
                p1Profile.setWins(p1Profile.getWins() + 1);
            } else {
                p1Profile.setLosses(p1Profile.getLosses() + 1);
            }
            profileRepo.save(p1Profile);
        }
        if (p2Profile != null) {
            p2Profile.setRating(p2Profile.getRating() + match.getPlayer2RatingChange());
            p2Profile.setTotalMatches(p2Profile.getTotalMatches() + 1);
            if (match.getWinnerId().equals(match.getPlayer2().getUserId())) {
                p2Profile.setWins(p2Profile.getWins() + 1);
            } else {
                p2Profile.setLosses(p2Profile.getLosses() + 1);
            }
            profileRepo.save(p2Profile);
        }
    }
}
