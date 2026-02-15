package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.dtos.CodeExecutionResultDto;
import com.codeduelz.codeduelz.entities.*;
import com.codeduelz.codeduelz.repo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final CodeExecutionService codeExecutionService;
    private final TestCaseRepo testCaseRepo;
    private final SubmissionRepo submissionRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        // Include test cases in the match data for the Run button
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

        System.out.println("SENDING MATCH: " + matchData);
        messaging.convertAndSend("/topic/user/" + username1, matchData);
        messaging.convertAndSend("/topic/user/" + username2, matchData);
    }

    /**
     * Run code against example test cases (for the "Run" button).
     * Does NOT affect the match outcome. Just returns execution results.
     */
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

        // Run asynchronously so we don't block the WebSocket thread
        CompletableFuture.runAsync(() -> {
            CodeExecutionResultDto result = codeExecutionService.evaluateAgainstTestCases(code, language, testCases);
            sendRunResult(username, result);
        });
    }

    /**
     * Submit code for judging (for the "Submit" button).
     * Evaluates code against ALL test cases. If ALL pass, the user wins.
     */
    public void submitCode(String username, Long matchId, String code, String language) {
        Match match = matchRepo.findById(matchId).orElse(null);
        if (match == null || match.getStatus() == MatchStatus.COMPLETED) {
            sendSubmitResult(username, errorResult("Match not found or already completed"));
            return;
        }

        User user = userRepo.findByUserName(username).orElse(null);
        if (user == null) return;

        List<TestCase> testCases = testCaseRepo.findByProblem(match.getProblem());

        // Save the submission
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
            // Fallback: no test cases → first-to-submit wins (legacy behavior)
            submission.setStatus(SubmissionStatus.ACCEPTED);
            submission.setTestCasesPassed(0);
            submission.setTestCasesTotal(0);
            submissionRepo.save(submission);
            declareWinner(match, user, username);
            sendSubmitResult(username, new CodeExecutionResultDto(
                "ACCEPTED", List.of(), null, 0, 0
            ));
            return;
        }

        // Run code against test cases asynchronously
        CompletableFuture.runAsync(() -> {
            CodeExecutionResultDto result = codeExecutionService.evaluateAgainstTestCases(code, language, testCases);

            // Update submission with results
            submission.setTestCasesPassed(result.getTotalPassed());
            submission.setTestCasesTotal(result.getTotalTests());
            try {
                submission.setExecutionOutput(objectMapper.writeValueAsString(result));
            } catch (Exception ignored) {}

            if ("ACCEPTED".equals(result.getStatus())) {
                submission.setStatus(SubmissionStatus.ACCEPTED);
                submissionRepo.save(submission);

                // Re-check match is still ongoing (another player might have won while we were executing)
                Match freshMatch = matchRepo.findById(matchId).orElse(null);
                if (freshMatch != null && freshMatch.getStatus() == MatchStatus.ONGOING) {
                    declareWinner(freshMatch, user, username);
                }
            } else {
                // Map result status to SubmissionStatus
                switch (result.getStatus()) {
                    case "COMPILATION_ERROR" -> submission.setStatus(SubmissionStatus.COMPILATION_ERROR);
                    case "RUNTIME_ERROR" -> submission.setStatus(SubmissionStatus.RUNTIME_ERROR);
                    case "TIME_LIMIT_EXCEEDED" -> submission.setStatus(SubmissionStatus.TIME_LIMIT_EXCEEDED);
                    default -> submission.setStatus(SubmissionStatus.WRONG);
                }
                submissionRepo.save(submission);
            }

            // Send results back to the submitter
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

        // Notify both players of the match result
        Map<String, Object> result = Map.of(
            "matchId", match.getMatchId(),
            "winnerId", winner.getUserId(),
            "winnerName", winnerName
        );
        messaging.convertAndSend("/topic/match/" + match.getMatchId(), result);

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
}
