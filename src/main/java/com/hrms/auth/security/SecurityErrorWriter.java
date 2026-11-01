package com.hrms.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.common.exceptions.ErrorCode;
import com.hrms.common.responses.ApiError;
import com.hrms.common.responses.ApiResponse;
import com.hrms.common.responses.ClockSource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;
    private final ClockSource clockSource;

    public SecurityErrorWriter(ObjectMapper objectMapper, ClockSource clockSource) {
        this.objectMapper = objectMapper;
        this.clockSource = clockSource;
    }

    public void unauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(request, response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.UNAUTHORIZED,
                "Authentication is required");
    }

    public void forbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(request, response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.FORBIDDEN,
                "You do not have permission to perform this operation");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, int status,
                       ErrorCode code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = new ApiError(code.name(), request.getRequestURI(), Map.of());
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(message, error, clockSource));
    }
}
