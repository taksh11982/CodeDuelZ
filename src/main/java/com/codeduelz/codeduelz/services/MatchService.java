package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.entities.Match;

public interface MatchService {
    public Match createMatch(Match match);
    public Match startMatch(Match match);
    public Match endMatch(Match match);
}
