package com.hermesync.auth.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromPhone;

    private final RedisTemplate<String, String> redisTemplate;

    @PostConstruct
    public void initTwilio() {
        Twilio.init(accountSid, authToken);
    }

    public void sendOtp(String phone) {
        String otp = generateOtp();

        redisTemplate.opsForValue().set(
                "otp:" + phone,
                otp,
                5,
                TimeUnit.MINUTES
        );

        Message.creator(
                new PhoneNumber(phone),
                new PhoneNumber(fromPhone),
                "Your OTP is: " + otp
        ).create();
    }

    public boolean verifyOtp(String phone, String otp) {
        String stored = redisTemplate.opsForValue().get("otp:" + phone);
        if (stored != null && stored.equals(otp)) {
            redisTemplate.delete("otp:" + phone);
            return true;
        }
        return false;
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}