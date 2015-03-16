package org.keycloak.example.oauth;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.servlet.ServletOAuthClient;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.UriUtils;

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

    public static class Failure extends Exception {
        private int status;

        public Failure(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }


    public static void redirect(HttpServletRequest request, HttpServletResponse response) {
        // The ServletOAuthClient is obtained by getting a context attribute
        // that is set in the Bootstrap context listener in this project.
        // You really should come up with a better way to initialize
        // and obtain the ServletOAuthClient.  I actually suggest downloading the ServletOAuthClient code
        // and take a look how it works. You can also take a look at third-party-cdi example
        ServletOAuthClient oAuthClient = (ServletOAuthClient) request.getServletContext().getAttribute(ServletOAuthClient.class.getName());
        try {
            oAuthClient.redirectRelative("pull_data.jsp", request, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class TypedList extends ArrayList<String> {}

    public static AccessTokenResponse getTokenResponse(HttpServletRequest request) {
        // The ServletOAuthClient is obtained by getting a context attribute
        // that is set in the Bootstrap context listener in this project.
        // You really should come up with a better way to initialize
        // and obtain the ServletOAuthClient.  I actually suggest downloading the ServletOAuthClient code
        // and take a look how it works. You can also take a look at third-party-cdi example
        ServletOAuthClient oAuthClient = (ServletOAuthClient) request.getServletContext().getAttribute(ServletOAuthClient.class.getName());
        try {
            return oAuthClient.getBearerToken(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ServerRequest.HttpFailure failure) {
            throw new RuntimeException(failure);
        }

    }

    public static List<String> getProducts(HttpServletRequest request, String accessToken) throws Failure {
        // The ServletOAuthClient is obtained by getting a context attribute
        // that is set in the Bootstrap context listener in this project.
        // You really should come up with a better way to initialize
        // and obtain the ServletOAuthClient.  I actually suggest downloading the ServletOAuthClient code
        // and take a look how it works. You can also take a look at third-party-cdi example
        ServletOAuthClient oAuthClient = (ServletOAuthClient) request.getServletContext().getAttribute(ServletOAuthClient.class.getName());
        HttpClient client = oAuthClient.getClient();

        HttpGet get = new HttpGet(getBaseUrl(oAuthClient, request) + "/database/products");
        get.addHeader("Authorization", "Bearer " + accessToken);
        try {
            HttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new Failure(response.getStatusLine().getStatusCode());
            }
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

    public static String getBaseUrl(ServletOAuthClient oAuthClient, HttpServletRequest request) {
        switch (oAuthClient.getRelativeUrlsUsed()) {
            case ALL_REQUESTS:
                // Resolve baseURI from the request
                return UriUtils.getOrigin(request.getRequestURL().toString());
            case BROWSER_ONLY:
                // Resolve baseURI from the codeURL (This is already non-relative and based on our hostname)
                return UriUtils.getOrigin(oAuthClient.getTokenUrl());
            case NEVER:
                return "";
            default:
                return "";
        }
    }

}
