package com.parlour.api.booking;

import com.parlour.api.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingFlowTest extends ApiTestSupport {

    private String firstServiceId() throws Exception {
        String services = mvc.perform(get("/api/services")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Integer id = json(services, "$[0].id");
        return String.valueOf(id);
    }

    private String availability(LocalDate day, String serviceId) throws Exception {
        return mvc.perform(get("/api/bookings/availability").param("date", day.toString()).param("serviceId", serviceId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("customer books online with an OTP; staff confirm it; chairs fill up")
    void customerBooksWithOtp() throws Exception {
        String serviceId = firstServiceId();
        LocalDate day = LocalDate.now().plusDays(1);
        assertTrue(availability(day, serviceId).contains("11:00"));

        String phone = "+8801711111111";
        String bookingJson = "{\"customerName\":\"Rina\",\"customerPhone\":\"" + phone + "\",\"serviceId\":" + serviceId
                + ",\"startTime\":\"" + day + "T11:00:00\",\"otp\":\"%s\"}";

        // 1) a wrong OTP is rejected
        String otp = json(postJson("/api/bookings/request-otp", "{\"phone\":\"" + phone + "\"}", null, 200), "$.devOtp");
        String wrong = otp.equals("123456") ? "654321" : "123456";
        postJson("/api/bookings", String.format(bookingJson, wrong), null, 400);

        // 2) the right OTP works (the failed attempt did not burn the code)
        String confirmation = postJson("/api/bookings", String.format(bookingJson, otp), null, 200);
        assertTrue(confirmation.contains("\"status\":\"PENDING\""));
        assertFalse(confirmation.contains(phone), "public response must not echo personal data");

        // 3) the same OTP can never be used twice
        postJson("/api/bookings", String.format(bookingJson, otp), null, 400);

        // 4) staff see the booking and confirm it
        String token = accessTokenOf(MANAGER_EMAIL, MANAGER_PASSWORD);
        String list = mvc.perform(withToken(get("/api/admin/bookings"), token).param("date", day.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(list.contains("Rina"));
        Integer id = json(list, "$[0].id");
        mvc.perform(withToken(patch("/api/admin/bookings/" + id + "/status"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 5) a phone booking by staff takes the second chair
        String byCall = postJson("/api/admin/bookings", staffBooking("Mim", "01722222222", serviceId, day), token, 200);
        assertEquals("CALL", json(byCall, "$.bookedVia"));

        // 6) both chairs are taken: the slot disappears and a third booking is refused
        assertFalse(availability(day, serviceId).contains("\"11:00\""));
        postJson("/api/admin/bookings", staffBooking("Third", "01733333333", serviceId, day), token, 409);
    }

    @Test
    @DisplayName("a cancelled booking cannot be changed again")
    void cancelledBookingIsFinal() throws Exception {
        String serviceId = firstServiceId();
        LocalDate day = LocalDate.now().plusDays(3);
        String token = accessTokenOf(MANAGER_EMAIL, MANAGER_PASSWORD);

        String created = postJson("/api/admin/bookings", staffBooking("Sara", "01744444444", serviceId, day), token, 200);
        Integer id = json(created, "$.id");
        String statusUrl = "/api/admin/bookings/" + id + "/status";

        mvc.perform(withToken(patch(statusUrl), token).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());
        mvc.perform(withToken(patch(statusUrl), token).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isBadRequest());
    }

    private static String staffBooking(String name, String phone, String serviceId, LocalDate day) {
        return "{\"customerName\":\"" + name + "\",\"customerPhone\":\"" + phone + "\",\"serviceId\":" + serviceId
                + ",\"startTime\":\"" + day + "T11:00:00\"}";
    }
}
