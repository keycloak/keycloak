package org.keycloak.testsuite.auth.page.login;

import jakarta.ws.rs.core.UriBuilder;

/**
 * @author mhajas
 */
public class SAMLIDPInitiatedLogin extends SAMLRedirectLogin {

    public void setUrlName(String urlName) {
        setUriParameter("clientUrlName", urlName);
    }

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("clients/{clientUrlName}");
    }
}
