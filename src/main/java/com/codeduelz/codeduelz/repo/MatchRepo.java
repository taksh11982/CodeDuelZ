package com.codeduelz.codeduelz.repo;

import com.codeduelz.codeduelz.entities.Match;
import com.codeduelz.codeduelz.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRepo extends JpaRepository<Match,Long> {
    List<Match> findByStatus(String status);
    List<Match> findByPlayer1OrPlayer2(User player1, User player2);
}
