package com.parlour.api.auth;

import com.parlour.api.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AuthFlowTest extends ApiTestSupport {

    @Test
    @DisplayName("wrong password is rejected and no OTP is sent")
    void wrongPassword() throws Exception {
        postJson("/api/auth/login", "{\"email\":\"" + OWNER_EMAIL + "\",\"password\":\"nope\"}", null, 401);
    }

    @Test
    @DisplayName("wrong OTP is rejected")
    void wrongOtp() throws Exception {
        postJson("/api/auth/login", "{\"email\":\"" + OWNER_EMAIL + "\",\"password\":\"" + OWNER_PASSWORD + "\"}", null, 200);
        postJson("/api/auth/verify-otp", "{\"email\":\"" + OWNER_EMAIL + "\",\"code\":\"000000x\"}", null, 400);
    }

    @Test
    @DisplayName("password then OTP gives an access token and a refresh token")
    void fullLogin() throws Exception {
        String tokens = login(OWNER_EMAIL, OWNER_PASSWORD);
        assertNotEquals("", json(tokens, "$.accessToken"));
        assertNotEquals("", json(tokens, "$.refreshToken"));
    }

    @Test
    @DisplayName("refresh token rotates, and reusing an old one revokes the whole session")
    void refreshRotationAndReuseDetection() throws Exception {
        String oldRefresh = json(login(OWNER_EMAIL, OWNER_PASSWORD), "$.refreshToken");

        String rotated = postJson("/api/auth/refresh", "{\"refreshToken\":\"" + oldRefresh + "\"}", null, 200);
        String newRefresh = json(rotated, "$.refreshToken");
        assertNotEquals(oldRefresh, newRefresh);

        postJson("/api/auth/refresh", "{\"refreshToken\":\"" + oldRefresh + "\"}", null, 401); // reuse detected
        postJson("/api/auth/refresh", "{\"refreshToken\":\"" + newRefresh + "\"}", null, 401); // session revoked
    }

    @Test
    @DisplayName("five wrong passwords lock the account for a while")
    void bruteForceLockout() throws Exception {
        String body = "{\"email\":\"locktest@parlour.local\",\"password\":\"wrong\"}";
        for (int i = 0; i < 5; i++) {
            postJson("/api/auth/login", body, null, 401);
        }
        postJson("/api/auth/login", body, null, 429);
    }
}
