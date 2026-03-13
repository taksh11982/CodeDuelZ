package com.codeduelz.codeduelz.ServiceImpl;

import com.codeduelz.codeduelz.dtos.CodeExecutionResultDto;
import com.codeduelz.codeduelz.entities.*;
import com.codeduelz.codeduelz.repo.*;
import com.codeduelz.codeduelz.services.CodeExecutionService;
import com.codeduelz.codeduelz.services.LeetCodeProblemService;
import com.codeduelz.codeduelz.services.MatchmakingService;
import com.codeduelz.codeduelz.services.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchmakingServiceImpl implements MatchmakingService {
    private final SimpMessagingTemplate messaging;
    private final UserRepo userRepo;
    private final MatchRepo matchRepo;
    private final LeetCodeProblemService leetCodeProblemService;
    private final ProfileRepo profileRepo;
    private final CodeExecutionService codeExecutionService;
    private final TestCaseRepo testCaseRepo;
    private final SubmissionRepo submissionRepo;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, ConcurrentLinkedQueue<String>> queues = new ConcurrentHashMap<>();
    private final Map<String, Long> userToMatch = new ConcurrentHashMap<>();

    @Override
    public void joinQueue(String username, String difficulty) {
        log.info("JOIN QUEUE: {} for {}", username, difficulty);
        ConcurrentLinkedQueue<String> queue = queues.computeIfAbsent(difficulty, k -> new ConcurrentLinkedQueue<>());
        if (queue.contains(username))
            return;
        queue.add(username);
        log.info("QUEUE SIZE for {}: {}", difficulty, queue.size());
        tryMatch(difficulty);
    }

    @Override
    public void leaveQueue(String username) {
        queues.values().forEach(q -> q.remove(username));
    }

    private void tryMatch(String difficulty) {
        ConcurrentLinkedQueue<String> queue = queues.get(difficulty);
        if (queue == null || queue.size() < 2) {
            log.info("NOT ENOUGH IN QUEUE: {}", (queue == null ? 0 : queue.size()));
            return;
        }

        String username1 = queue.poll();
        String username2 = queue.poll();
        log.info("MATCHING: {} vs {}", username1, username2);
        if (username1 == null || username2 == null)
            return;

        User player1 = userRepo.findByUserName(username1).orElse(null);
        User player2 = userRepo.findByUserName(username2).orElse(null);
        log.info("FOUND USERS: p1={}, p2={}", (player1 != null), (player2 != null));
        if (player1 == null || player2 == null)
            return;

        try {
            Difficulty diff;
            try {
                diff = Difficulty.valueOf(difficulty.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid difficulty '{}', defaulting to MEDIUM", difficulty);
                diff = Difficulty.MEDIUM;
            }

            Map<String, Object> problemData = leetCodeProblemService.getRandomProblemData(diff);
            Problem problem = (Problem) problemData.get("problem");
            if (problem == null) {
                throw new IllegalStateException("Problem data was loaded but problem entity is null");
            }

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

            Map<String, Object> matchData = new HashMap<>();
            matchData.put("matchId", saved.getMatchId());
            matchData.put("problem", problemPayload);
            matchData.put("timeLimitSeconds", 900);
            matchData.put("startTimeMs", System.currentTimeMillis());
            matchData.put("player1", Map.of("name", username1));
            matchData.put("player2", Map.of("name", username2));

            log.info("SENDING MATCH: {}", matchData);
            messaging.convertAndSend("/topic/user/" + username1, matchData);
            messaging.convertAndSend("/topic/user/" + username2, matchData);
        } catch (Exception ex) {
            log.error("Failed to create match for users '{}' and '{}' on difficulty '{}': {}",
                    username1, username2, difficulty, ex.getMessage(), ex);
            queue.offer(username1);
            queue.offer(username2);
        }
    }

    @Override
    public void runCode(String username, Long matchId, String code, String language) {
        Match match = matchRepo.findById(matchId).orElse(null);
        if (match == null) {
            sendRunResult(username, errorResult("Match not found"));
            return;
        }

        List<TestCase> testCases = testCaseRepo.findByProblem(match.getProblem());
        if (testCases.isEmpty()) {
            sendRunResult(username, errorResult("No test cases available for this problem"));
            return;
        }

        CompletableFuture.runAsync(() -> {
            String methodName = match.getProblem().getMethodName();
            CodeExecutionResultDto result = codeExecutionService.evaluateAgainstTestCases(code, language, testCases,
                    methodName);
            sendRunResult(username, result);
        });
    }

    @Override
    public void submitCode(String username, Long matchId, String code, String language) {
        Match match = matchRepo.findById(matchId).orElse(null);
        if (match == null || match.getStatus() == MatchStatus.COMPLETED) {
            sendSubmitResult(username, errorResult("Match not found or already completed"));
            return;
        }

        User user = userRepo.findByUserName(username).orElse(null);
        if (user == null)
            return;

        List<TestCase> testCases = testCaseRepo.findByProblem(match.getProblem());

        Submission submission = new Submission();
        submission.setUser(user);
        submission.setMatch(match);
        submission.setProblem(match.getProblem());
        submission.setCode(code);
        submission.setLanguage(language);
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setSubmittedAt(LocalDateTime.now());
        submissionRepo.save(submission);

        if (testCases.isEmpty()) {
            submission.setStatus(SubmissionStatus.ACCEPTED);
            submission.setTestCasesPassed(0);
            submission.setTestCasesTotal(0);
            submissionRepo.save(submission);
            declareWinner(match, user, username);
            sendSubmitResult(username, new CodeExecutionResultDto(
                    "ACCEPTED", List.of(), null, 0, 0));
            return;
        }

        CompletableFuture.runAsync(() -> {
            String methodName = match.getProblem().getMethodName();
            CodeExecutionResultDto result = codeExecutionService.evaluateAgainstTestCases(code, language, testCases,
                    methodName);

            submission.setTestCasesPassed(result.getTotalPassed());
            submission.setTestCasesTotal(result.getTotalTests());
            try {
                submission.setExecutionOutput(objectMapper.writeValueAsString(result));
            } catch (Exception ignored) {
            }

            if ("ACCEPTED".equals(result.getStatus())) {
                submission.setStatus(SubmissionStatus.ACCEPTED);
                submissionRepo.save(submission);

                Match freshMatch = matchRepo.findById(matchId).orElse(null);
                if (freshMatch != null && freshMatch.getStatus() == MatchStatus.ONGOING) {
                    declareWinner(freshMatch, user, username);
                }
            } else {
                switch (result.getStatus()) {
                    case "COMPILATION_ERROR" -> submission.setStatus(SubmissionStatus.COMPILATION_ERROR);
                    case "RUNTIME_ERROR" -> submission.setStatus(SubmissionStatus.RUNTIME_ERROR);
                    case "TIME_LIMIT_EXCEEDED" -> submission.setStatus(SubmissionStatus.TIME_LIMIT_EXCEEDED);
                    default -> submission.setStatus(SubmissionStatus.WRONG);
                }
                submissionRepo.save(submission);
            }

            sendSubmitResult(username, result);
        });
    }

    private void declareWinner(Match match, User winner, String winnerName) {
        match.setWinnerId(winner.getUserId());
        match.setStatus(MatchStatus.COMPLETED);
        match.setEndTime(LocalDateTime.now());

        boolean isPlayer1 = match.getPlayer1().getUserId().equals(winner.getUserId());
        match.setPlayer1RatingChange(isPlayer1 ? 25 : -15);
        match.setPlayer2RatingChange(isPlayer1 ? -15 : 25);
        matchRepo.save(match);

        updateRatings(match);

        Map<String, Object> result = Map.of(
                "matchId", match.getMatchId(),
                "winnerId", winner.getUserId(),
                "winnerName", winnerName);
        messaging.convertAndSend("/topic/match/" + match.getMatchId(), result);

        User loser = isPlayer1 ? match.getPlayer2() : match.getPlayer1();
        notificationService.create(winner, NotificationType.MATCH_RESULT,
                "You won against " + loser.getUsername() + "! +25 ELO", loser.getUsername(), match.getMatchId());
        notificationService.create(loser, NotificationType.MATCH_RESULT,
                "You lost to " + winner.getUsername() + ". -15 ELO", winner.getUsername(), match.getMatchId());

        userToMatch.remove(match.getPlayer1().getUsername());
        userToMatch.remove(match.getPlayer2().getUsername());
    }

    private void sendRunResult(String username, CodeExecutionResultDto result) {
        messaging.convertAndSend("/topic/user/" + username + "/run-result", result);
    }

    private void sendSubmitResult(String username, CodeExecutionResultDto result) {
        messaging.convertAndSend("/topic/user/" + username + "/submit-result", result);
    }

    private CodeExecutionResultDto errorResult(String message) {
        return new CodeExecutionResultDto(message, List.of(), null, 0, 0);
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

    @Override
    public void handleTimeout(Long matchId) {
        Match match = matchRepo.findById(matchId).orElse(null);
        if (match == null || match.getStatus() == MatchStatus.COMPLETED) return;

        match.setStatus(MatchStatus.COMPLETED);
        match.setEndTime(LocalDateTime.now());
        match.setWinnerId(null);
        match.setPlayer1RatingChange(-10);
        match.setPlayer2RatingChange(-10);
        matchRepo.save(match);

        for (User player : List.of(match.getPlayer1(), match.getPlayer2())) {
            profileRepo.findByUser(player).ifPresent(p -> {
                p.setTotalMatches(p.getTotalMatches() + 1);
                p.setLosses(p.getLosses() + 1);
                p.setRating(p.getRating() - 10);
                profileRepo.save(p);
            });
        }

        messaging.convertAndSend("/topic/match/" + matchId,
                Map.of("matchId", matchId, "winnerId", "TIMEOUT", "winnerName", "TIMEOUT"));

        notificationService.create(match.getPlayer1(), NotificationType.MATCH_RESULT,
                "Match timed out. -10 ELO", match.getPlayer2().getUsername(), matchId);
        notificationService.create(match.getPlayer2(), NotificationType.MATCH_RESULT,
                "Match timed out. -10 ELO", match.getPlayer1().getUsername(), matchId);

        userToMatch.remove(match.getPlayer1().getUsername());
        userToMatch.remove(match.getPlayer2().getUsername());
    }
}
