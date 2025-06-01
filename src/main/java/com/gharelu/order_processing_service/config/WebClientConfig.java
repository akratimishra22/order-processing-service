package com.gharelu.order_processing_service.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Configuration
public class WebClientConfig {

    @Autowired
    EurekaDiscoveryClient discoveryClient;

    @Bean
    @Scope(value = "prototype")
    public WebClient authServiceWebClientEurekaDiscovered(WebClient.Builder webClientBuilder) {
        List<ServiceInstance> instances = discoveryClient.getInstances("auth-service");

        if(instances.isEmpty()){
            throw new RuntimeException("No instances found for auth-service");
        }

        String hostname = instances.get(0).getHost();
        String port = String.valueOf(instances.get(0).getPort());

        return webClientBuilder
                .baseUrl(String.format("http://%s:%s/api/v1/validate-token", hostname, port))
                .build();
    }

    @Bean
    @Scope(value = "prototype")
    public WebClient productCatalogServiceWebClientEurekaDiscovered(WebClient.Builder webClientBuilder) {
        List<ServiceInstance> instances = discoveryClient.getInstances("product-catalog-service");

        if(instances.isEmpty()){
            throw new RuntimeException("No instances found for product-catalog-service");
        }

        String hostname = instances.get(0).getHost();
        String port = String.valueOf(instances.get(0).getPort());

        return webClientBuilder
                .baseUrl(String.format("http://%s:%s/products", hostname, port))
                .build();
    }

    @Bean
    @Scope(value = "prototype")
    public WebClient inventoryServiceWebClientEurekaDiscovered(WebClient.Builder webClientBuilder) {
        List<ServiceInstance> instances = discoveryClient.getInstances("inventory-service");

        if(instances.isEmpty()){
            throw new RuntimeException("No instances found for inventory-service");
        }

        String hostname = instances.get(0).getHost();
        String port = String.valueOf(instances.get(0).getPort());

        return webClientBuilder
                .baseUrl(String.format("http://%s:%s/inventory", hostname, port))
                .build();
    }

    @Bean
    @Scope(value = "prototype")
    public WebClient paymentServiceWebClientEurekaDiscovered(WebClient.Builder webClientBuilder) {
        List<ServiceInstance> instances = discoveryClient.getInstances("payment-service");

        if(instances.isEmpty()){
            throw new RuntimeException("No instances found for payment-service");
        }

        String hostname = instances.get(0).getHost();
        String port = String.valueOf(instances.get(0).getPort());

        return webClientBuilder
                .baseUrl(String.format("http://%s:%s/payment", hostname, port))
                .build();
    }
}
