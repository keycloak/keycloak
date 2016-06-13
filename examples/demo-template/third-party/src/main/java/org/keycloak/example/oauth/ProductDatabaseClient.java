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

package org.keycloak.example.oauth;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.servlet.ServletOAuthClient;
import org.keycloak.util.JsonSerialization;
import org.keycloak.common.util.UriUtils;

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
        KeycloakSecurityContext session = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());

        // The ServletOAuthClient is obtained by getting a context attribute
        // that is set in the Bootstrap context listener in this project.
        // You really should come up with a better way to initialize
        // and obtain the ServletOAuthClient.  I actually suggest downloading the ServletOAuthClient code
        // and take a look how it works. You can also take a look at third-party-cdi example
        ServletOAuthClient oAuthClient = (ServletOAuthClient) request.getServletContext().getAttribute(ServletOAuthClient.class.getName());
        HttpClient client = new DefaultHttpClient();

        HttpGet get = new HttpGet(UriUtils.getOrigin(request.getRequestURL().toString()) + "/database/products");
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

}
