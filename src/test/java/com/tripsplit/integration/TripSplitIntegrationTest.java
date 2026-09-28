package com.tripsplit.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripsplit.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TripSplitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long tripId;
    private static Long aliceId;
    private static Long bobId;
    private static Long charlieId;
    private static Long expenseId;

    @Test
    @Order(1)
    void createTrip() throws Exception {
        TripRequest request = TripRequest.builder()
                .name("Goa Trip")
                .description("Integration test trip")
                .destination("Goa")
                .startDate(LocalDate.of(2026, 10, 10))
                .endDate(LocalDate.of(2026, 10, 13))
                .participants(List.of(
                        ParticipantRequest.builder().name("Alice").email("alice@test.com").build(),
                        ParticipantRequest.builder().name("Bob").email("bob@test.com").build(),
                        ParticipantRequest.builder().name("Charlie").email("charlie@test.com").build()
                ))
                .build();

        MvcResult result = mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User", "TestUser")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Goa Trip"))
                .andExpect(jsonPath("$.participants", hasSize(3)))
                .andReturn();

        TripResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), TripResponse.class);
        tripId = response.getId();
        aliceId = response.getParticipants().get(0).getId();
        bobId = response.getParticipants().get(1).getId();
        charlieId = response.getParticipants().get(2).getId();
    }

    @Test
    @Order(2)
    void getTrip() throws Exception {
        mockMvc.perform(get("/api/trips/" + tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tripId))
                .andExpect(jsonPath("$.name").value("Goa Trip"));
    }

    @Test
    @Order(3)
    void addParticipant() throws Exception {
        ParticipantRequest request = ParticipantRequest.builder()
                .name("David")
                .email("david@test.com")
                .build();

        mockMvc.perform(post("/api/trips/" + tripId + "/participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User", "TestUser")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("David"));
    }

    @Test
    @Order(4)
    void addDuplicateParticipant_shouldFail() throws Exception {
        ParticipantRequest request = ParticipantRequest.builder()
                .name("Alice Duplicate")
                .email("alice@test.com")
                .build();

        mockMvc.perform(post("/api/trips/" + tripId + "/participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    void createEqualExpense() throws Exception {
        ExpenseRequest request = ExpenseRequest.builder()
                .description("Dinner")
                .amount(BigDecimal.valueOf(2400))
                .payerId(aliceId)
                .expenseDate(LocalDate.of(2026, 10, 10))
                .participants(List.of(
                        ExpenseParticipantRequest.builder().participantId(aliceId).build(),
                        ExpenseParticipantRequest.builder().participantId(bobId).build(),
                        ExpenseParticipantRequest.builder().participantId(charlieId).build()
                ))
                .build();

        MvcResult result = mockMvc.perform(post("/api/trips/" + tripId + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User", "TestUser")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Dinner"))
                .andExpect(jsonPath("$.participants", hasSize(3)))
                .andReturn();

        ExpenseResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), ExpenseResponse.class);
        expenseId = response.getId();
    }

    @Test
    @Order(6)
    void createCustomExpense() throws Exception {
        ExpenseRequest request = ExpenseRequest.builder()
                .description("Hotel")
                .amount(BigDecimal.valueOf(5000))
                .payerId(bobId)
                .expenseDate(LocalDate.of(2026, 10, 11))
                .participants(List.of(
                        ExpenseParticipantRequest.builder().participantId(aliceId).owedAmount(BigDecimal.valueOf(2000)).build(),
                        ExpenseParticipantRequest.builder().participantId(bobId).owedAmount(BigDecimal.valueOf(1500)).build(),
                        ExpenseParticipantRequest.builder().participantId(charlieId).owedAmount(BigDecimal.valueOf(1500)).build()
                ))
                .build();

        mockMvc.perform(post("/api/trips/" + tripId + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User", "TestUser")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Hotel"));
    }

    @Test
    @Order(7)
    void getExpensesPaginated() throws Exception {
        mockMvc.perform(get("/api/trips/" + tripId + "/expenses")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "expenseDate")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)));
    }

    @Test
    @Order(8)
    void getBalances() throws Exception {
        mockMvc.perform(get("/api/trips/" + tripId + "/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    @Order(9)
    void generateSettlement() throws Exception {
        mockMvc.perform(post("/api/trips/" + tripId + "/settlements/generate")
                        .header("X-User", "TestUser"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.settlements", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @Order(10)
    void getSettlement() throws Exception {
        mockMvc.perform(get("/api/trips/" + tripId + "/settlement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(tripId));
    }

    @Test
    @Order(11)
    void updateExpense() throws Exception {
        ExpenseRequest request = ExpenseRequest.builder()
                .description("Dinner Updated")
                .amount(BigDecimal.valueOf(3000))
                .payerId(aliceId)
                .expenseDate(LocalDate.of(2026, 10, 10))
                .participants(List.of(
                        ExpenseParticipantRequest.builder().participantId(aliceId).build(),
                        ExpenseParticipantRequest.builder().participantId(bobId).build(),
                        ExpenseParticipantRequest.builder().participantId(charlieId).build()
                ))
                .build();

        mockMvc.perform(put("/api/trips/" + tripId + "/expenses/" + expenseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User", "TestUser")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Dinner Updated"));
    }

    @Test
    @Order(12)
    void getHistory() throws Exception {
        mockMvc.perform(get("/api/trips/" + tripId + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @Order(13)
    void getAuditLogs() throws Exception {
        mockMvc.perform(get("/api/trips/" + tripId + "/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @Order(14)
    void deleteExpense() throws Exception {
        mockMvc.perform(delete("/api/trips/" + tripId + "/expenses/" + expenseId)
                        .header("X-User", "TestUser"))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(15)
    void tripNotFound() throws Exception {
        mockMvc.perform(get("/api/trips/99999"))
                .andExpect(status().isNotFound());
    }
}
