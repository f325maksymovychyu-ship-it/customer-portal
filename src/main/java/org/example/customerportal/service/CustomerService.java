package org.example.customerportal.service;

import org.example.customerportal.exception.DuplicateEmailException;
import org.example.customerportal.exception.InvalidPasswordException;
import org.example.customerportal.model.dto.CustomerResponse;
import org.example.customerportal.model.entity.Customer;
import org.example.customerportal.model.entity.Role;
import org.example.customerportal.model.request.RegistrationRequest;
import org.example.customerportal.repository.CustomerRepository;
import org.example.customerportal.validation.PasswordPolicyValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Registration business logic (architecture.md AD-2 / AD-3 / AD-4).
 *
 * <p>Normalizes the email to lowercase (OD-006:A), re-checks the password policy
 * in bytes before hashing (FR-6, PD-2), rejects a duplicate email
 * (OD-003:A), BCrypt-hashes the password (SC-1), and persists an enabled
 * {@code CUSTOMER} account with UTC audit timestamps. Entity ↔ DTO mapping is
 * done here (PD-3).
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CustomerResponse register(RegistrationRequest request) {
        String email = normalizeEmail(request.email());
        String password = request.password();

        // FR-6 / SC-1: service-layer policy re-check before hashing (defense in depth).
        if (!PasswordPolicyValidator.isCompliant(password)) {
            throw new InvalidPasswordException();
        }

        if (customerRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setPasswordHash(passwordEncoder.encode(password));
        customer.setRole(Role.CUSTOMER);
        customer.setEnabled(true);

        return toResponse(customerRepository.save(customer));
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getEmail(),
                customer.getRole().name(),
                customer.getCreatedAt());
    }
}
