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

package org.keycloak.adapters.authentication;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.logging.Logger;
import org.keycloak.adapters.KeycloakDeployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientCredentialsProviderUtils {

    private static Logger logger = Logger.getLogger(ClientCredentialsProviderUtils.class);

    public static ClientCredentialsProvider bootstrapClientAuthenticator(KeycloakDeployment deployment) {
        String clientId = deployment.getResourceName();
        Map<String, Object> clientCredentials = deployment.getResourceCredentials();

        String authenticatorId;
        if (clientCredentials == null || clientCredentials.isEmpty()) {
            authenticatorId = ClientIdAndSecretCredentialsProvider.PROVIDER_ID;
        } else {
            authenticatorId = (String) clientCredentials.get("provider");
            if (authenticatorId == null) {
                // If there is just one credential type, use provider from it
                if (clientCredentials.size() == 1) {
                    authenticatorId = clientCredentials.keySet().iterator().next();
                } else {
                    throw new RuntimeException("Can't identify clientAuthenticator from the configuration of client '" + clientId + "' . Check your adapter configurations");
                }
            }
        }

        logger.debugf("Using provider '%s' for authentication of client '%s'", authenticatorId, clientId);

        Map<String, ClientCredentialsProvider> authenticators = new HashMap<>();
        loadAuthenticators(authenticators, ClientCredentialsProviderUtils.class.getClassLoader());
        loadAuthenticators(authenticators, Thread.currentThread().getContextClassLoader());

        ClientCredentialsProvider authenticator = authenticators.get(authenticatorId);
        if (authenticator == null) {
            throw new RuntimeException("Couldn't find ClientCredentialsProvider implementation class with id: " + authenticatorId + ". Loaded authentication providers: " + authenticators.keySet());
        }

        Object config = (clientCredentials==null) ? null : clientCredentials.get(authenticatorId);
        authenticator.init(deployment, config);

        return authenticator;
    }

    private static void loadAuthenticators(Map<String, ClientCredentialsProvider> authenticators, ClassLoader classLoader) {
        for (ClientCredentialsProvider authenticator : ServiceLoader.load(ClientCredentialsProvider.class, classLoader)) {
            try {
                logger.debugf("Loaded clientCredentialsProvider %s", authenticator.getId());
                authenticators.put(authenticator.getId(), authenticator);
            } catch (ServiceConfigurationError e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to load clientCredentialsProvider with classloader: " + classLoader, e);
                }
            }
        }
    }

    /**
     * Use this method when calling backchannel request directly from your application. See service-account example from demo for more details
     */
    public static void setClientCredentials(KeycloakDeployment deployment, Map<String, String> requestHeaders, Map<String, String> formparams) {
        ClientCredentialsProvider authenticator = deployment.getClientAuthenticator();
        authenticator.setClientCredentials(deployment, requestHeaders, formparams);
    }

    /**
     * Don't use directly from your JEE apps to avoid HttpClient linkage errors! Instead use the method {@link #setClientCredentials(KeycloakDeployment, Map, Map)}
     */
    public static void setClientCredentials(KeycloakDeployment deployment, HttpPost post, List<NameValuePair> formparams) {
        Map<String, String> reqHeaders = new HashMap<>();
        Map<String, String> reqParams = new HashMap<>();
        setClientCredentials(deployment, reqHeaders, reqParams);

        for (Map.Entry<String, String> header : reqHeaders.entrySet()) {
            post.setHeader(header.getKey(), header.getValue());
        }

        for (Map.Entry<String, String> param : reqParams.entrySet()) {
            formparams.add(new BasicNameValuePair(param.getKey(), param.getValue()));
        }
    }

}
