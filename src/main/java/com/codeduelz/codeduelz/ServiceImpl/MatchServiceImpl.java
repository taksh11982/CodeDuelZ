package com.codeduelz.codeduelz.ServiceImpl;

import com.codeduelz.codeduelz.entities.Match;
import com.codeduelz.codeduelz.repo.MatchRepo;
import com.codeduelz.codeduelz.services.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MatchServiceImpl implements MatchService {
    @Autowired
    private MatchRepo matchRepository;

}
