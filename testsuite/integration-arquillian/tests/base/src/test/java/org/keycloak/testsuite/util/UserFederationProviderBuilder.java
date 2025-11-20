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

package org.keycloak.testsuite.util;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.representations.idm.UserFederationProviderRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserFederationProviderBuilder {

    private String id;
    private String displayName;
    private String providerName;
    private Map<String, String> config;
    private int priority = 1;
    private int fullSyncPeriod = -1;
    private int changedSyncPeriod = -1;
    private int lastSync = -1;

    private UserFederationProviderBuilder() {};

    public static UserFederationProviderBuilder create() {
        return new UserFederationProviderBuilder();
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserFederationProviderBuilder displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public UserFederationProviderBuilder providerName(String providerName) {
        this.providerName = providerName;
        return this;
    }

    public UserFederationProviderBuilder configProperty(String key, String value) {
        if (this.config == null) {
            this.config = new HashMap<>();
        }
        this.config.put(key, value);
        return this;
    }

    public UserFederationProviderBuilder removeConfigProperty(String key) {
        if (this.config != null) {
            this.config.remove(key);
        }
        return this;
    }

    public UserFederationProviderBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }

    public UserFederationProviderBuilder fullSyncPeriod(int fullSyncPeriod) {
        this.fullSyncPeriod = fullSyncPeriod;
        return this;
    }

    public UserFederationProviderBuilder changedSyncPeriod(int changedSyncPeriod) {
        this.changedSyncPeriod = changedSyncPeriod;
        return this;
    }

    public UserFederationProviderBuilder lastSync(int lastSync) {
        this.lastSync = lastSync;
        return this;
    }

    public UserFederationProviderRepresentation build() {
        UserFederationProviderRepresentation rep = new UserFederationProviderRepresentation();
        rep.setId(id);
        rep.setDisplayName(displayName);
        rep.setProviderName(providerName);
        rep.setConfig(config);
        rep.setPriority(priority);
        rep.setFullSyncPeriod(fullSyncPeriod);
        rep.setChangedSyncPeriod(changedSyncPeriod);
        rep.setLastSync(lastSync);
        return rep;
    }


}
