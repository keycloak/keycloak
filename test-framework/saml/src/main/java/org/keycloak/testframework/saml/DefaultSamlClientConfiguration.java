package org.keycloak.testframework.saml;

import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.testframework.realm.ClientBuilder;

public class DefaultSamlClientConfiguration implements SamlClientConfig {

    public static final String DEFAULT_SAML_CLIENT_ID = "http://localhost:8280/test-saml-app/";

    @Override
    public ClientBuilder configure(ClientBuilder client) {
        return client.clientId(DEFAULT_SAML_CLIENT_ID)
                .protocol("saml")
                .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, "true")
                .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false");
    }
}
