package com.quickbite.tracking.service;

import com.quickbite.tracking.dto.TrackingResponse;
public interface TrackingService {

    TrackingResponse getOrderTracking(String userEmail, String orderReference);
}

