package com.gharelu.order_processing_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    private Long orderId;
    private String paymentStatus;  // e.g. "SUCCESS" or "FAILED"
}


