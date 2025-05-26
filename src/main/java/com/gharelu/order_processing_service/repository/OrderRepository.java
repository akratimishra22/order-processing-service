package com.gharelu.order_processing_service.repository;

import com.gharelu.order_processing_service.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Orders, Long> {
    List<Orders> findAllByCustomerId(Long customerId);
}

