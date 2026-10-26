package com.parlour.api.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for API tests: starts the whole application against an in-memory H2 database
 * (profile "test") and offers small helpers so the tests read like the user's steps.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestSupport {

    protected static final String OWNER_EMAIL = "owner@parlour.local";
    protected static final String OWNER_PASSWORD = "Owner@12345";
    protected static final String MANAGER_EMAIL = "manager@parlour.local";
    protected static final String MANAGER_PASSWORD = "Manager@12345";

    @Autowired
    protected MockMvc mvc;

    /** Reads one value out of a JSON string, e.g. json(body, "$.accessToken"). */
    protected static <T> T json(String body, String path) {
        return JsonPath.read(body, path);
    }

    protected static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    /** POSTs JSON, asserts the HTTP status and returns the response body. */
    protected String postJson(String url, String body, String token, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder request = post(url).contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) {
            request = withToken(request, token);
        }
        return mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
    }

    /** Full two-step staff login (password, then OTP). Returns the token response JSON. */
    protected String login(String email, String password) throws Exception {
        String challenge = postJson("/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}", null, 200);
        String otp = json(challenge, "$.devOtp");
        return postJson("/api/auth/verify-otp", "{\"email\":\"" + email + "\",\"code\":\"" + otp + "\"}", null, 200);
    }

    protected String accessTokenOf(String email, String password) throws Exception {
        return json(login(email, password), "$.accessToken");
    }
}
