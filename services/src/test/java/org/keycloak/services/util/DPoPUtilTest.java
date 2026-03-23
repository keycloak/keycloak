package org.keycloak.services.util;

import java.net.URI;
import java.util.UUID;
import java.util.stream.Stream;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.dpop.DPoP;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DPoPUtilTest {

    @Mock
    private KeycloakSession session;

    @Mock
    private KeycloakSessionFactory sessionFactory;

    @Mock
    private SignatureProvider signatureProvider;

    @Mock
    private TokenVerifier<DPoP> verifier;

    @Mock
    private ProviderFactory<SignatureProvider> signatureProviderFactory;

    @BeforeEach
    public void setup() throws Exception {
        // Mock session factory and provider factories
        when(session.getKeycloakSessionFactory()).thenReturn(sessionFactory);
        when(signatureProviderFactory.getId()).thenReturn(Algorithm.RS256);
        when(sessionFactory.getProviderFactoriesStream(SignatureProvider.class))
                .thenReturn(Stream.of(signatureProviderFactory));
        
        // Mock signature provider to be asymmetric
        when(signatureProvider.isAsymmetricAlgorithm()).thenReturn(true);
        // Mock session behavior
        when(session.getProvider(eq(SignatureProvider.class), anyString())).thenReturn(signatureProvider);
    }

    @Test
    public void testValidateDPoP_ThrowExceptionForUnSupportedJWKKeyType() throws Exception {

        URI uri = new URI("https://example.com/token");
        String expectedMethod = "POST";
        String actualMethod = "GET";

        DPoP dpop = new DPoP();
        dpop.id(UUID.randomUUID().toString());
        dpop.setHttpMethod(actualMethod);
        dpop.setHttpUri(uri.toString());

        JWK jwk = createJWK();
        String token = getDPoPMockToken();
        JWSHeader header = mock(JWSHeader.class);

        try(MockedStatic<TokenVerifier> mocked = mockStatic(TokenVerifier.class)){

            mocked.when((MockedStatic.Verification) TokenVerifier.create(any(), any())).thenReturn(verifier);
            when(verifier.getHeader()).thenReturn(header);
            when(header.getType()).thenReturn("dpop+jwt");
            when(header.getAlgorithm()).thenReturn(org.keycloak.jose.jws.Algorithm.RS256);
            when(header.getKey()).thenReturn(jwk);

            DPoPUtil.Validator validator = new DPoPUtil.Validator(session);

            Exception exception =  assertThrows(VerificationException.class, () -> validator.uri(uri.toString())
                    .method(expectedMethod)
                    .dPoP(token)
                    .validate());

            assertEquals("Unsupported or invalid JWK", exception.getMessage());
        }
    }


    private JWK createJWK() {
        JWK jwk = new JWK();
        jwk.setKeyType("UNKNOWN_KEY_TYPE");
        jwk.setAlgorithm(Algorithm.RS256);
        jwk.setPublicKeyUse(KeyUse.SIG.getSpecName());
        jwk.setKeyId(UUID.randomUUID().toString());
        return jwk;
    }

    private String getDPoPMockToken() {
        return "eyJhbGciOiJSUzI1NiIsInR5cCI6ImRwb3Arand0IiwiandrIjp7Imt0eSI6IlJTQSJ9fQ.eyJqdGkiOiJ0ZXN0IiwiaHRtIjoiUE9TVCIsImh0dSI6Imh0dHBzOi8vZXhhbXBsZS5jb20vdG9rZW4iLCJpYXQiOjE2MzAwMDAwMDB9.signature";
    }
}
