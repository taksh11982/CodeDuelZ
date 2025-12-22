package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.entities.Match;
import com.codeduelz.codeduelz.repo.MatchRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MatchService {
    @Autowired
    private MatchRepo matchRepository;
    public Match createMatch(Match match) {
        match.setStatus("CREATED");
        return matchRepository.save(match);
    }

    public Match startMatch(Match match) {
        match.setStatus("RUNNING");
        match.setStartTime(LocalDateTime.now());
        return matchRepository.save(match);
    }

    public Match endMatch(Match match) {
        match.setStatus("COMPLETED");
        match.setEndTime(LocalDateTime.now());
        return matchRepository.save(match);
    }
}
