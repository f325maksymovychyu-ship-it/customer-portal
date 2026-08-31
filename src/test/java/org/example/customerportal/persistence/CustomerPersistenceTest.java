package org.example.customerportal.persistence;

import org.example.customerportal.model.entity.Customer;
import org.example.customerportal.model.entity.Role;
import org.example.customerportal.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence-slice tests that need the {@link Customer} entity (plan C-T2;
 * {@code DEFERRED → IMPLEMENTATION} rows §4 of
 * {@code docs/tests/US-001-ac-test-matrix.md}).
 *
 * <p>Covers: JPA-auditing populates UTC {@code created_at} / {@code updated_at};
 * {@code created_at} is not modified by a later update; {@code role} round-trips
 * as the {@code EnumType.STRING} name; and — implicitly, by the context booting
 * with {@code ddl-auto=validate} — the entity mapping agrees with
 * {@code schema.sql} (risk R-1 / PD-6).
 *
 * <p>Uses {@code @SpringBootTest} (not {@code @DataJpaTest}) so the datasource,
 * SQL-init and auditing configuration are exactly those of the running
 * application; deviation from plan skeleton C-T2 noted in the implementation
 * report.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerPersistenceTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private Customer newCustomer(String email) {
        Customer c = new Customer();
        c.setEmail(email);
        c.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        c.setRole(Role.CUSTOMER);
        c.setEnabled(true);
        return c;
    }

    @Test
    void auditingPopulatesBothTimestampsInUtc() {
        Customer saved = customerRepository.saveAndFlush(newCustomer("audit-" + System.nanoTime() + "@example.com"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(saved.getUpdatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void createdAtIsNotChangedByALaterUpdate() {
        Customer saved = customerRepository.saveAndFlush(newCustomer("immutable-" + System.nanoTime() + "@example.com"));
        Long id = saved.getId();
        OffsetDateTime createdAtBeforeUpdate = jdbc.queryForObject(
                "SELECT created_at FROM customer WHERE id = ?", OffsetDateTime.class, id);

        saved.setEmail("changed-" + System.nanoTime() + "@example.com");
        customerRepository.saveAndFlush(saved);

        OffsetDateTime createdAtAfterUpdate = jdbc.queryForObject(
                "SELECT created_at FROM customer WHERE id = ?", OffsetDateTime.class, id);
        assertThat(createdAtAfterUpdate).isEqualTo(createdAtBeforeUpdate);
    }

    @Test
    void roleIsPersistedAsTheEnumName() {
        Customer saved = customerRepository.saveAndFlush(newCustomer("role-" + System.nanoTime() + "@example.com"));

        String storedRole = jdbc.queryForObject(
                "SELECT role FROM customer WHERE id = ?", String.class, saved.getId());
        assertThat(storedRole).isEqualTo("CUSTOMER");

        Customer reloaded = customerRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getRole()).isEqualTo(Role.CUSTOMER);
    }
}
