package com.codeduelz.codeduelz.ServiceImpl;

import com.codeduelz.codeduelz.entities.Profile;
import com.codeduelz.codeduelz.entities.User;
import com.codeduelz.codeduelz.repo.ProfileRepo;
import com.codeduelz.codeduelz.repo.UserRepo;
import com.codeduelz.codeduelz.services.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public int getUserRating(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getRating();
    }

}
