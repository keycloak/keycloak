package org.keycloak.tests.webauthn;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

@KeycloakIntegrationTest(config = WebAuthnMLDSATest.ServerConfig.class)
public class WebAuthnMLDSASignatureAlgorithmsTest extends WebAuthnSignatureAlgorithmsTest {

    @Override
    protected List<String> expectedAlgorithms() {
        List<String> algorithms = new ArrayList<>(super.expectedAlgorithms());
        algorithms.addAll(List.of("ML-DSA-44", "ML-DSA-65", "ML-DSA-87"));
        return algorithms;
    }
}
