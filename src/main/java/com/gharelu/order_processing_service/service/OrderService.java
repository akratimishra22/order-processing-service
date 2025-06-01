package com.gharelu.order_processing_service.service;

import com.gharelu.order_processing_service.model.Orders;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    Orders placeOrder(Orders order);
    Optional<Orders> getOrderById(Long id);
    List<Orders> getOrdersByCustomerId(Long id);
    List<Orders> getAllOrders();
    void cancelOrder(Long id);

    Optional<Orders> updateStatus(Orders order);
}

