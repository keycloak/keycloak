package org.keycloak.example.oauth;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.keycloak.adapters.TokenGrantRequest;
import org.keycloak.servlet.ServletOAuthClient;
import org.keycloak.util.JsonSerialization;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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

    static class TypedList extends ArrayList<String> {}

    public static List<String> getProducts(HttpServletRequest request) {
        // This is really the worst code ever. The ServletOAuthClient is obtained by getting a context attribute
        // that is set in the Bootstrap context listenr in this project.
        // You really should come up with a better way to initialize
        // and obtain the ServletOAuthClient.  I actually suggest downloading the ServletOAuthClient code
        // and take a look how it works.
        ServletOAuthClient oAuthClient = (ServletOAuthClient) request.getServletContext().getAttribute(ServletOAuthClient.class.getName());
        String token = null;
        try {
            token = oAuthClient.getBearerToken(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TokenGrantRequest.HttpFailure failure) {
            throw new RuntimeException(failure);
        }

        HttpClient client = oAuthClient.getClient();

        HttpGet get = new HttpGet("http://localhost:8080/database/products");
        get.addHeader("Authorization", "Bearer " + token);
        try {
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            try {
                return JsonSerialization.readValue(is, TypedList.class);
            } finally {
                is.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
