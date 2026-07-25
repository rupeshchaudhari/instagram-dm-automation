package com.igdm.controller;

import com.igdm.dto.AuthDto.*;
import com.igdm.entity.User;
import com.igdm.repository.UserRepository;
import com.igdm.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    /**
     * POST /api/v1/auth/register — Register a new SaaS user.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is already registered"));
        }

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setPlanTier("free");
        user.setActive(true);

        user = userRepository.save(user);
        log.info("Registered new user: {}", user.getEmail());

        String token = tokenProvider.generateToken(user.getId(), user.getEmail());
        UserProfileDto profile = new UserProfileDto(user.getId(), user.getEmail(), user.getName(), user.getPlanTier());

        return ResponseEntity.ok(new AuthResponse(token, profile));
    }

    /**
     * POST /api/v1/auth/login — Authenticate existing user and return JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        var userOpt = userRepository.findByEmail(request.getEmail().toLowerCase().trim());

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            return ResponseEntity.status(403).body(Map.of("error", "Account is deactivated"));
        }

        String token = tokenProvider.generateToken(user.getId(), user.getEmail());
        UserProfileDto profile = new UserProfileDto(user.getId(), user.getEmail(), user.getName(), user.getPlanTier());

        log.info("User logged in: {}", user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, profile));
    }

    /**
     * GET /api/v1/auth/me — Fetch currently authenticated user profile.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        UserProfileDto profile = new UserProfileDto(user.getId(), user.getEmail(), user.getName(), user.getPlanTier());
        return ResponseEntity.ok(profile);
    }
}
