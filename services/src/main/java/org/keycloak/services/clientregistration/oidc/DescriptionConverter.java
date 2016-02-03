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

package org.keycloak.services.clientregistration.oidc;

import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientregistration.ClientRegistrationException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DescriptionConverter {

    public static ClientRepresentation toInternal(OIDCClientRepresentation clientOIDC) throws ClientRegistrationException {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientOIDC.getClientId());
        client.setName(clientOIDC.getClientName());
        client.setRedirectUris(clientOIDC.getRedirectUris());
        client.setBaseUrl(clientOIDC.getClientUri());

        List<String> oidcResponseTypes = clientOIDC.getResponseTypes();
        if (oidcResponseTypes == null || oidcResponseTypes.isEmpty()) {
            oidcResponseTypes = Collections.singletonList(OIDCResponseType.CODE);
        }
        List<String> oidcGrantTypes = clientOIDC.getGrantTypes();

        try {
            OIDCResponseType responseType = OIDCResponseType.parse(oidcResponseTypes);
            client.setStandardFlowEnabled(responseType.hasResponseType(OIDCResponseType.CODE));
            client.setImplicitFlowEnabled(responseType.isImplicitOrHybridFlow());
            if (oidcGrantTypes != null) {
                client.setDirectAccessGrantsEnabled(oidcGrantTypes.contains(OAuth2Constants.PASSWORD));
                client.setServiceAccountsEnabled(oidcGrantTypes.contains(OAuth2Constants.CLIENT_CREDENTIALS));
            }
        } catch (IllegalArgumentException iae) {
            throw new ClientRegistrationException(iae.getMessage(), iae);
        }

        return client;
    }

    public static OIDCClientRepresentation toExternalResponse(ClientRepresentation client, URI uri) {
        OIDCClientRepresentation response = new OIDCClientRepresentation();
        response.setClientId(client.getClientId());
        response.setClientName(client.getName());
        response.setClientUri(client.getBaseUrl());
        response.setClientSecret(client.getSecret());
        response.setRedirectUris(client.getRedirectUris());
        response.setRegistrationAccessToken(client.getRegistrationAccessToken());
        response.setRegistrationClientUri(uri.toString());
        response.setResponseTypes(getOIDCResponseTypes(client));
        response.setGrantTypes(getOIDCGrantTypes(client));
        return response;
    }

    private static List<String> getOIDCResponseTypes(ClientRepresentation client) {
        List<String> responseTypes = new ArrayList<>();
        if (client.isStandardFlowEnabled()) {
            responseTypes.add(OAuth2Constants.CODE);
            responseTypes.add(OIDCResponseType.NONE);
        }
        if (client.isImplicitFlowEnabled()) {
            responseTypes.add(OIDCResponseType.ID_TOKEN);
            responseTypes.add("id_token token");
        }
        if (client.isStandardFlowEnabled() && client.isImplicitFlowEnabled()) {
            responseTypes.add("code id_token");
            responseTypes.add("code token");
            responseTypes.add("code id_token token");
        }
        return responseTypes;
    }

    private static List<String> getOIDCGrantTypes(ClientRepresentation client) {
        List<String> grantTypes = new ArrayList<>();
        if (client.isStandardFlowEnabled()) {
            grantTypes.add(OAuth2Constants.AUTHORIZATION_CODE);
        }
        if (client.isImplicitFlowEnabled()) {
            grantTypes.add(OAuth2Constants.IMPLICIT);
        }
        if (client.isDirectAccessGrantsEnabled()) {
            grantTypes.add(OAuth2Constants.PASSWORD);
        }
        if (client.isServiceAccountsEnabled()) {
            grantTypes.add(OAuth2Constants.CLIENT_CREDENTIALS);
        }
        grantTypes.add(OAuth2Constants.REFRESH_TOKEN);
        return grantTypes;
    }

}
