package org.keycloak.tests.broker.oidc;

import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.broker.AbstractKcOidcEcdhEsJweBrokerTest;

@KeycloakIntegrationTest
public class KcOidcBrokerJWEEcdhEsP384A192GcmTest extends AbstractKcOidcEcdhEsJweBrokerTest {

    @Override
    protected String getCurve() {
        return "P-384";
    }

    @Override
    protected String getEncAlg() {
        return JWEConstants.ECDH_ES;
    }

    @Override
    protected String getEncEnc() {
        return JWEConstants.A192GCM;
    }

    @Override
    protected String getSigAlg() {
        return Algorithm.ES384;
    }
}
