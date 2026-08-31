package org.example.customerportal.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the password policy (plan C-T1; AC-006 exhaustive row of
 * {@code docs/tests/US-001-ac-test-matrix.md}).
 *
 * <p>Binds the byte-length boundary (PD-2 / risk R-3): the 12..72 bound is
 * measured in UTF-8 <em>bytes</em>, not characters. Also covers each
 * character-class rule and the no-value-echo message requirement (SC-9 / R-7).
 */
class PasswordPolicyValidatorTest {

    private final PasswordPolicyValidator validator = new PasswordPolicyValidator();

    // -------------------------------------------------------------- byte-length boundary

    @Test
    void elevenBytesIsRejected() {
        String pw = "Aa1!" + "a".repeat(7); // 11 bytes
        assertThat(byteLength(pw)).isEqualTo(11);
        assertThat(PasswordPolicyValidator.isCompliant(pw)).isFalse();
    }

    @Test
    void twelveBytesIsAccepted() {
        String pw = "Aa1!" + "a".repeat(8); // 12 bytes
        assertThat(byteLength(pw)).isEqualTo(12);
        assertThat(PasswordPolicyValidator.isCompliant(pw)).isTrue();
    }

    @Test
    void seventyTwoBytesIsAccepted() {
        String pw = "Aa1!" + "a".repeat(68); // 72 bytes
        assertThat(byteLength(pw)).isEqualTo(72);
        assertThat(PasswordPolicyValidator.isCompliant(pw)).isTrue();
    }

    @Test
    void seventyThreeBytesIsRejected() {
        String pw = "Aa1!" + "a".repeat(69); // 73 bytes
        assertThat(byteLength(pw)).isEqualTo(73);
        assertThat(PasswordPolicyValidator.isCompliant(pw)).isFalse();
    }

    @Test
    void multiByteStringOver72BytesButUnder72CharsIsRejected() {
        String pw = "Aa1!" + "€".repeat(23); // 4 + 69 = 73 bytes, 27 chars
        assertThat(pw.length()).isEqualTo(27);
        assertThat(byteLength(pw)).isEqualTo(73);
        assertThat(PasswordPolicyValidator.isCompliant(pw)).isFalse();
    }

    @Test
    void multiByteStringAtOrUnder72BytesIsAccepted() {
        String pw = "Aa1!" + "€".repeat(22) + "aa"; // 4 + 66 + 2 = 72 bytes
        assertThat(byteLength(pw)).isEqualTo(72);
        assertThat(PasswordPolicyValidator.isCompliant(pw)).isTrue();
    }

    // -------------------------------------------------------------- character classes

    @ParameterizedTest
    @ValueSource(strings = {
            "aa1!aaaaaaaa", // no uppercase
            "AA1!AAAAAAAA", // no lowercase
            "Aaa!aaaaaaaa", // no digit
            "Aa1aaaaaaaaa"  // no special
    })
    void missingACharacterClassIsRejected(String password) {
        assertThat(PasswordPolicyValidator.isCompliant(password)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "Aa1!aaaaaaaa, true",
            "'', false",
            "Aa1!bbbbbbbb, true"
    })
    void compliantFlagMatchesExpectation(String password, boolean expected) {
        assertThat(PasswordPolicyValidator.isCompliant(password)).isEqualTo(expected);
    }

    @Test
    void nullIsNotCompliant() {
        assertThat(PasswordPolicyValidator.isCompliant(null)).isFalse();
    }

    // -------------------------------------------------------------- @NotBlank delegation

    @Test
    void isValidLeavesNullAndEmptyToNotBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
    }

    @Test
    void isValidRejectsANonCompliantValue() {
        assertThat(validator.isValid("short", null)).isFalse();
    }

    // -------------------------------------------------------------- SC-9: no value echo

    @Test
    void defaultMessageNeverEchoesASubmittedValue() throws Exception {
        String message = (String) ValidPassword.class.getMethod("message").getDefaultValue();
        assertThat(message).isNotBlank();
        assertThat(message).doesNotContain("Sup3rSecret!Value");
        // The message must be a fixed, generic string with no interpolation of input.
        assertThat(message).isEqualTo("Password does not meet the security policy.");
    }

    private static int byteLength(String s) {
        return s.getBytes(StandardCharsets.UTF_8).length;
    }
}
