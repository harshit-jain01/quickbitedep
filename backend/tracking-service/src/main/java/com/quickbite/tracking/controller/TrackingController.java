package com.quickbite.tracking.controller;

import com.quickbite.tracking.dto.TrackingResponse;
import com.quickbite.tracking.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tracking")
public class TrackingController {

    private static final Logger logger = LoggerFactory.getLogger(TrackingController.class);

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/orders/{orderReference}")
    public ResponseEntity<TrackingResponse> getOrderTracking(
            @RequestHeader("X-Authenticated-User") String userEmail,
            @PathVariable String orderReference
    ) {
        logger.info("Tracking request received for orderReference={} user={}", orderReference, userEmail);
        TrackingResponse response = trackingService.getOrderTracking(userEmail, orderReference);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/health")
    public ResponseEntity<String> health() {
        logger.debug("Tracking health endpoint invoked");
        return ResponseEntity.ok("Tracking Service is running");
    }
}

