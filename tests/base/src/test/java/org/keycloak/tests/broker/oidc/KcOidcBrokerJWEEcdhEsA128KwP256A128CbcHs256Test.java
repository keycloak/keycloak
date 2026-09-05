package org.keycloak.tests.broker.oidc;

import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.broker.AbstractKcOidcEcdhEsJweBrokerTest;

@KeycloakIntegrationTest
public class KcOidcBrokerJWEEcdhEsA128KwP256A128CbcHs256Test extends AbstractKcOidcEcdhEsJweBrokerTest {

    @Override
    protected String getCurve() {
        return "P-256";
    }

    @Override
    protected String getEncAlg() {
        return JWEConstants.ECDH_ES_A128KW;
    }

    @Override
    protected String getEncEnc() {
        return JWEConstants.A128CBC_HS256;
    }

    @Override
    protected String getSigAlg() {
        return Algorithm.ES256;
    }
}
