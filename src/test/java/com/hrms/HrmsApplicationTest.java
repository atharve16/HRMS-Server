package com.hrms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.data.mongodb.auto-index-creation=false",
        "management.health.mongo.enabled=false",
        "app.auth.jwt.secret=jwt-test-secret-at-least-thirty-two-characters",
        "app.auth.otp.hash-secret=otp-test-secret-at-least-thirty-two-characters",
        "app.auth.mail.from=noreply@example.com"
})
@AutoConfigureMockMvc
class HrmsApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void startsApplicationAndExposesStandardHealthResponse() throws Exception {
        mockMvc.perform(get("/api/v1/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.companyTimeZone").value("Asia/Kolkata"));
    }

    @Test
    void deniesUnconfiguredApiRoutesByDefault() throws Exception {
        mockMvc.perform(get("/api/v1/private-probe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exposesOpenApiDocumentInDevelopmentProfile() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("HRMS REST API"))
                .andExpect(jsonPath("$.paths['/api/v1/system/health']").exists());
    }
}
