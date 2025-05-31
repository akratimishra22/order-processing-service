package com.gharelu.order_processing_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent {

    private Long orderId;
    private Long productId;
    private int quantity;
    private Double amount;
    private String status;
}

