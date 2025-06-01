package com.gharelu.order_processing_service.service;

import com.gharelu.order_processing_service.model.Orders;
import com.gharelu.order_processing_service.repository.OrderRepository;
import org.springframework.stereotype.Service;

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
    
    @Override
    public Optional<Orders> updateStatus(Orders order) {
        return repository.findById(order.getId())
                .map(existingOrder -> {
                    existingOrder.setStatus(order.getStatus());
                    existingOrder.setTotalAmount(existingOrder.getTotalAmount());
                    existingOrder.setCustomerId(existingOrder.getCustomerId());
                    existingOrder.setId(existingOrder.getId());
                    existingOrder.setQuantity(existingOrder.getQuantity());
                    existingOrder.setProductId(existingOrder.getProductId());
                    return repository.save(existingOrder);
                });
    }
}

