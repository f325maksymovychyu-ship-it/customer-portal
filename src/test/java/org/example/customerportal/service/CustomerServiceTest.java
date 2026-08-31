package org.example.customerportal.service;

import org.example.customerportal.exception.DuplicateEmailException;
import org.example.customerportal.exception.InvalidPasswordException;
import org.example.customerportal.model.dto.CustomerResponse;
import org.example.customerportal.model.entity.Customer;
import org.example.customerportal.model.entity.Role;
import org.example.customerportal.model.request.RegistrationRequest;
import org.example.customerportal.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Service-layer unit tests for registration (plan C-T3; AC-001 / AC-002 / AC-004
 * {@code DEFERRED → IMPLEMENTATION} rows of
 * {@code docs/tests/US-001-ac-test-matrix.md}).
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    // A real encoder: the tests assert a genuine BCrypt hash is produced and verifies.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private CustomerService service;

    private CustomerService service() {
        if (service == null) {
            service = new CustomerService(customerRepository, passwordEncoder);
        }
        return service;
    }

    private static final String RAW_PASSWORD = "Aa1!aaaaaaaa";

    @Test
    void happyPathCreatesAnEnabledCustomerWithABcryptHash() {
        when(customerRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CustomerResponse response = service().register(
                new RegistrationRequest("alice@example.com", RAW_PASSWORD));

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        Customer persisted = captor.getValue();

        assertThat(persisted.getEmail()).isEqualTo("alice@example.com");
        assertThat(persisted.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(persisted.isEnabled()).isTrue();
        assertThat(persisted.getPasswordHash())
                .matches("^\\$2[aby]\\$\\d\\d\\$.{53}$")
                .isNotEqualTo(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, persisted.getPasswordHash())).isTrue();

        assertThat(response.role()).isEqualTo("CUSTOMER");
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("alice@example.com");
    }

    @Test
    void emailIsNormalisedToLowercaseAndTrimmedBeforeCheckAndSave() {
        when(customerRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        service().register(new RegistrationRequest("  Alice@Example.COM  ", RAW_PASSWORD));

        verify(customerRepository).existsByEmail("alice@example.com");
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void duplicateEmailSameCaseThrowsAndDoesNotSave() {
        when(customerRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThatExceptionOfType(DuplicateEmailException.class).isThrownBy(() ->
                service().register(new RegistrationRequest("bob@example.com", RAW_PASSWORD)));

        verify(customerRepository, never()).save(any());
    }

    @Test
    void duplicateEmailDifferentCaseThrowsAndDoesNotSave() {
        when(customerRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThatExceptionOfType(DuplicateEmailException.class).isThrownBy(() ->
                service().register(new RegistrationRequest("BOB@Example.com", RAW_PASSWORD)));

        verify(customerRepository, never()).save(any());
    }

    @Test
    void serviceRechecksThePasswordPolicyBeforeHashingOrTouchingTheRepository() {
        assertThatExceptionOfType(InvalidPasswordException.class).isThrownBy(() ->
                service().register(new RegistrationRequest("carol@example.com", "weak")));

        verify(customerRepository, never()).existsByEmail(anyString());
        verify(customerRepository, never()).save(any());
    }
}
