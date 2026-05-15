package com.quickbite.payment.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
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
    void shouldHandleIllegalStateException() throws Exception {
        mockMvc.perform(get("/throw/state"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("state error"));
    }

    @Test
    void shouldHandleRuntimeException() throws Exception {
        mockMvc.perform(get("/throw/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("runtime error"));
    }

    @RestController
    static class ThrowingController {
        @GetMapping("/throw/illegal")
        ResponseEntity<Void> illegal() {
            throw new IllegalArgumentException("bad input");
        }

        @GetMapping("/throw/state")
        ResponseEntity<Void> state() {
            throw new IllegalStateException("state error");
        }

        @GetMapping("/throw/runtime")
        ResponseEntity<Void> runtime() {
            throw new RuntimeException("runtime error");
        }
    }
}
