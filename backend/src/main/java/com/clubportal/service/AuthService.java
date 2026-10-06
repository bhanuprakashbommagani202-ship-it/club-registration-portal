package com.clubportal.service;

import com.clubportal.dto.AuthResponse;
import com.clubportal.dto.LoginRequest;
import com.clubportal.dto.RegisterRequest;
import com.clubportal.dto.UserDto;
import com.clubportal.exception.ApiException;
import com.clubportal.model.Role;
import com.clubportal.model.User;
import com.clubportal.repository.UserRepository;
import com.clubportal.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Public sign-up always creates a STUDENT. Coordinators and admins are created by an admin. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_ALREADY_EXISTS", "An account with this email already exists.");
        }
        User user = new User(req.name().trim(), email, passwordEncoder.encode(req.password()), Role.STUDENT,
                Text.clean(req.department()), Text.clean(req.rollNumber()));
        userRepository.save(user);
        return new AuthResponse(jwtService.createToken(user), UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                // Same message for unknown email and wrong password, so nobody can probe which accounts exist.
                .orElseThrow(() -> new BadCredentialsException("bad credentials"));
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED",
                    "Your account has been disabled. Contact the admin.");
        }
        return new AuthResponse(jwtService.createToken(user), UserDto.from(user));
    }
}
