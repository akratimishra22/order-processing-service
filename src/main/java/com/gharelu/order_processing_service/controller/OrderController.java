package com.gharelu.order_processing_service.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gharelu.order_processing_service.model.InventoryResponse;
import com.gharelu.order_processing_service.model.OrderEvent;
import com.gharelu.order_processing_service.model.Orders;
import com.gharelu.order_processing_service.model.PaymentRequest;
import com.gharelu.order_processing_service.model.PaymentResponse;
import com.gharelu.order_processing_service.model.ProductResponse;
import com.gharelu.order_processing_service.service.OrderService;
import lombok.RequiredArgsConstructor;
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
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;
    @Autowired
    ApplicationContext ctx;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

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

    private Mono<Double> getProductPrice(Long productId, String authHeader) {
        WebClient webClient = ctx.getBean("productCatalogServiceWebClientEurekaDiscovered", WebClient.class);
        return webClient.get()
                .uri("/{id}", productId)
                .header("Authorization", authHeader)
                .retrieve()
                .bodyToMono(ProductResponse.class)
                .map(ProductResponse::getPrice);
    }

    private Mono<Boolean> getInventory(Long productId, Integer requestedQty, String authHeader) {
        WebClient webClient = ctx.getBean("inventoryServiceWebClientEurekaDiscovered", WebClient.class);
        return webClient.get()
                .uri("/{id}", productId)
                .header("Authorization", authHeader)
                .retrieve()
                .bodyToMono(InventoryResponse.class)
                .map(InventoryResponse::getAvailableQuantity)
                .map(inventory -> inventory >= requestedQty);
    }

    private Mono<Boolean> decrementInventory(Long productId, Integer requestedQty, String authHeader) {
        WebClient webClient = ctx.getBean("inventoryServiceWebClientEurekaDiscovered", WebClient.class);
        return webClient.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/decrement/{id}")
                        .queryParam("quantity", requestedQty)
                        .build(productId))
                .header("Authorization", authHeader)
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorReturn(false);
    }

    private Mono<Boolean> incrementInventory(Long productId, Integer requestedQty, String authHeader) {
        WebClient webClient = ctx.getBean("inventoryServiceWebClientEurekaDiscovered", WebClient.class);
        return webClient.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/increment/{id}")
                        .queryParam("quantity", requestedQty)
                        .build(productId))
                .header("Authorization", authHeader)
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorReturn(false);
    }


    @PostMapping
    public ResponseEntity<Orders> placeOrder(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                             @RequestBody Orders order) {

        String authResponse = isTokenValid(authHeader);
        if (!"Valid".equals(authResponse)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Double price = getProductPrice(order.getProductId(), authHeader).block();
        if (price == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Set tentative details and save order as PENDING
        order.setTotalAmount(price * order.getQuantity());
        order.setStatus("PENDING");
        Orders savedOrder = service.placeOrder(order); // save PENDING order with ID

        // Start async processing (fire & forget)
        processOrderAsync(savedOrder, authHeader);

        return ResponseEntity.ok(savedOrder);
    }

    private void processOrderAsync(Orders order, String authHeader) {
        // Run async in new thread or scheduler (or use reactor if you want)
        Mono.zip(
                        getInventory(order.getProductId(), order.getQuantity(), authHeader),
                        makePayment(order.getId(), order.getTotalAmount(), authHeader),
                        decrementInventory(order.getProductId(), order.getQuantity(), authHeader)
                )
                .flatMap(tuple -> {
                    Boolean inStock = tuple.getT1();
                    String paymentStatus = tuple.getT2();
                    Boolean inventoryDecremented = tuple.getT3();

                    if (!inStock) {
                        order.setStatus("FAILED");
                    } else if (!"COMPLETED".equals(paymentStatus)) {
                        // revert inventory if decremented
                        if (Boolean.TRUE.equals(inventoryDecremented)) {
                            return incrementInventory(order.getProductId(), order.getQuantity(), authHeader)
                                    .then(Mono.fromRunnable(() -> order.setStatus("FAILED")));
                        } else {
                            order.setStatus("FAILED");
                        }
                    } else {
                        order.setStatus("COMPLETED");
                    }

                    emitEvent(order, order.getStatus());
                    service.updateStatus(order); // update DB with final status
                    return Mono.empty();
                })
                .subscribe();
    }


    private Mono<String> makePayment(Long orderId, Double amount, String authHeader) {
        WebClient webClient = ctx.getBean("paymentServiceWebClientEurekaDiscovered", WebClient.class);
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(orderId)
                .amount(amount)
                .paymentMethod("COD")
                .build();
        return webClient.post()
                .bodyValue(paymentRequest)
                .header("Authorization", authHeader)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .flatMap(paymentResponse -> {
                    if ("SUCCESS".equals(paymentResponse.getPaymentStatus())) {
                        return Mono.just("COMPLETED");
                    } else {
                        return Mono.just("FAILED");
                    }
                });
    }

    private void emitEvent(Orders order, String status) {
        OrderEvent event = OrderEvent.builder()
                .orderId(order.getId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .amount(order.getTotalAmount())
                .status(status)
                .build();

        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("order-created-topic", "1", json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Kafka event", e);
        }
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

    @PutMapping("/{id}/update-status")
    public ResponseEntity<Void> updateStatusOrder(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                  @RequestBody Orders order) {
        String authResponse = isTokenValid(authHeader);
        if(authResponse.equals("Valid")) {
            service.updateStatus(order);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}

