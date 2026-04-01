package org.keycloak.services.util;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.keycloak.common.Profile;
import org.keycloak.common.VerificationException;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resteasy.HttpRequestImpl;
import org.keycloak.services.resteasy.ResteasyKeycloakSession;
import org.keycloak.services.resteasy.ResteasyKeycloakSessionFactory;

import org.jboss.resteasy.mock.MockHttpRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DPoPUtilTests {
    private static final String URL = "http://localhost/test";
    private static final long IAT = 1_000_000_000L;

    private static KeycloakSession session;

    @BeforeAll
    public static void beforeAll() {
        Profile.defaults();
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
        ResteasyKeycloakSessionFactory sessionFactory = new ResteasyKeycloakSessionFactory();
        sessionFactory.init();
        session = new ResteasyKeycloakSession(sessionFactory);
    }

    @Test
    public void testRejectsUnsupportedJwkKeyType() {
        String jwkJson = "{\"kty\":\"foo\",\"kid\":\"invalid-kid\",\"alg\":\"RS256\"}";
        String headerJson = "{\"typ\":\"dpop+jwt\",\"alg\":\"RS256\",\"jwk\":" + jwkJson + "}";
        String payloadJson = "{\"jti\":\"test-jti\",\"htm\":\"GET\",\"htu\":\"http://localhost/test\",\"iat\":" + IAT + "}";

        String header = encode(headerJson);
        String payload = encode(payloadJson);
        String dpop = header + "." + payload + "." + "sig";

        createHttpRequest("GET", dpop);

        DPoPUtil.Validator validator = new DPoPUtil.Validator(session)
                .request(session.getContext().getHttpRequest())
                .uriInfo(session.getContext().getUri());

        VerificationException thrown = assertThrows(VerificationException.class, validator::validate);
        assertThat(thrown.getMessage(), containsString("Unsupported key type"));
    }

    private static void createHttpRequest(String httpMethod, String dpop) {
        MockHttpRequest mockRequest = MockHttpRequest.create(httpMethod, URI.create(URL), URI.create(URL));
        mockRequest.header("DPoP", dpop);
        HttpRequest httpRequest = new HttpRequestImpl(mockRequest);
        session.getContext().setHttpRequest(httpRequest);
    }

    private static String encode(String data) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }
}
