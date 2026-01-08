package com.codeduelz.codeduelz.ServiceImpl;

import com.codeduelz.codeduelz.dtos.ProfileDto;
import com.codeduelz.codeduelz.dtos.UpdateProfileDto;
import com.codeduelz.codeduelz.entities.Profile;
import com.codeduelz.codeduelz.entities.User;
import com.codeduelz.codeduelz.repo.ProfileRepo;
import com.codeduelz.codeduelz.repo.UserRepo;
import com.codeduelz.codeduelz.services.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class ProfileServiceImpl implements ProfileService {
    @Autowired
    private ProfileRepo profileRepo;
    @Autowired
    private UserRepo userRepo;
    public Profile createProfile(User user){
        Profile profile= new Profile();
        profile.setUser(user);
        profile.setWins(0);
        profile.setTotalMatches(0);
        profile.setAvatar("/avatars/default.png");
        profileRepo.save(profile);
        return profile;
    }

    @Override
    public ProfileDto getProfile(User user) {
        Profile profile = profileRepo.findByUser(user)
                .orElseGet(() -> createProfile(user));

        ProfileDto dto = new ProfileDto();
        dto.setUserName(user.getEmail());
        dto.setEmail(user.getEmail());
        dto.setRating(profile.getRating());
        dto.setTotalMatches(profile.getTotalMatches());
        dto.setWins(profile.getWins());
        dto.setBio(profile.getBio());
        dto.setAvatar(profile.getAvatar());

        return dto;
    }

    @Override
    public ProfileDto updateProfile(User user, UpdateProfileDto dto) {
        Profile profile = profileRepo.findByUser(user)
                .orElseGet(() -> createProfile(user));

        if (dto.getBio() != null) {
            profile.setBio(dto.getBio());
        }
        if (dto.getAvatar() != null) {
            profile.setAvatar(dto.getAvatar());
        }
        if (dto.getLeetcodeUsername() != null) {
            profile.setLeetcodeUsername(dto.getLeetcodeUsername());
        }
        if (dto.getCodechefUsername() != null) {
            profile.setCodechefUsername(dto.getCodechefUsername());
        }
        if (dto.getCodeforcesHandle() != null) {
            profile.setCodeforcesHandle(dto.getCodeforcesHandle());
        }
        profileRepo.save(profile);
        return getProfile(user);
    }


}
