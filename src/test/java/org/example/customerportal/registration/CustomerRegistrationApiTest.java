package org.example.customerportal.registration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Story-level HTTP contract tests for US-001 Customer Registration.
 *
 * <p>Deliberately references no production type: the endpoint, DTOs, validator,
 * service and security configuration do not exist yet. Every scenario here
 * asserts the approved contract (Specification US-001 §5/§6, API design
 * {@code docs/designs/api/US-001-openapi.yaml}). Before IMPLEMENTATION these
 * fail because the behavior is not built; after IMPLEMENTATION they pass.
 *
 * <p>AC-test mapping is authoritative in {@code docs/tests/US-001-ac-test-matrix.md}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerRegistrationApiTest {

    private static final String PATH = "/api/v1/customers";

    /** 12 chars, one upper / lower / digit / special — the minimum-length compliant password. */
    private static final String VALID_PASSWORD = "Aa1!aaaaaaaa";

    @Autowired
    private MockMvc mockMvc;

    private static String body(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static String uniqueEmail(String tag) {
        return "user-" + tag + "-" + System.nanoTime() + "@example.com";
    }

    // ---------------------------------------------------------------- AC-001

    @Test
    void validRegistrationReturns201WithLocationAndCustomerBody() throws Exception {
        String email = uniqueEmail("ac001");

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, VALID_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/v1/customers/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void emailIsStoredAndReturnedNormalisedToLowercase() throws Exception {
        String mixedCase = "MixedCase-" + System.nanoTime() + "@Example.COM";

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(mixedCase, VALID_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(mixedCase.toLowerCase(Locale.ROOT)));
    }

    // ---------------------------------------------------------------- AC-005

    @Test
    void successResponseNeverExposesCredentialOrInternalState() throws Exception {
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(uniqueEmail("ac005"), VALID_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password_hash").doesNotExist())
                .andExpect(jsonPath("$.enabled").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }

    // ---------------------------------------------------------------- AC-002

    @Test
    void duplicateEmailIsRejectedCaseInsensitivelyWith409() throws Exception {
        String email = "dup-" + System.nanoTime() + "@example.com";

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, VALID_PASSWORD)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email.toUpperCase(Locale.ROOT), VALID_PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("An account with this email already exists."));
    }

    // ---------------------------------------------------------------- AC-003

    @Test
    void malformedEmailReturns400WithEmailFieldError() throws Exception {
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("not-an-email", VALID_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")));
    }

    @Test
    void blankEmailReturns400WithEmailFieldError() throws Exception {
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", VALID_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")));
    }

    @Test
    void emailLongerThan254CharsReturns400WithEmailFieldError() throws Exception {
        String local = "a".repeat(250);
        String tooLong = local + "@example.com"; // 262 chars

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(tooLong, VALID_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")));
    }

    // ---------------------------------------------------------------- AC-006

    @Test
    void passwordShorterThan12CharsReturns400WithPasswordFieldError() throws Exception {
        assertPasswordRejected("Aa1!aaaa"); // 8 chars
    }

    @Test
    void passwordWithoutUppercaseReturns400WithPasswordFieldError() throws Exception {
        assertPasswordRejected("aa1!aaaaaaaa");
    }

    @Test
    void passwordWithoutLowercaseReturns400WithPasswordFieldError() throws Exception {
        assertPasswordRejected("AA1!AAAAAAAA");
    }

    @Test
    void passwordWithoutDigitReturns400WithPasswordFieldError() throws Exception {
        assertPasswordRejected("Aaa!aaaaaaaa");
    }

    @Test
    void passwordWithoutSpecialCharReturns400WithPasswordFieldError() throws Exception {
        assertPasswordRejected("Aa1aaaaaaaaa");
    }

    @Test
    void blankPasswordReturns400WithPasswordFieldError() throws Exception {
        assertPasswordRejected("");
    }

    /**
     * R-3 / PD-2: the 12..72 bound is measured in UTF-8 <em>bytes</em>, not characters.
     * "Aa1!" + 23×"€" = 27 characters but 4 + 23×3 = 73 bytes — over the limit.
     */
    @Test
    void passwordOver72BytesButUnder72CharsReturns400WithPasswordFieldError() throws Exception {
        String multiByte = "Aa1!" + "€".repeat(23);
        int chars = multiByte.length();
        int bytes = multiByte.getBytes(StandardCharsets.UTF_8).length;
        org.junit.jupiter.api.Assertions.assertEquals(27, chars, "test vector should be 27 chars");
        org.junit.jupiter.api.Assertions.assertEquals(73, bytes, "test vector should be 73 bytes");

        assertPasswordRejected(multiByte);
    }

    /** Boundary: a 72-byte password that meets every class rule must be accepted. */
    @Test
    void password72CharsMeetingPolicyIsAccepted() throws Exception {
        String pw = "Aa1!" + "b".repeat(68); // 72 ASCII chars = 72 bytes

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(uniqueEmail("pw72"), pw)))
                .andExpect(status().isCreated());
    }

    private void assertPasswordRejected(String password) throws Exception {
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(uniqueEmail("pwpolicy"), password)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("password")));
    }

    /** SC-9: a validation message must never echo the submitted password. */
    @Test
    void passwordValidationMessageDoesNotEchoTheSubmittedValue() throws Exception {
        String secret = "supersecretweak"; // lowercase-only, fails policy, distinctive

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(uniqueEmail("noecho"), secret)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$..message").value(hasItem(matchesPattern("(?s)^(?!.*supersecretweak).*$"))));
    }

    // ---------------------------------------------------------------- AC-007

    @Test
    void nonJsonContentTypeReturns415() throws Exception {
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(body(uniqueEmail("ac007"), VALID_PASSWORD)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void missingContentTypeReturns415() throws Exception {
        mockMvc.perform(post(PATH)
                        .content(body(uniqueEmail("ac007b"), VALID_PASSWORD)))
                .andExpect(status().isUnsupportedMediaType());
    }

    // ------------------------------------------------ derived: request shape

    @Test
    void unknownJsonPropertyReturns400() throws Exception {
        String withExtra = "{\"email\":\"" + uniqueEmail("unknown")
                + "\",\"password\":\"" + VALID_PASSWORD + "\",\"role\":\"ADMIN\"}";

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withExtra))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"broken@example.com\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ------------------------------------------------ derived: AC-6 error body

    @Test
    void errorBodyHasTheApiConventionShape() throws Exception {
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("not-an-email", VALID_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value(PATH));
    }

    @Test
    void errorBodyNeverLeaksInternals() throws Exception {
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"broken@example.com\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(matchesPattern(
                        "(?s)^(?!.*(Exception|org\\.example|org\\.springframework|jdbc:h2|SELECT |INSERT )).*$")));
    }
}
