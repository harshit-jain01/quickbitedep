package com.quickbite.auth.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void shouldHandleIllegalArgumentException() throws Exception {
        mockMvc.perform(get("/throw/illegal"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("bad input"));
    }

    @Test
    void shouldHandleBadCredentialsException() throws Exception {
        mockMvc.perform(get("/throw/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("invalid credentials"));
    }

    @Test
    void shouldHandleDataIntegrityException() throws Exception {
        mockMvc.perform(get("/throw/data-integrity"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unable to save user data. Please check your input and try again."));
    }

    @Test
    void shouldHandleUnexpectedException() throws Exception {
        mockMvc.perform(get("/throw/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected server error. Please try again."));
    }

    @RestController
    static class ThrowingController {
        @GetMapping("/throw/illegal")
        ResponseEntity<Void> illegal() {
            throw new IllegalArgumentException("bad input");
        }

        @GetMapping("/throw/bad-credentials")
        ResponseEntity<Void> badCredentials() {
            throw new BadCredentialsException("invalid credentials");
        }

        @GetMapping("/throw/data-integrity")
        ResponseEntity<Void> dataIntegrity() {
            throw new DataIntegrityViolationException("constraint");
        }

        @GetMapping("/throw/unexpected")
        ResponseEntity<Void> unexpected() {
            throw new RuntimeException("boom");
        }
    }
}
