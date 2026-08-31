package org.example.customerportal.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security-posture tests for US-001 (Specification §7, API design §6, OD-002:B).
 *
 * <p>Encodes the plan's recommended entry-point choice (F-3 / R-2): unauthenticated
 * access to a protected route returns {@code 401}, not a form-login {@code 302}.
 * If IMPLEMENTATION adopts form login instead, {@link #protectedRouteReturns401WhenUnauthenticated}
 * fails and SECURITY_REVIEW arbitrates per risk R-2.
 *
 * <p>Full "CSRF still enforced everywhere else" verification is SECURITY_REVIEW's
 * (R-2); US-001 introduces no second writable endpoint to assert it against here.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationSecurityPostureTest {

    private static final String PATH = "/api/v1/customers";
    private static final String VALID_BODY =
            "{\"email\":\"posture@example.com\",\"password\":\"Aa1!aaaaaaaa\"}";

    @Autowired
    private MockMvc mockMvc;

    /** SEC-1: registration is the one public endpoint — it must not require authentication. */
    @Test
    void registrationEndpointIsReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().is(org.hamcrest.Matchers.not(401)))
                .andExpect(status().is(org.hamcrest.Matchers.not(403)));
    }

    /** OD-002:B / SC-5: the CSRF exemption covers POST /api/v1/customers, so a
     *  tokenless POST is not rejected for CSRF. */
    @Test
    void registrationPostIsAcceptedWithoutACsrfToken() throws Exception {
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().is(org.hamcrest.Matchers.not(403)));
    }

    /** SC-4 / SEC-1: every other route stays deny-by-default. */
    @Test
    void protectedRouteReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(PATH + "/1"))
                .andExpect(status().isUnauthorized());
    }

    /** SC-6 / SEC-9: the H2 console is never exposed. */
    @Test
    void h2ConsoleIsNotExposed() throws Exception {
        mockMvc.perform(get("/h2-console"))
                .andExpect(status().is(org.hamcrest.Matchers.not(200)));
    }
}
