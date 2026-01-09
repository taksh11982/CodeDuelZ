package com.codeduelz.codeduelz.controller;

import com.codeduelz.codeduelz.dtos.CreateMatchDto;
import com.codeduelz.codeduelz.dtos.MatchDto;
import com.codeduelz.codeduelz.dtos.MatchHistoryDto;
import com.codeduelz.codeduelz.dtos.MatchResultDto;
import com.codeduelz.codeduelz.entities.User;
import com.codeduelz.codeduelz.services.MatchService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/matches")
public class MatchController {
    private MatchService matchService;
    @PostMapping
    public MatchDto createMatch(@AuthenticationPrincipal User user,@RequestBody CreateMatchDto dto) {
        return  matchService.createMatch(user, dto);
    }
    @PostMapping("/{matchId}/result")
    public void submitResult(@PathVariable Long matchId,@RequestBody MatchResultDto dto){
        matchService.completeMatch(matchId, dto);
    }
    @GetMapping("/history")
    public List<MatchHistoryDto> getMatchHistory(@AuthenticationPrincipal User user){
        return matchService.getMatchHistory(user);
    }

}
