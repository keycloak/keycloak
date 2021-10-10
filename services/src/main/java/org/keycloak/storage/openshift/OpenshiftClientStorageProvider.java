/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.storage.openshift;

import com.openshift.restclient.IClient;
import com.openshift.restclient.NotFoundException;
import com.openshift.restclient.model.IResource;
import java.util.Collections;
import java.util.Map;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.storage.client.ClientStorageProviderModel;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class OpenshiftClientStorageProvider implements ClientStorageProvider {

    private final KeycloakSession session;
    private final ClientStorageProviderModel providerModel;
    private final IClient client;

    OpenshiftClientStorageProvider(KeycloakSession session, ClientStorageProviderModel providerModel, IClient client) {
        this.session = session;
        this.providerModel = providerModel;
        this.client = client;
    }

    @Override
    public ClientModel getClientById(RealmModel realm, String id) {
        StorageId storageId = new StorageId(id);
        if (!storageId.getProviderId().equals(providerModel.getId())) return null;
        String clientId = storageId.getExternalId();
        return getClientByClientId(realm, clientId);
    }

    @Override
    public ClientModel getClientByClientId(RealmModel realm, String clientId) {
        Matcher matcher = OpenshiftClientStorageProviderFactory.SERVICE_ACCOUNT_PATTERN.matcher(clientId);
        IResource resource = null;

        if (matcher.matches()) {
            resource = getServiceAccount(matcher.group(2), matcher.group(1));
        } else {
            String defaultNamespace = providerModel.get(OpenshiftClientStorageProviderFactory.CONFIG_PROPERTY_DEFAULT_NAMESPACE);

            if (defaultNamespace != null) {
                resource = getServiceAccount(clientId, defaultNamespace);
            }
        }

        if (resource == null) {
            return null;
        }

        return new OpenshiftSAClientAdapter(clientId, resource, client, session, realm, providerModel);
    }

    @Override
    public Stream<ClientModel> searchClientsByClientIdStream(RealmModel realm, String clientId, Integer firstResult, Integer maxResults) {
        // TODO not sure about this, but I don't see this implementation using the search now
        return Stream.of(getClientByClientId(realm, clientId));
    }

    @Override
    public Stream<ClientModel> searchClientsByAttributes(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        // TODO not sure if we support searching clients for this provider
        return Stream.empty();
    }

    @Override
    public void close() {

    }

    private IResource getServiceAccount(String name, String namespace) {
        try {
            return client.get("ServiceAccount", name, namespace);
        } catch (NotFoundException nfe) {
            return null;
        }
    }

    @Override
    public Map<String, ClientScopeModel> getClientScopes(RealmModel realm, ClientModel client, boolean defaultScopes) {
        // TODO not sure about this, this implementation doesn't use it now
        return Collections.EMPTY_MAP;
    }
}
