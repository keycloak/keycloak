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

package org.keycloak.client.registration.cli.common;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class CmdStdinContext {

    private EndpointType regType;
    private ClientRepresentation client;
    private OIDCClientRepresentation oidcClient;
    private String content;

    public CmdStdinContext() {}

    public EndpointType getEndpointType() {
        return regType;
    }

    public void setEndpointType(EndpointType regType) {
        this.regType = regType;
    }

    public ClientRepresentation getClient() {
        return client;
    }

    public void setClient(ClientRepresentation client) {
        this.client = client;
    }

    public OIDCClientRepresentation getOidcClient() {
        return oidcClient;
    }

    public void setOidcClient(OIDCClientRepresentation oidcClient) {
        this.oidcClient = oidcClient;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRegistrationAccessToken() {
        if (client != null) {
            return client.getRegistrationAccessToken();
        } else if (oidcClient != null) {
            return oidcClient.getRegistrationAccessToken();
        }
        return null;
    }
}
