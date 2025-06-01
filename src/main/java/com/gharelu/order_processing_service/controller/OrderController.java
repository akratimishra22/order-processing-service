package com.gharelu.order_processing_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gharelu.order_processing_service.model.OrderEvent;
import com.gharelu.order_processing_service.model.Orders;
import com.gharelu.order_processing_service.model.ProductResponse;
import com.gharelu.order_processing_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;
    @Autowired
    ApplicationContext ctx;

    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String isTokenValid(String authHeader) {
        WebClient webClient = ctx.getBean("authServiceWebClientEurekaDiscovered", WebClient.class);
        String authResponse = webClient.get()
                .header("Authorization", authHeader)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return authResponse;
    }

    private Double getProductPrice(Long productId, String authHeader) {
        WebClient webClient = ctx.getBean("productCatalogServiceWebClientEurekaDiscovered", WebClient.class);
        ProductResponse productCatalogResponse = webClient.get()
                .uri("/{id}", productId)
                .header("Authorization", authHeader)
                .retrieve()
                .bodyToMono(ProductResponse.class)
                .block();
        return productCatalogResponse.getPrice();
    }

    @SneakyThrows
    @PostMapping
    public ResponseEntity<Orders> placeOrder(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                             @RequestBody Orders order) {
        String authResponse = isTokenValid(authHeader);
        if(authResponse.equals("Valid")) {
            double amount = getProductPrice(order.getProductId(), authHeader) * order.getQuantity();
            System.out.println("amount is"+amount);
            order.setTotalAmount(amount);
            service.placeOrder(order);
            OrderEvent event = OrderEvent.builder()
                    .orderId(order.getId())
                    .productId(order.getProductId())
                    .quantity(order.getQuantity())
                    .amount(order.getTotalAmount())
                    .status("CONFIRMED")
                    .build();
            //String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("order-created-topic", event);
            return ResponseEntity.ok(order);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Orders> getOrder(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                           @PathVariable Long id) {
        String authResponse = isTokenValid(authHeader);
        if(authResponse.equals("Valid"))
            return service.getOrderById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Orders>> getOrdersByCustomerId(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                              @PathVariable Long customerId) {
        String authResponse = isTokenValid(authHeader);
        if(authResponse.equals("Valid"))
            return ResponseEntity.ok(service.getOrdersByCustomerId(customerId));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping
    public ResponseEntity<List<Orders>> getAllOrders(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String authResponse = isTokenValid(authHeader);
        if(authResponse.equals("Valid"))
            return ResponseEntity.ok(service.getAllOrders());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                            @PathVariable Long id) {
        String authResponse = isTokenValid(authHeader);
        if(authResponse.equals("Valid")) {
            service.cancelOrder(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}

