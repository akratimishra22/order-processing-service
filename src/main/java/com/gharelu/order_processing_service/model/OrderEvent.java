package com.gharelu.order_processing_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class OrderEvent {

    private Long orderId;
    private Long productId;
    private int quantity;
    private Double amount;
    private String status;
}

