package org.example.customerportal.repository;

import org.example.customerportal.model.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistence access for {@link Customer}. Queries only — no business logic
 * (architecture.md AD-2, package-map.md).
 *
 * <p>The email arguments are expected to be already normalized to lowercase by
 * the service (OD-006:A); {@code uq_customer_email} then enforces
 * case-insensitive uniqueness (business-rules.md BR-001 / BR-002).
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmail(String email);

    Optional<Customer> findByEmail(String email);
}
