package org.keycloak.adapters.saml.profile;

import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.spi.HttpFacade;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class SamlInvocationContext {

    private String samlRequest;
    private String samlResponse;
    private String relayState;

    public SamlInvocationContext() {
        this(null, null, null);
    }

    public SamlInvocationContext(String samlRequest, String samlResponse, String relayState) {
        this.samlRequest = samlRequest;
        this.samlResponse = samlResponse;
        this.relayState = relayState;
    }

    public String getSamlRequest() {
        return this.samlRequest;
    }

    public String getSamlResponse() {
        return this.samlResponse;
    }

    public String getRelayState() {
        return this.relayState;
    }
}
