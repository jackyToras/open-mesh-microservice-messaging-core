package com.hermesync.auth.service;

import com.hermesync.auth.dto.AuthResponse;
import com.hermesync.auth.model.User;
import com.hermesync.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final OtpService otpService;

    public void sendOtp(String phone) {
        otpService.sendOtp(phone);
    }

    public AuthResponse verifyOtp(String phone, String otp) {

        boolean isValid = otpService.verifyOtp(phone, otp);

        if (!isValid) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        User user = userRepository.findByPhone(phone)
                .orElseGet(() -> createNewUser(phone));

        String userId = user.getUserId().toString();
        String accessToken = jwtService.generateAccessToken(userId, phone);
        String refreshToken = jwtService.generateRefreshToken(userId);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(userId)
                .build();
    }

    public String refreshToken(String userId, String refreshToken) {

        boolean isValid = jwtService.validateRefreshToken(userId, refreshToken);

        if (!isValid) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        return jwtService.generateAccessToken(userId, user.getPhone());
    }

    public void logout(String userId) {
        jwtService.deleteRefreshToken(userId);
    }

    private User createNewUser(String phone) {
        User user = User.builder()
                .userId(UUID.randomUUID())
                .phone(phone)
                .createdAt(Instant.now())
                .build();
        return userRepository.save(user);
    }
}