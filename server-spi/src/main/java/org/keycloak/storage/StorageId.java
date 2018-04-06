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
package org.keycloak.storage;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;

import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class StorageId implements Serializable {
    private String id;
    private String providerId;
    private String externalId;


    public StorageId(String id) {
        this.id = id;
        if (!id.startsWith("f:")) {
            externalId = id;
            return;
        }
        int providerIndex = id.indexOf(':', 2);
        providerId = id.substring(2, providerIndex);
        externalId = id.substring(providerIndex + 1);

    }

    public StorageId(String providerId, String externalId) {
        this.id = "f:" + providerId + ":" + externalId;
        this.providerId = providerId;
        this.externalId = externalId;
    }

    /**
     * generate the id string that should be returned by UserModel.getId()
     *
     * @param model
     * @param externalId id used to resolve user in external storage
     * @return
     */
    public static String keycloakId(ComponentModel model, String externalId) {
        return new StorageId(model.getId(), externalId).getId();
    }

    public static String externalId(String keycloakId) {
        return new StorageId(keycloakId).getExternalId();
    }
    public static String providerId(String keycloakId) {
        return new StorageId(keycloakId).getProviderId();
    }



    public static String resolveProviderId(UserModel user) {
        return new StorageId(user.getId()).getProviderId();
    }
    public static boolean isLocalStorage(UserModel user) {
        return new StorageId(user.getId()).getProviderId() == null;
    }
    public static boolean isLocalStorage(String id) {
        return new StorageId(id).getProviderId() == null;
    }

    public static String resolveProviderId(ClientModel client) {
        return new StorageId(client.getId()).getProviderId();
    }
    public static boolean isLocalStorage(ClientModel client) {
        return new StorageId(client.getId()).getProviderId() == null;
    }
    public boolean isLocal() {
        return getProviderId() == null;

    }

    public String getId() {
        return id;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getExternalId() {
        return externalId;
    }


}
