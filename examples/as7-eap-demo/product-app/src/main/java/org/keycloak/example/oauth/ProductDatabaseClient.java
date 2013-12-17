package org.keycloak.example.oauth;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.keycloak.SkeletonKeySession;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProductDatabaseClient
{
    static class TypedList extends ArrayList<String> {}

    public static List<String> getProducts() {
        SkeletonKeySession session = SkeletonKeySession.getContext();
        HttpClient client = new HttpClientBuilder()
                .trustStore(session.getMetadata().getTruststore())
                .hostnameVerification(HttpClientBuilder.HostnameVerificationPolicy.ANY).build();
        try {
            HttpGet get = new HttpGet("http://localhost:8080/database/products");
            get.addHeader("Authorization", "Bearer " + session.getTokenString());
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
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

}
