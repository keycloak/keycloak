package org.keycloak.testsuite.broker;

import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.services.resources.RealmsResource;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import java.net.URI;

public abstract class AbstractSamlBrokerTest extends AbstractInitializedBaseBrokerTest {

    protected URI getAuthServerSamlEndpoint(String realm) throws IllegalArgumentException, UriBuilderException {
        return RealmsResource
                .protocolUrl(UriBuilder.fromUri(getAuthServerRoot()))
                .build(realm, SamlProtocol.LOGIN_PROTOCOL);
    }
}
