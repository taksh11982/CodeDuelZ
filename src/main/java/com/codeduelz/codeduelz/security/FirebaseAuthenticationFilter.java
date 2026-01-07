package com.codeduelz.codeduelz.security;

import com.codeduelz.codeduelz.entities.User;
import com.codeduelz.codeduelz.services.FirebaseAuthService;
import com.codeduelz.codeduelz.services.UserService;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class FirebaseAuthenticationFilter
        extends OncePerRequestFilter {

    private final FirebaseAuthService firebaseAuthService;
    private final UserService userService;

    public FirebaseAuthenticationFilter(
            FirebaseAuthService firebaseAuthService,
            UserService userService) {
        this.firebaseAuthService = firebaseAuthService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            FirebaseToken decoded = firebaseAuthService.verifyToken(token);

            User user = userService.findOrCreateFirebaseUser(decoded);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}

