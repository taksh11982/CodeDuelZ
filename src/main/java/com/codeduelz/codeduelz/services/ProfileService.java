package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.entities.Profile;
import com.codeduelz.codeduelz.entities.User;

public interface ProfileService {
    public Profile createProfile(User user);
    public int getUserRating(Long userId);
}
