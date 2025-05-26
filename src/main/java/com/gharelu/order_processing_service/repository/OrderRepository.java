package com.gharelu.order_processing_service.repository;

import com.gharelu.order_processing_service.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Orders, Long> {
}

