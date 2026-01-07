package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.dtos.ProfileDto;
import com.codeduelz.codeduelz.dtos.UpdateProfileDto;
import com.codeduelz.codeduelz.entities.Profile;
import com.codeduelz.codeduelz.entities.User;

import java.util.Optional;

public interface ProfileService {
    public Profile createProfile(User user);
    public ProfileDto getProfile(User user);
    public ProfileDto updateProfile(User user, UpdateProfileDto dto);
}
