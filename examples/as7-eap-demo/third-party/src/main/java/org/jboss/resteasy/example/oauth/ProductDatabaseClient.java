package org.jboss.resteasy.example.oauth;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.servlet.ServletOAuthClient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProductDatabaseClient {
    public static void redirect(HttpServletRequest request, HttpServletResponse response) {
        // This is really the worst code ever. The ServletOAuthClient is obtained by getting a context attribute
        // that is set in the Bootstrap context listenr in this project.
        // You really should come up with a better way to initialize
        // and obtain the ServletOAuthClient.  I actually suggest downloading the ServletOAuthClient code
        // and take a look how it works.
        ServletOAuthClient oAuthClient = (ServletOAuthClient) request.getServletContext().getAttribute(ServletOAuthClient.class.getName());
        try {
            oAuthClient.redirectRelative("pull_data.jsp", request, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> getProducts(HttpServletRequest request) {
        // This is really the worst code ever. The ServletOAuthClient is obtained by getting a context attribute
        // that is set in the Bootstrap context listenr in this project.
        // You really should come up with a better way to initialize
        // and obtain the ServletOAuthClient.  I actually suggest downloading the ServletOAuthClient code
        // and take a look how it works.
        ServletOAuthClient oAuthClient = (ServletOAuthClient) request.getServletContext().getAttribute(ServletOAuthClient.class.getName());
        String token = oAuthClient.getBearerToken(request);
        ResteasyClient client = new ResteasyClientBuilder()
                .trustStore(oAuthClient.getTruststore())
                .hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY).build();
        try {
            // invoke without the Authorization header
            Response response = client.target("http://localhost:8080/database/products").request().get();
            response.close();
            if (response.getStatus() != 401) {
                response.close();
                client.close();
                throw new RuntimeException("Expecting an auth status code: " + response.getStatus());
            }
        } finally {
        }
        try {
            Response response = client.target("http://localhost:8080/database/products").request()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
            if (response.getStatus() != 200) {
               response.close();
               throw new RuntimeException("Failed to access!: " + response.getStatus());
            }
                return response.readEntity(new GenericType<List<String>>() {
                });
        } finally {
            client.close();
        }
    }
}
