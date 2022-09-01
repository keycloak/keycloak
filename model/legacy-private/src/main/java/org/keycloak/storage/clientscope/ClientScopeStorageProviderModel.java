/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.storage.clientscope;

import org.keycloak.component.ComponentModel;
import org.keycloak.storage.CacheableStorageProviderModel;

/**
 * Stored configuration of a Client scope Storage provider instance.
 */
public class ClientScopeStorageProviderModel extends CacheableStorageProviderModel {

    public ClientScopeStorageProviderModel() {
        setProviderType(ClientScopeStorageProvider.class.getName());
    }

    public ClientScopeStorageProviderModel(ComponentModel copy) {
        super(copy);
    }

    private transient Boolean enabled;

    @Override
    public void setEnabled(boolean flag) {
        enabled = flag;
        getConfig().putSingle(ENABLED, Boolean.toString(flag));
    }

    @Override
    public boolean isEnabled() {
        if (enabled == null) {
            String val = getConfig().getFirst(ENABLED);
            if (val == null) {
                enabled = true;
            } else {
                enabled = Boolean.valueOf(val);
            }
        }
        return enabled;

    }
}
