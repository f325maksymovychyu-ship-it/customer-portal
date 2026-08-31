package org.example.customerportal.controller;

import jakarta.validation.Valid;
import org.example.customerportal.model.dto.CustomerResponse;
import org.example.customerportal.model.request.RegistrationRequest;
import org.example.customerportal.service.CustomerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * HTTP entry point for customer registration
 * ({@code POST /api/v1/customers}). Binds and validates the request, delegates
 * to {@link CustomerService}, and maps the outcome to {@code 201} + a
 * {@code Location} header (api-conventions.md AC-4, FR-1 / FR-8).
 *
 * <p>No business logic, no repository access, no entity in a signature
 * (architecture.md AD-2 / AD-4). Error responses are produced by
 * {@link org.example.customerportal.exception.GlobalExceptionHandler}, not here.
 */
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CustomerResponse> register(@Valid @RequestBody RegistrationRequest request) {
        CustomerResponse created = customerService.register(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
}
