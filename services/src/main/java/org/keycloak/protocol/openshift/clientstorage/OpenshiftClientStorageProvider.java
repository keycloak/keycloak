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
package org.keycloak.protocol.openshift.clientstorage;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.openshift.connections.rest.OpenshiftClient;
import org.keycloak.protocol.openshift.connections.rest.api.v1.ServiceAccounts;
import org.keycloak.protocol.openshift.connections.rest.apis.oauth.OAuthClients;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.client.ClientLookupProvider;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.storage.client.ClientStorageProviderModel;

import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OpenshiftClientStorageProvider implements ClientStorageProvider, ClientLookupProvider {
    protected KeycloakSession session;
    protected OpenshiftClientStorageModel component;
    protected OpenshiftClient openshiftClient;

    public OpenshiftClientStorageProvider(KeycloakSession session, ClientStorageProviderModel component) {
        this.session = session;
        this.component = new OpenshiftClientStorageModel(component);
    }

    public OpenshiftClientStorageModel getComponent() {
        return component;
    }

    @Override
    public void close() {
        if (openshiftClient != null) openshiftClient.close();

    }

    protected void initClient() {
        if (openshiftClient != null) return;
        openshiftClient = OpenshiftClient.instance(component.getOpenshiftUri(), component.getToken());

    }

    public OpenshiftClient getOpenshiftClient() {
        initClient();
        return openshiftClient;
    }

    @Override
    public ClientModel getClientById(String id, RealmModel realm) {
        StorageId storageId = new StorageId(id);
        if (!storageId.getProviderId().equals(component.getId())) return null;
        String clientId = storageId.getExternalId();
        return getClientByClientId(clientId, realm);
    }

    @Override
    public ClientModel getClientByClientId(String clientId, RealmModel realm) {
        initClient();
        clientId = clientId.trim();
        Matcher matcher = OpenshiftSAClientAdapter.SERVICE_ACCOUNT_PATTERN.matcher(clientId);
        if (matcher.matches()) {
            String namespace = matcher.group(1);
            String name = matcher.group(2);
            try {
                ServiceAccounts.ServiceAccountRepresentation rep = openshiftClient.api().namespace(namespace).serviceAccounts().get(name);
                return new OpenshiftSAClientAdapter(session, realm, clientId, rep, this);
            } catch (NotFoundException nfe) {
                if (nfe.getResponse() != null) nfe.getResponse().close();
                return null;
            }
        } else {
            OAuthClients.OAuthClientRepresentation client = null;
            try {
                client = openshiftClient.apis().oauth().clients().get(clientId);
                return new OpenshiftOAuthClientAdapter(session, realm, component, client);
            } catch (NotFoundException nfe) {
                if (nfe.getResponse() != null) nfe.getResponse().close();
                return null;
            }
        }
    }


}
