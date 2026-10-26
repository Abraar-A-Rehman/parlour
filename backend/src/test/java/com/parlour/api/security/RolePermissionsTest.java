package com.parlour.api.security;

import com.parlour.api.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Who may call what: anonymous visitor, MANAGER and OWNER. */
class RolePermissionsTest extends ApiTestSupport {

    private static final String NEW_SERVICE =
            "{\"name\":\"Role Test Service\",\"category\":\"Hair\",\"description\":\"d\",\"price\":500,\"durationMin\":30}";
    private static final String SETTINGS =
            "{\"parlourName\":\"X\",\"tagline\":\"Y\",\"phone\":\"+8801700000000\"}";

    @Test
    @DisplayName("anonymous visitors can read the website but not the bookings")
    void anonymous() throws Exception {
        mvc.perform(get("/api/services")).andExpect(status().isOk());
        mvc.perform(get("/api/settings")).andExpect(status().isOk());
        mvc.perform(get("/api/admin/bookings")).andExpect(status().isUnauthorized());
        postJson("/api/services", NEW_SERVICE, null, 401);
    }

    @Test
    @DisplayName("manager sees bookings but cannot edit the website")
    void manager() throws Exception {
        String token = accessTokenOf(MANAGER_EMAIL, MANAGER_PASSWORD);

        mvc.perform(withToken(get("/api/admin/bookings"), token)).andExpect(status().isOk());
        postJson("/api/services", NEW_SERVICE, token, 403);
        mvc.perform(withToken(put("/api/settings"), token).contentType(MediaType.APPLICATION_JSON).content(SETTINGS))
                .andExpect(status().isForbidden());
        mvc.perform(withToken(multipart("/api/uploads").file(pngFile()), token)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("owner can edit services and upload images, and fake images are rejected")
    void owner() throws Exception {
        String token = accessTokenOf(OWNER_EMAIL, OWNER_PASSWORD);

        String created = postJson("/api/services", NEW_SERVICE, token, 200);
        assertTrue(created.contains("Role Test Service"));

        MockMultipartFile fake = new MockMultipartFile("file", "a.png", "image/png", "not an image at all".getBytes());
        mvc.perform(withToken(multipart("/api/uploads").file(fake), token)).andExpect(status().isBadRequest());
        mvc.perform(withToken(multipart("/api/uploads").file(pngFile()), token)).andExpect(status().isOk());
    }

    private static MockMultipartFile pngFile() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 0};
        return new MockMultipartFile("file", "a.png", "image/png", png);
    }
}
