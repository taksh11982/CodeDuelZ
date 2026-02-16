package com.codeduelz.codeduelz.controller;

import com.codeduelz.codeduelz.dtos.FriendDto;
import com.codeduelz.codeduelz.dtos.FriendRequestDto;
import com.codeduelz.codeduelz.entities.User;
import com.codeduelz.codeduelz.repo.UserRepo;
import com.codeduelz.codeduelz.services.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {
    private final FriendService friendService;
    private final UserRepo userRepo;

    /**
     * Send a friend request to another user
     * POST /api/friends/request
     * Body: { "toUsername": "username" }
     */
    @PostMapping("/request")
    public ResponseEntity<?> sendFriendRequest(
            @RequestBody Map<String, String> payload,
            Authentication authentication) {

        User currentUser = userRepo.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String toUsername = payload.get("toUsername");
        User toUser = userRepo.findByUserName(toUsername)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        friendService.sendRequest(currentUser, toUser);

        return ResponseEntity.ok(Map.of("message", "Friend request sent successfully"));
    }

    /**
     * Accept a friend request
     * POST /api/friends/accept/{requestId}
     */
    @PostMapping("/accept/{requestId}")
    public ResponseEntity<?> acceptFriendRequest(
            @PathVariable Long requestId,
            Authentication authentication) {

        User currentUser = userRepo.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        friendService.acceptFriendRequest(requestId, currentUser);

        return ResponseEntity.ok(Map.of("message", "Friend request accepted"));
    }

    /**
     * Reject a friend request
     * POST /api/friends/reject/{requestId}
     */
    @PostMapping("/reject/{requestId}")
    public ResponseEntity<?> rejectFriendRequest(
            @PathVariable Long requestId,
            Authentication authentication) {

        User currentUser = userRepo.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        friendService.rejectFriendRequest(requestId, currentUser);

        return ResponseEntity.ok(Map.of("message", "Friend request rejected"));
    }

    /**
     * Get all accepted friends (sorted by online status - online friends first)
     * GET /api/friends
     */
    @GetMapping
    public ResponseEntity<List<FriendDto>> getFriends(Authentication authentication) {
        User currentUser = userRepo.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<FriendDto> friends = friendService.getFriends(currentUser);

        return ResponseEntity.ok(friends);
    }

    /**
     * Get pending friend requests (incoming requests)
     * GET /api/friends/requests
     */
    @GetMapping("/requests")
    public ResponseEntity<List<FriendRequestDto>> getPendingRequests(Authentication authentication) {
        User currentUser = userRepo.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<FriendRequestDto> requests = friendService.getPendingRequests(currentUser);

        return ResponseEntity.ok(requests);
    }

    /**
     * Remove a friend
     * DELETE /api/friends/{friendId}
     */
    @DeleteMapping("/{friendId}")
    public ResponseEntity<?> removeFriend(
            @PathVariable Long friendId,
            Authentication authentication) {

        User currentUser = userRepo.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        friendService.removeFriend(friendId, currentUser);

        return ResponseEntity.ok(Map.of("message", "Friend removed successfully"));
    }
}
