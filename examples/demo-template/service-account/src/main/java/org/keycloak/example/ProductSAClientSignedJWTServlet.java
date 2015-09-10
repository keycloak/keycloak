package org.keycloak.example;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ProductSAClientSignedJWTServlet extends ProductServiceAccountServlet {

    @Override
    protected String getAdapterConfigLocation() {
        return "/WEB-INF/keycloak-client-signed-jwt.json";
    }

    @Override
    protected String getClientAuthenticationMethod() {
        return "jwt";
    }
}
