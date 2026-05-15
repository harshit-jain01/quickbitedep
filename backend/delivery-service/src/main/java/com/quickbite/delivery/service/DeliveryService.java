package com.quickbite.delivery.service;

import com.quickbite.delivery.dto.AgentAssignedOrderResponse;
import com.quickbite.delivery.dto.AgentEarningsResponse;
import com.quickbite.delivery.dto.DeliveryAssignmentRequest;
import com.quickbite.delivery.dto.DeliveryAssignmentResponse;
import com.quickbite.delivery.dto.DeliveryAgentAvailabilityRequest;
import com.quickbite.delivery.dto.DeliveryAgentRegistrationRequest;
import com.quickbite.delivery.dto.DeliveryAgentResponse;
import java.util.List;
public interface DeliveryService {

    DeliveryAgentResponse register(DeliveryAgentRegistrationRequest request);

    List<DeliveryAgentResponse> getAllAgents();

    DeliveryAgentResponse verifyAndActivate(String agentId);

    DeliveryAgentResponse getAgentByPhone(String phone);

    DeliveryAgentResponse getOrCreateAgentByPhone(String phone, String authenticatedUser);

    DeliveryAgentResponse updateAvailability(String agentId, DeliveryAgentAvailabilityRequest request);

    DeliveryAssignmentResponse assign(DeliveryAssignmentRequest request);

    List<AgentAssignedOrderResponse> getAssignedOrders(String agentId);

    AgentAssignedOrderResponse acceptOrder(String agentId, String orderReference);

    AgentAssignedOrderResponse markPickedUp(String agentId, String orderReference);

    AgentAssignedOrderResponse markDelivered(String agentId, String orderReference);

    AgentEarningsResponse getEarnings(String agentId);
}
