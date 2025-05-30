package com.gharelu.order_processing_service.service;

import com.gharelu.order_processing_service.model.Orders;
import com.gharelu.order_processing_service.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository repository;

    public OrderServiceImpl(OrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Orders placeOrder(Orders order) {
        order.setStatus("PENDING");
        return repository.save(order);
    }

    @Override
    public Optional<Orders> getOrderById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Orders> getOrdersByCustomerId(Long id) {
        return repository.findAllByCustomerId(id);
    }

    @Override
    public List<Orders> getAllOrders() {
        return repository.findAll();
    }

    @Override
    public void cancelOrder(Long id) {
        repository.findById(id).ifPresent(order -> {
            order.setStatus("CANCELLED");
            repository.save(order);
        });
    }
}

