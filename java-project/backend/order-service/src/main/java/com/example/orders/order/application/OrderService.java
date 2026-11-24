package com.example.orders.order.application;

import com.example.orders.inventory.InventoryClient;
import com.example.orders.inventory.InventoryClientException;
import com.example.orders.inventory.InventoryReservationItemRequest;
import com.example.orders.order.domain.OrderStatus;
import com.example.orders.order.persistence.OrderEntity;
import com.example.orders.order.persistence.OrderItemSnapshot;
import com.example.orders.order.persistence.OrderRepository;
import com.example.orders.order.presentation.CreateOrderRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    public OrderService(OrderRepository orderRepository, InventoryClient inventoryClient) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
    }

    public PlaceOrderResult create(UUID idempotencyKey, CreateOrderRequest request) {
        String requestFingerprint = fingerprint(request);
        var existing = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return resumeOrReturn(existing.get(), request, requestFingerprint);
        }

        OrderEntity pendingOrder;
        try {
            pendingOrder = orderRepository.saveAndFlush(new OrderEntity(
                    UUID.randomUUID(),
                    idempotencyKey,
                    requestFingerprint,
                    request.userId(),
                    request.customerName(),
                    request.customerEmail()
            ));
        } catch (DataIntegrityViolationException exception) {
            var concurrentOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (concurrentOrder.isEmpty()) {
                throw exception;
            }
            return resumeOrReturn(concurrentOrder.get(), request, requestFingerprint);
        }
        return new PlaceOrderResult(confirmPendingOrder(pendingOrder, request), true);
    }

    private PlaceOrderResult resumeOrReturn(
            OrderEntity order,
            CreateOrderRequest request,
            String requestFingerprint
    ) {
        if (!order.getRequestFingerprint().equals(requestFingerprint)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "La clave de idempotencia ya se usó con otra solicitud"
            );
        }
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            return new PlaceOrderResult(order, false);
        }
        if (order.getStatus() == OrderStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido ya fue rechazado");
        }
        return new PlaceOrderResult(confirmPendingOrder(order, request), false);
    }

    private OrderEntity confirmPendingOrder(OrderEntity order, CreateOrderRequest request) {
        List<InventoryReservationItemRequest> requestedItems = request.items().stream()
                .map(item -> new InventoryReservationItemRequest(item.productId(), item.quantity()))
                .toList();

        try {
            var reservation = inventoryClient.reserve(order.getId(), requestedItems);
            if (!order.getId().equals(reservation.reservationId())) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "La reserva de inventario no coincide con el pedido");
            }

            List<OrderItemSnapshot> items = reservation.items().stream()
                    .map(item -> new OrderItemSnapshot(
                            item.productId(), item.productName(), item.quantity(), item.unitPrice()
                    ))
                    .toList();
            BigDecimal total = items.stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.confirm(total, items);
            return orderRepository.save(order);
        } catch (InventoryClientException exception) {
            int statusCode = exception.getStatusCode().value();
            if (statusCode == HttpStatus.BAD_REQUEST.value()
                    || statusCode == HttpStatus.NOT_FOUND.value()
                    || statusCode == HttpStatus.CONFLICT.value()) {
                order.reject();
                orderRepository.save(order);
                throw new ResponseStatusException(
                        exception.getStatusCode(), "No se pudo reservar el inventario para este pedido"
                );
            }
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "El pedido está pendiente; inténtalo de nuevo más tarde"
            );
        }
    }

    private String fingerprint(CreateOrderRequest request) {
        String canonicalRequest = request.userId() + "\n"
                + request.customerName().length() + ":" + request.customerName() + "\n"
                + request.customerEmail().length() + ":" + request.customerEmail() + "\n"
                + request.items().stream()
                        .sorted(Comparator.comparing(item -> item.productId()))
                        .map(item -> item.productId() + ":" + item.quantity())
                        .collect(Collectors.joining(","));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
