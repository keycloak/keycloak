package org.keycloak.example;

/**
 * Client authentication with traditional OAuth2 client_id + client_secret
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ProductSAClientSecretServlet extends ProductServiceAccountServlet {

    @Override
    protected String getAdapterConfigLocation() {
        return "/WEB-INF/keycloak-client-secret.json";
    }

    @Override
    protected String getClientAuthenticationMethod() {
        return "secret";
    }
}
