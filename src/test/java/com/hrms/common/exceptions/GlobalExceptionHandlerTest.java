package com.hrms.common.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hrms.common.responses.ClockSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-10-31T10:00:00Z"), ZoneOffset.UTC);
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new ValidationProbeController())
                .setControllerAdvice(new GlobalExceptionHandler(new ClockSource(fixedClock)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void returnsStableValidationErrorEnvelope() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.path").value("/test/validate"))
                .andExpect(jsonPath("$.data.details.email").exists())
                .andExpect(jsonPath("$.data.details.name").exists())
                .andExpect(jsonPath("$.timestamp").value("2026-10-31T10:00:00Z"));
    }

    @RestController
    @RequestMapping("/test")
    static class ValidationProbeController {
        @PostMapping("/validate")
        void validate(@Valid @RequestBody ProbeRequest request) {
        }
    }

    record ProbeRequest(@NotBlank String name, @Email String email) {
    }
}
