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
package org.keycloak.storage.client;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;

import java.util.Collections;
import java.util.Map;

/**
 * Helper base class for ClientModel implementations for ClientStorageProvider implementations.
 *
 * Contains default implementations of some methods
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractClientStorageAdapter extends UnsupportedOperationsClientStorageAdapter {
    protected KeycloakSession session;
    protected RealmModel realm;
    protected ClientStorageProviderModel component;
    private StorageId storageId;


    public AbstractClientStorageAdapter(KeycloakSession session, RealmModel realm, ClientStorageProviderModel component) {
        this.session = session;
        this.realm = realm;
        this.component = component;
    }

    /**
     * Creates federated id based on getClientId() method
     *
     * @return
     */
    @Override
    public String getId() {
        if (storageId == null) {
            storageId = new StorageId(component.getId(), getClientId());
        }
        return storageId.getId();
    }

    @Override
    public final RealmModel getRealm() {
        return realm;
    }


    /**
     * This method really isn't used by anybody anywhere.  Legacy feature never supported.
     *
     * @return
     */
    @Override
    public boolean isSurrogateAuthRequired() {
        return false;
    }

    /**
     * This method really isn't used by anybody anywhere.  Legacy feature never supported.
     *
     * @return
     */
    @Override
    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        // do nothing, we don't do anything with this.
    }

    /**
     * This is for logout.  Empty implementation for now.  Can override if you can store this information somewhere.
     *
     * @return
     */
    @Override
    public Map<String, Integer> getRegisteredNodes() {
        return Collections.EMPTY_MAP;
    }

    /**
     * This is for logout.  Empty implementation for now.  Can override if you can store this information somewhere.
     *
     * @return
     */
    @Override
    public void registerNode(String nodeHost, int registrationTime) {
        // do nothing
    }

    /**
     * This is for logout.  Empty implementation for now.  Can override if you can store this information somewhere.
     *
     * @return
     */
    @Override
    public void unregisterNode(String nodeHost) {
        // do nothing
    }

    /**
     * Overriding implementations should call super.updateClient() as this fires off an update event.
     *
     */
    @Override
    public void updateClient() {
        session.getKeycloakSessionFactory().publish(new ClientModel.ClientUpdatedEvent() {

            @Override
            public ClientModel getUpdatedClient() {
                return AbstractClientStorageAdapter.this;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });

    }


}
