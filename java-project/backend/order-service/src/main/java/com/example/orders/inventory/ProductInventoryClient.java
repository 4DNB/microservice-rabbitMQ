package com.example.orders.inventory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
public class ProductInventoryClient implements InventoryClient {

    private final RestClient restClient;
    private final String serviceApiKey;

    public ProductInventoryClient(
            RestClient.Builder builder,
            @Value("${services.products.base-url}") String productsBaseUrl,
            @Value("${services.products.connect-timeout:2s}") Duration connectTimeout,
            @Value("${services.products.read-timeout:3s}") Duration readTimeout,
            @Value("${services.products.api-key:}") String serviceApiKey
    ) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = builder
                .baseUrl(productsBaseUrl)
                .requestFactory(requestFactory)
                .build();
        this.serviceApiKey = serviceApiKey;
    }

    @Override
    public InventoryReservationResponse reserve(UUID reservationId, List<InventoryReservationItemRequest> items) {
        try {
            InventoryReservationResponse response = restClient.post()
                    .uri("/internal/v1/inventory/reservations")
                    .header("X-Inventory-Service-Key", serviceApiKey)
                    .body(new InventoryReservationRequest(reservationId, items))
                    .retrieve()
                    .body(InventoryReservationResponse.class);
            if (response == null) {
                throw new InventoryClientException(HttpStatus.BAD_GATEWAY, "Inventory service returned an empty response", null);
            }
            return response;
        } catch (RestClientResponseException exception) {
            throw new InventoryClientException(
                    exception.getStatusCode(), "Inventory reservation request was rejected", exception
            );
        } catch (RestClientException exception) {
            throw new InventoryClientException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Inventory service is unavailable", exception
            );
        }
    }
}
