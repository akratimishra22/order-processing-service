package com.gharelu.order_processing_service.controller;

import com.gharelu.order_processing_service.model.Orders;
import com.gharelu.order_processing_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @Qualifier("authServiceWebClient")
    private final WebClient webClient;

    private String isTokenValid(String authHeader) {
        String authResponse = webClient.get()
                .header("Authorization", authHeader)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return authResponse;
    }

    @PostMapping
    public ResponseEntity<Orders> placeOrder(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                             @RequestBody Orders order) {
        String authResponse = isTokenValid(authHeader);
        if(authResponse.equals("Valid"))
            return ResponseEntity.ok(service.placeOrder(order));
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

