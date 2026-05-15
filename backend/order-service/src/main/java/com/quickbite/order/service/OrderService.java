package com.quickbite.order.service;

import com.quickbite.order.dto.CheckoutRequest;
import com.quickbite.order.dto.OrderResponse;
import java.util.List;
public interface OrderService {

    OrderResponse checkout(String userEmail, CheckoutRequest request);

    List<OrderResponse> getOrders(String userEmail, String role);

    OrderResponse getOrder(String userEmail, String orderReference);

    List<OrderResponse> getAllOrdersForAdmin();

    long getTotalOrders();

    OrderResponse updateOrderStatusForAdmin(String orderReference, String status);

    OrderResponse updateOrderDeliveryStatusInternal(String orderReference, String status);

    OrderResponse updateOrderDeliveryStatusForAgent(String orderReference, String status);

    OrderResponse updateDeliveryAgentInternal(String orderReference, String agentId, String agentName, int etaMinutes);
}
