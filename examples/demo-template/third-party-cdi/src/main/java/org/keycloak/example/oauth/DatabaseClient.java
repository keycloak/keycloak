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
import org.jboss.logging.Logger;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.common.util.UriUtils;
import org.keycloak.servlet.ServletOAuthClient;
import org.keycloak.util.JsonSerialization;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @version $Revision: 1 $
 */
@ApplicationScoped
@Named("databaseClient")
public class DatabaseClient {

    @Inject
    @ServletRequestQualifier
    private HttpServletRequest request;

    @Inject
    private HttpServletResponse response;

    @Inject
    private FacesContext facesContext;

    @Inject
    private ServletOAuthClient oauthClient;

    @Inject
    private UserData userData;

    private static final Logger logger = Logger.getLogger(DatabaseClient.class);

    public void retrieveAccessToken() {
        try {
            oauthClient.redirectRelative("client.jsf", request, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class TypedList extends ArrayList<String> {}

    public void sendCustomersRequest() {
        List<String> customers = sendRequestToDBApplication(getBaseUrl() + "/database/customers");
        userData.setCustomers(customers);
    }

    public void sendProductsRequest() {
        List<String> products = sendRequestToDBApplication(getBaseUrl() + "/database/products");
        userData.setProducts(products);
    }

    protected List<String> sendRequestToDBApplication(String dbUri) {
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(dbUri);
        try {

            if (userData.isHasAccessToken()) {
                get.addHeader("Authorization", "Bearer " + userData.getAccessToken());
            }

            HttpResponse response = client.execute(get);
            switch (response.getStatusLine().getStatusCode()) {
                case 200: HttpEntity entity = response.getEntity();
                    InputStream is = entity.getContent();
                    try {
                        return JsonSerialization.readValue(is, TypedList.class);
                    } finally {
                        is.close();
                    }
                case 401: facesContext.addMessage(null, new FacesMessage("Status: 401. Request not authenticated! You need to retrieve access token first."));
                    break;
                case 403: facesContext.addMessage(null, new FacesMessage("Status: 403. Access token has insufficient privileges"));
                    break;
                default: facesContext.addMessage(null, new FacesMessage("Status: " + response.getStatusLine() + ". Not able to retrieve data. See log for details"));
                    logger.warn("Error occured. Status: " + response.getStatusLine());
            }

            return null;
        } catch (IOException e) {
            e.printStackTrace();
            facesContext.addMessage(null, new FacesMessage("Unknown error. See log for details"));
            return null;
        }
    }

    public String getBaseUrl() {
        KeycloakSecurityContext session = (KeycloakSecurityContext)request.getAttribute(KeycloakSecurityContext.class.getName());
        return UriUtils.getOrigin(request.getRequestURL().toString());
    }

}
