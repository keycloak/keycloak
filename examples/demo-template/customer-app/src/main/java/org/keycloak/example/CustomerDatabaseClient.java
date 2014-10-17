package org.keycloak.example;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CustomerDatabaseClient {

    static class TypedList extends ArrayList<String> {
    }

    public static class Failure extends Exception {
        private int status;

        public Failure(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }

    public static IDToken getIDToken(HttpServletRequest req) {
        KeycloakSecurityContext session = (KeycloakSecurityContext) req.getAttribute(KeycloakSecurityContext.class.getName());
        return session.getIdToken();

    }

    public static List<String> getCustomers(HttpServletRequest req) throws Failure {
        KeycloakSecurityContext session = (KeycloakSecurityContext) req.getAttribute(KeycloakSecurityContext.class.getName());

        HttpClient client = new HttpClientBuilder()
                .disableTrustManager().build();
        try {
            HttpGet get = new HttpGet(AdapterUtils.getOrigin(req.getRequestURL().toString(), session) + "/database/customers");
            get.addHeader("Authorization", "Bearer " + session.getTokenString());
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
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public static String increaseAndGetCounter(HttpServletRequest req) {
        HttpSession session = req.getSession();
        Integer counter = (Integer)session.getAttribute("counter");
        counter = (counter == null) ? 1 : counter + 1;
        session.setAttribute("counter", counter);
        return String.valueOf(counter);
    }
}
