package com.quickbite.delivery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickbite.delivery.client.OrderInternalClient;
import com.quickbite.delivery.dto.AgentAssignedOrderResponse;
import com.quickbite.delivery.dto.AgentEarningsResponse;
import com.quickbite.delivery.dto.DeliveryAgentRegistrationRequest;
import com.quickbite.delivery.dto.DeliveryAssignmentRequest;
import com.quickbite.delivery.model.AgentOrderAssignment;
import com.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.delivery.service.impl.DeliveryServiceImpl;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryAgentRepository deliveryAgentRepository;

    @Mock
    @SuppressWarnings("unused")
    private OrderInternalClient orderInternalClient;

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    @Test
    void register_shouldTrimInputAndReturnMappedResponse() {
        DeliveryAgentRegistrationRequest request = new DeliveryAgentRegistrationRequest(
                "  John  ",
                "  +91-9999988888  ",
                "  Bike  ",
                "  KA01AB1234  "
        );

        DeliveryAgent savedAgent = agent("AGT1001", "John", "+91-9999988888", "Bike", "KA01AB1234", 0);
        when(deliveryAgentRepository.register("John", "+91-9999988888", "Bike", "KA01AB1234")).thenReturn(savedAgent);

        var response = deliveryService.register(request);

        assertEquals("AGT1001", response.id());
        assertEquals("John", response.name());
        verify(deliveryAgentRepository).register("John", "+91-9999988888", "Bike", "KA01AB1234");
    }

    @Test
    void getOrCreateAgentByPhone_shouldCreateAgent_whenNotFound() {
        String phone = "+91 9999900000";
        when(deliveryAgentRepository.findByPhone(phone)).thenThrow(new IllegalArgumentException("Delivery agent not found"));

        DeliveryAgent created = agent("AGT1003", "John doe", "+91 9999900000", "Bike", "TEMP-9199999", 0);
        when(deliveryAgentRepository.register(anyString(), eq(phone), eq("Bike"), anyString())).thenReturn(created);

        var response = deliveryService.getOrCreateAgentByPhone(phone, "john.doe@mail.com");

        assertEquals("AGT1003", response.id());
        assertEquals("John doe", response.name());

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(deliveryAgentRepository).register(nameCaptor.capture(), eq(phone), eq("Bike"), anyString());
        assertEquals("John doe", nameCaptor.getValue());
    }

    @Test
    void getOrCreateAgentByPhone_shouldThrow_whenPhoneBlankAfterFallback() {
        when(deliveryAgentRepository.findByPhone("   ")).thenThrow(new IllegalArgumentException("Delivery agent not found"));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> deliveryService.getOrCreateAgentByPhone("   ", "agent@mail.com")
        );

        assertEquals("Authenticated phone not found. Please login again.", ex.getMessage());
        verify(deliveryAgentRepository, never()).register(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void assign_shouldCreatePendingAssignmentAndReturnPendingResponse() {
        DeliveryAssignmentRequest request = new DeliveryAssignmentRequest("OD123", "Biryani House", "MG Road");

        var response = deliveryService.assign(request);

        assertEquals("OD123", response.orderReference());
        assertEquals("PENDING_ASSIGNMENT", response.assignmentStatus());
        verify(deliveryAgentRepository).createAssignment("OD123", null, "Biryani House", "MG Road");
    }

    @Test
    void getAssignedOrders_shouldMapRepositoryAssignments() {
        Instant now = Instant.now();
        AgentOrderAssignment assignment = new AgentOrderAssignment("OD999", "AGT1005", "Pizza Point", "Sector 21", "ACCEPTED", now, now, null, null);
        when(deliveryAgentRepository.findVisibleAssignmentsForAgent("AGT1005")).thenReturn(List.of(assignment));

        List<AgentAssignedOrderResponse> responses = deliveryService.getAssignedOrders("AGT1005");

        assertEquals(1, responses.size());
        assertEquals("OD999", responses.get(0).orderReference());
        assertEquals("ACCEPTED", responses.get(0).assignmentStatus());
    }

    @Test
    void getEarnings_shouldReturnCompletedCountAndCalculatedAmount() {
        when(deliveryAgentRepository.findById("AGT2001")).thenReturn(agent("AGT2001", "Ravi", "8888877777", "Scooter", "DL01AA1111", 3));

        AgentEarningsResponse response = deliveryService.getEarnings("AGT2001");

        assertEquals("AGT2001", response.agentId());
        assertEquals(3, response.completedDeliveries());
        assertEquals(120.0, response.estimatedEarnings());
    }

    private DeliveryAgent agent(String id, String name, String phone, String vehicleType, String vehicleNumber, int completedDeliveries) {
        return new DeliveryAgent(id, name, phone, vehicleType, vehicleNumber, true, true, true, null, completedDeliveries, Instant.now());
    }
}


