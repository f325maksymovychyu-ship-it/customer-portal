package org.example.customerportal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Enables JPA auditing so {@code @CreatedDate} / {@code @LastModifiedDate} on
 * {@link org.example.customerportal.model.entity.Customer} are populated
 * automatically (persistence-conventions.md PC-6).
 *
 * <p>The {@link DateTimeProvider} is pinned to UTC (business-rules.md BR-007,
 * risk R-6). Auditing is enabled here rather than on the main application class
 * (architecture.md AD-7).
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider utcDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now(ZoneOffset.UTC));
    }
}
