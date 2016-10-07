/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.HttpClientBuilder;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CamelClient {

    public static String sendRequest(HttpServletRequest req) throws CxfRsClient.Failure {
        KeycloakSecurityContext session = (KeycloakSecurityContext) req.getAttribute(KeycloakSecurityContext.class.getName());

        HttpClient client = new HttpClientBuilder()
                .disableTrustManager().build();

        StringBuilder sb = new StringBuilder();
        try {
            // Initially let's invoke a simple Camel-Jetty exposed endpoint
            HttpGet get = new HttpGet("http://localhost:8383/admin-camel-endpoint");
            get.addHeader("Authorization", "Bearer " + session.getTokenString());
            try {
                HttpResponse response = client.execute(get);
                if (response.getStatusLine().getStatusCode() != 200) {
                    return "There was a failure processing request.  You either didn't configure Keycloak properly or you don't have admin permission? Status code is "
                            + response.getStatusLine().getStatusCode();
                }
                HttpEntity entity = response.getEntity();
                InputStream is = entity.getContent();
                try {
                    sb.append(getStringFromInputStream(is));
                } finally {
                    is.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // Here we invoke a Jetty endpoint, published using Camel RestDSL
            get = new HttpGet("http://localhost:8484/restdsl/hello/world");
            get.addHeader("Authorization", "Bearer " + session.getTokenString());
            try {
                HttpResponse response = client.execute(get);
                if (response.getStatusLine().getStatusCode() != 200) {
                    return "There was a failure processing request with the RestDSL endpoint.  You either didn't configure Keycloak properly or you don't have admin permission? Status code is "
                            + response.getStatusLine().getStatusCode();
                }
                HttpEntity entity = response.getEntity();
                InputStream is = entity.getContent();
                try {
                    sb.append(getStringFromInputStream(is));
                } finally {
                    is.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            client.getConnectionManager().shutdown();
        }

        return sb.toString();
    }

    private static String getStringFromInputStream(InputStream is) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return sb.toString();

    }
}
