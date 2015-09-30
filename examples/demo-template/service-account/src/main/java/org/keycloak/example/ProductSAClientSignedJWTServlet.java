package org.keycloak.example;

/**
 * Client authentication based on JWT signed by client private key .
 * See Keycloak documentation and <a href="https://tools.ietf.org/html/rfc7519">specs</a> for more details.
 *
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
