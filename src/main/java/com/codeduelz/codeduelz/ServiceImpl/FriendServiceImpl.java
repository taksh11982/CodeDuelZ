package com.codeduelz.codeduelz.ServiceImpl;

import com.codeduelz.codeduelz.dtos.FriendDto;
import com.codeduelz.codeduelz.dtos.FriendRequestDto;
import com.codeduelz.codeduelz.entities.Friend;
import com.codeduelz.codeduelz.entities.User;
import com.codeduelz.codeduelz.repo.FriendRepo;
import com.codeduelz.codeduelz.services.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendServiceImpl implements FriendService {
    private final FriendRepo friendRepo;

    @Override
    public void sendRequest(User from, User to) {
        if (from.getUserId().equals(to.getUserId())) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }
        boolean exists = friendRepo.existsByUserAndFriendUser(from, to);
        if (exists) {
            throw new IllegalStateException("Friend request already exists");
        }

        log.info("Sending friend request from {} to {}", from.getUsername(), to.getUsername());

        Friend request = new Friend();
        request.setUser(from);
        request.setFriendUser(to);
        request.setStatus("PENDING");

        Friend reverse = new Friend();
        reverse.setUser(to);
        reverse.setFriendUser(from);
        reverse.setStatus("PENDING");

        friendRepo.save(request);
        friendRepo.save(reverse);
    }

    @Override
    public void acceptFriendRequest(Long requestId, User user) {
        // requestId points to the record where friendUser=currentUser (from getPendingRequests)
        Friend friendRequest = friendRepo.findByIdAndFriendUser(requestId, user)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        if (!"PENDING".equals(friendRequest.getStatus())) {
            throw new IllegalStateException("Friend request is not pending");
        }

        log.info("User {} accepting friend request from {}", user.getUsername(),
                friendRequest.getUser().getUsername());

        // Update this side (sender -> currentUser) to ACCEPTED
        friendRequest.setStatus("ACCEPTED");
        friendRepo.save(friendRequest);

        // Find and update the reverse relationship (currentUser -> sender)
        Friend reverseRequest = friendRepo.findByUserAndFriendUserAndStatus(
                user, friendRequest.getUser(), "PENDING")
                .orElse(null);

        if (reverseRequest != null) {
            reverseRequest.setStatus("ACCEPTED");
            friendRepo.save(reverseRequest);
        }
    }

    @Override
    public void rejectFriendRequest(Long requestId, User user) {
        // requestId points to the record where friendUser=currentUser (from getPendingRequests)
        Friend friendRequest = friendRepo.findByIdAndFriendUser(requestId, user)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        if (!"PENDING".equals(friendRequest.getStatus())) {
            throw new IllegalStateException("Friend request is not pending");
        }

        log.info("User {} rejecting friend request from {}", user.getUsername(),
                friendRequest.getUser().getUsername());

        // Delete both sides of the friendship
        friendRepo.delete(friendRequest);

        // Find and delete the reverse relationship (currentUser -> sender)
        Friend reverseRequest = friendRepo.findByUserAndFriendUserAndStatus(
                user, friendRequest.getUser(), "PENDING")
                .orElse(null);

        if (reverseRequest != null) {
            friendRepo.delete(reverseRequest);
        }
    }

    @Override
    public List<FriendDto> getFriends(User user) {
        List<Friend> friends = friendRepo.findByUserAndStatus(user, "ACCEPTED");

        log.info("Fetching {} accepted friends for user {}", friends.size(), user.getUsername());

        // Map to DTOs and sort by online status (online friends first)
        return friends.stream()
                .map(friend -> {
                    User friendUser = friend.getFriendUser();
                    return new FriendDto(
                            friend.getId(),
                            friendUser.getUserId(),
                            friendUser.getUsername(),
                            friendUser.getEmail(),
                            friendUser.getIsOnline() != null ? friendUser.getIsOnline() : false);
                })
                .sorted(Comparator.comparing(FriendDto::getIsOnline).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<FriendRequestDto> getPendingRequests(User user) {
        // Get incoming friend requests (where user is the friendUser)
        List<Friend> pendingRequests = friendRepo.findByFriendUserAndStatus(user, "PENDING");

        log.info("Fetching {} pending friend requests for user {}", pendingRequests.size(), user.getUsername());

        return pendingRequests.stream()
                .map(request -> new FriendRequestDto(
                        request.getId(),
                        request.getUser().getUserId(),
                        request.getUser().getUsername(),
                        user.getUserId(),
                        user.getUsername(),
                        request.getStatus(),
                        null // createdAt not available in current Friend entity
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void removeFriend(Long friendId, User user) {
        Friend friendship = friendRepo.findByIdAndUser(friendId, user)
                .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));

        if (!"ACCEPTED".equals(friendship.getStatus())) {
            throw new IllegalStateException("Can only remove accepted friends");
        }

        log.info("User {} removing friend {}", user.getUsername(), friendship.getFriendUser().getUsername());

        // Delete both sides of the friendship
        friendRepo.delete(friendship);

        // Find and delete the reverse relationship
        Friend reverseFriendship = friendRepo.findByUserAndFriendUserAndStatus(
                friendship.getFriendUser(), user, "ACCEPTED")
                .orElse(null);

        if (reverseFriendship != null) {
            friendRepo.delete(reverseFriendship);
        }
    }
}
