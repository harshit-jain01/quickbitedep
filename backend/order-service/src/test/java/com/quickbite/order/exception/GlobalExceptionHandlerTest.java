package com.quickbite.order.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
    void shouldHandleIllegalArgument() throws Exception {
        mockMvc.perform(get("/throw/illegal"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("bad input"));
    }

    @Test
    void shouldHandleFeignExceptionAsBadGateway() throws Exception {
        mockMvc.perform(get("/throw/feign"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldHandleDataAccessException() throws Exception {
        mockMvc.perform(get("/throw/db"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("db down"));
    }

    @Test
    void shouldHandleAccessDenied() throws Exception {
        mockMvc.perform(get("/throw/access"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("forbidden"));
    }

    @Test
    void shouldHandleRuntimeException() throws Exception {
        mockMvc.perform(get("/throw/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("runtime failure"));
    }

    @RestController
    static class ThrowingController {
        @GetMapping("/throw/illegal")
        ResponseEntity<Void> illegal() {
            throw new IllegalArgumentException("bad input");
        }

        @GetMapping("/throw/feign")
        ResponseEntity<Void> feign() {
            Request request = Request.create(
                    Request.HttpMethod.GET,
                    "http://service/path",
                    Map.of(),
                    null,
                    StandardCharsets.UTF_8,
                    null
            );
            throw feign.FeignException.errorStatus("call", Response.builder()
                    .status(502)
                    .reason("bad gateway")
                    .request(request)
                    .headers(Map.of())
                    .build());
        }

        @GetMapping("/throw/db")
        ResponseEntity<Void> db() {
            throw new DataAccessResourceFailureException("db", new RuntimeException("db down"));
        }

        @GetMapping("/throw/access")
        ResponseEntity<Void> access() {
            throw new AccessDeniedException("forbidden");
        }

        @GetMapping("/throw/runtime")
        ResponseEntity<Void> runtime() {
            throw new RuntimeException("runtime failure");
        }
    }
}
