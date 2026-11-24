package com.example.products.inventory.presentation;

import com.example.products.inventory.application.InventoryReservationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/internal/v1/inventory/reservations")
public class InventoryReservationController {

    private final InventoryReservationService service;
    private final String serviceApiKey;

    public InventoryReservationController(
            InventoryReservationService service,
            @Value("${inventory.service-api-key:}") String serviceApiKey
    ) {
        this.service = service;
        this.serviceApiKey = serviceApiKey;
    }

    @PostMapping
    public ResponseEntity<InventoryReservationResponse> reserve(
            @RequestHeader(name = "X-Inventory-Service-Key", required = false) String apiKey,
            @Valid @RequestBody ReserveInventoryRequest request
    ) {
        if (serviceApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Inventory service key is not configured");
        }
        if (apiKey == null || !MessageDigest.isEqual(
                serviceApiKey.getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid inventory service key");
        }
        return ResponseEntity.ok(InventoryReservationResponse.from(service.reserve(request)));
    }
}
