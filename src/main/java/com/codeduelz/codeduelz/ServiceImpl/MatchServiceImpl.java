package com.codeduelz.codeduelz.ServiceImpl;

import com.codeduelz.codeduelz.dtos.*;
import com.codeduelz.codeduelz.entities.Match;
import com.codeduelz.codeduelz.entities.MatchStatus;
import com.codeduelz.codeduelz.entities.Problem;
import com.codeduelz.codeduelz.entities.User;
import com.codeduelz.codeduelz.repo.MatchRepo;
import com.codeduelz.codeduelz.repo.ProblemRepo;
import com.codeduelz.codeduelz.repo.ProfileRepo;
import com.codeduelz.codeduelz.repo.UserRepo;
import com.codeduelz.codeduelz.services.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MatchServiceImpl implements MatchService {
    @Autowired
    private MatchRepo matchRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private ProblemRepo problemRepo;
    @Autowired
    private ProfileRepo profileRepo;

    @Override
    public MatchDto createMatch(User user, CreateMatchDto dto) {
        User player1 = userRepo.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        User opponent = userRepo.findById(dto.getOpponentUserId())
                .orElseThrow(() -> new RuntimeException("Opponent not found"));

        Problem problem = problemRepo.findById(dto.getProblemId())
                .orElseThrow(() -> new RuntimeException("Problem not found"));
        Match match = new Match();
        match.setPlayer1(player1);
        match.setPlayer2(opponent);
        match.setProblem(problem);
        match.setStartTime(LocalDateTime.now());
        match.setStatus(MatchStatus.ONGOING);
        match = matchRepo.save(match);
        return mapToMatchDto(match);
    }

    @Override
    public void completeMatch(Long matchId, MatchResultDto dto) {
        Match match = matchRepo.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new RuntimeException("Match already completed");
        }
        User winner = userRepo.findById(dto.getWinnerUserId())
                .orElseThrow(() -> new RuntimeException("Winner not found"));
        match.setEndTime(LocalDateTime.now());
        match.setStatus(MatchStatus.COMPLETED);
        matchRepo.save(match);
        updateProfileStats(match.getPlayer1(), winner);
        updateProfileStats(match.getPlayer2(), winner);
    }

    @Override
    public List<MatchHistoryDto> getMatchHistory(User user) {
        List<Match> matches =matchRepo.findByPlayer1OrPlayer2(user, user);
        return matches.stream().map(match -> {

            User opponent = match.getPlayer1().getUserId().equals(user.getUserId())
                    ? match.getPlayer2()
                    : match.getPlayer1();

            MatchHistoryDto dto = new MatchHistoryDto();
            dto.setMatchId(match.getMatchId());
            dto.setOpponentId(opponent.getUserId());
            dto.setOpponentName(opponent.getUsername());
            dto.setProblemId(match.getProblem().getProblemId());
            dto.setProblemTitle(match.getProblem().getTitle());
            dto.setStatus(match.getStatus().name());

            if (match.getStatus() == MatchStatus.COMPLETED) {
                dto.setResult(
                        match.getEndTime() != null &&
                                match.getPlayer1().getUserId().equals(user.getUserId())
                                ? "WIN"
                                : "LOSS"
                );
            }
            dto.setStartTime(match.getStartTime());
            dto.setEndTime(match.getEndTime());
            return dto;
        }).toList();
    }

    private void updateProfileStats(User player, User winner) {
        var profile = profileRepo.findByUser(player)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        profile.setTotalMatches(profile.getTotalMatches() + 1);
        if (player.getUserId().equals(winner.getUserId())) {
            profile.setWins(profile.getWins() + 1);
            profile.setRating(profile.getRating() + 10);
        } else {
            profile.setLosses(profile.getLosses() + 1);
            profile.setRating(profile.getRating() - 5);
        }
        profileRepo.save(profile);
    }

    private MatchDto mapToMatchDto(Match match) {
        MatchDto dto = new MatchDto();
        dto.setMatchId(match.getMatchId());
        dto.setPlayer1Id(match.getPlayer1().getUserId());
        dto.setPlayer1Name(match.getPlayer1().getUsername());
        dto.setPlayer2Id(match.getPlayer2().getUserId());
        dto.setPlayer2Name(match.getPlayer2().getUsername());
        dto.setProblemId(match.getProblem().getProblemId());
        dto.setProblemTitle(match.getProblem().getTitle());
        dto.setStartTime(match.getStartTime());
        dto.setEndTime(match.getEndTime());
        dto.setStatus(match.getStatus().name());
        return dto;
    }

}
