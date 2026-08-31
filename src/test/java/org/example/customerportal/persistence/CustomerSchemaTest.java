package org.example.customerportal.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence-schema tests for US-001, asserted directly against H2's
 * INFORMATION_SCHEMA so they need no entity class.
 *
 * <p>Source of truth: {@code docs/designs/database/US-001-db-design.md} §4.2 / §8.1
 * and {@code docs/designs/database/US-001-entity-model.md} §2. Before IMPLEMENTATION
 * these fail because {@code src/main/resources/schema.sql} and the {@code customer}
 * table do not exist yet; after IMPLEMENTATION (with {@code ddl-auto=validate} and
 * the hand-written schema) they pass.
 */
@SpringBootTest
@ActiveProfiles("test")
class CustomerSchemaTest {

    @Autowired
    private JdbcTemplate jdbc;

    private String columnType(String column) {
        List<String> rows = jdbc.queryForList(
                "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE UPPER(TABLE_NAME) = 'CUSTOMER' AND UPPER(COLUMN_NAME) = ?",
                String.class, column.toUpperCase(Locale.ROOT));
        return rows.isEmpty() ? null : rows.get(0).toUpperCase(Locale.ROOT);
    }

    private Integer charLength(String column) {
        List<Integer> rows = jdbc.queryForList(
                "SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE UPPER(TABLE_NAME) = 'CUSTOMER' AND UPPER(COLUMN_NAME) = ?",
                Integer.class, column.toUpperCase(Locale.ROOT));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private boolean isNullable(String column) {
        List<String> rows = jdbc.queryForList(
                "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE UPPER(TABLE_NAME) = 'CUSTOMER' AND UPPER(COLUMN_NAME) = ?",
                String.class, column.toUpperCase(Locale.ROOT));
        return rows.isEmpty() || "YES".equalsIgnoreCase(rows.get(0));
    }

    @Test
    void customerTableExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = 'CUSTOMER'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void idColumnIsBigintNotNull() {
        assertThat(columnType("id")).contains("BIGINT");
        assertThat(isNullable("id")).isFalse();
    }

    @Test
    void emailColumnIsVarchar254NotNull() {
        assertThat(columnType("email")).contains("CHARACTER VARYING");
        assertThat(charLength("email")).isEqualTo(254);
        assertThat(isNullable("email")).isFalse();
    }

    @Test
    void passwordHashColumnIsVarchar60NotNull() {
        assertThat(columnType("password_hash")).contains("CHARACTER VARYING");
        assertThat(charLength("password_hash")).isEqualTo(60);
        assertThat(isNullable("password_hash")).isFalse();
    }

    @Test
    void noPlaintextPasswordColumnExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE UPPER(TABLE_NAME) = 'CUSTOMER' AND UPPER(COLUMN_NAME) = 'PASSWORD'",
                Integer.class);
        assertThat(count).isZero();
    }

    @Test
    void roleColumnIsNotNull() {
        assertThat(columnType("role")).contains("CHARACTER VARYING");
        assertThat(isNullable("role")).isFalse();
    }

    @Test
    void enabledColumnIsBooleanNotNull() {
        assertThat(columnType("enabled")).contains("BOOLEAN");
        assertThat(isNullable("enabled")).isFalse();
    }

    @Test
    void auditTimestampColumnsAreTimeZoneAwareAndNotNull() {
        assertThat(columnType("created_at")).contains("TIMESTAMP WITH TIME ZONE");
        assertThat(columnType("updated_at")).contains("TIMESTAMP WITH TIME ZONE");
        assertThat(isNullable("created_at")).isFalse();
        assertThat(isNullable("updated_at")).isFalse();
    }

    @Test
    void emailHasAUniqueConstraint() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc "
                        + "JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu "
                        + "  ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME "
                        + "WHERE UPPER(tc.TABLE_NAME) = 'CUSTOMER' "
                        + "  AND tc.CONSTRAINT_TYPE = 'UNIQUE' "
                        + "  AND UPPER(kcu.COLUMN_NAME) = 'EMAIL'",
                Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void caseInsensitiveDuplicateEmailCollidesOnTheUniqueConstraint() {
        jdbc.update("INSERT INTO customer (email, password_hash, role, enabled, created_at, updated_at) "
                + "VALUES ('collide@example.com', '"
                + "$2a$10$abcdefghijklmnopqrstuvabcdefghijklmnopqrstuvwxyz012345"
                + "', 'CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        // The service lower-cases before insert (OD-006:A); a second row with the
        // same normalised address must violate uq_customer_email.
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                jdbc.update("INSERT INTO customer (email, password_hash, role, enabled, created_at, updated_at) "
                        + "VALUES ('collide@example.com', '"
                        + "$2a$10$abcdefghijklmnopqrstuvabcdefghijklmnopqrstuvwxyz012345"
                        + "', 'CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"));
    }
}
