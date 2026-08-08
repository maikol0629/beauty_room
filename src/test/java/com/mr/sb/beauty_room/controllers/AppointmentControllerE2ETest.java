package com.mr.sb.beauty_room.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AppointmentControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAsClient() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    @Test
    void fullAppointmentFlow_shouldBookSlotAndRejectDoubleBooking() throws Exception {
        String jwt = loginAsClient();

        LocalDate nextMonday = nextDayOfWeek(LocalDate.now().plusDays(2), DayOfWeek.MONDAY);

        mockMvc.perform(get("/api/appointment/slots")
                        .param("stylistId", "1")
                        .param("serviceId", "1")
                        .param("date", nextMonday.toString())
                        .header("X-Tenant-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        LocalDateTime start = nextMonday.atTime(10, 0);
        String appointmentJson = "{"
                + "\"startDate\": \"" + start + "\","
                + "\"clientId\": 3,"
                + "\"stylistId\": 1,"
                + "\"serviceId\": 1"
                + "}";

        mockMvc.perform(post("/api/appointment/save")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointmentJson))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/appointment/save")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointmentJson))
                .andExpect(status().isConflict());
    }

    @Test
    void telegramChatId_shouldBeExposedInClientResponse() throws Exception {
        String jwt = loginAsClient();

        MvcResult result = mockMvc.perform(get("/api/client/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("telegramChatId").asText()).isEqualTo("111111111");
    }

    private LocalDate nextDayOfWeek(LocalDate start, DayOfWeek day) {
        LocalDate date = start;
        while (date.getDayOfWeek() != day) {
            date = date.plusDays(1);
        }
        return date;
    }
}
