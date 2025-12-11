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
package org.keycloak.client.cli.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ConfigData {

    @JsonIgnore
    private String externalToken;

    private String serverUrl;

    private String realm;

    private String truststore;

    private String trustpass;

    private Map<String, Map<String, RealmConfigData>> endpoints = new HashMap<>();


    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @JsonIgnore
    public String getExternalToken() {
        return externalToken;
    }

    @JsonIgnore
    public void setExternalToken(String externalToken) {
        this.externalToken = externalToken;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getTruststore() {
        return truststore;
    }

    public void setTruststore(String truststore) {
        this.truststore = truststore;
    }

    public String getTrustpass() {
        return trustpass;
    }

    public void setTrustpass(String trustpass) {
        this.trustpass = trustpass;
    }

    public Map<String, Map<String, RealmConfigData>> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, Map<String, RealmConfigData>> endpoints) {
        for (Map.Entry<String, Map<String, RealmConfigData>> entry: endpoints.entrySet()) {
            String endpoint = entry.getKey();
            for (Map.Entry<String, RealmConfigData> sub: entry.getValue().entrySet()) {
                RealmConfigData rdata = sub.getValue();
                rdata.serverUrl(endpoint);
                rdata.realm(sub.getKey());
            }
        }
        this.endpoints = endpoints;
    }

    public RealmConfigData sessionRealmConfigData() {
        if (serverUrl == null)
            throw new RuntimeException("Illegal state - no current endpoint in config data");
        if (realm == null)
            throw new RuntimeException("Illegal state - no current realm in config data");
        return ensureRealmConfigData(serverUrl, realm);
    }

    public RealmConfigData getRealmConfigData(String endpoint, String realm) {
        Map<String, RealmConfigData> realmData = endpoints.get(endpoint);
        if (realmData == null) {
            return null;
        }
        return realmData.get(realm);
    }

    public RealmConfigData ensureRealmConfigData(String endpoint, String realm) {
        RealmConfigData result = getRealmConfigData(endpoint, realm);
        if (result == null) {
            result = new RealmConfigData();
            result.serverUrl(endpoint);
            result.realm(realm);
            setRealmConfigData(result);
        }
        return result;
    }


    public void setRealmConfigData(RealmConfigData data) {
        Map<String, RealmConfigData> realm = endpoints.get(data.serverUrl());
        if (realm == null) {
            realm = new HashMap<>();
            endpoints.put(data.serverUrl(), realm);
        }
        realm.put(data.realm(), data);
    }

    public void merge(ConfigData source) {
        serverUrl = source.serverUrl;
        realm = source.realm;
        truststore = source.truststore;
        trustpass = source.trustpass;

        RealmConfigData current = getRealmConfigData(serverUrl, realm);
        RealmConfigData sourceRealm = source.getRealmConfigData(serverUrl, realm);

        if (current == null) {
            setRealmConfigData(sourceRealm);
        } else {
            current.merge(sourceRealm);
        }
    }

    public ConfigData deepcopy() {
        ConfigData data = new ConfigData();
        data.serverUrl = serverUrl;
        data.realm = realm;
        data.truststore = truststore;
        data.trustpass = trustpass;
        data.endpoints = new HashMap<>();

        for (Map.Entry<String, Map<String, RealmConfigData>> item: endpoints.entrySet()) {

            Map<String, RealmConfigData> nuitems = new HashMap<>();
            Map<String, RealmConfigData> curitems = item.getValue();

            if (curitems != null) {
                for (Map.Entry<String, RealmConfigData> ditem : curitems.entrySet()) {
                    RealmConfigData nudata = ditem.getValue();
                    if (nudata != null) {
                        nuitems.put(ditem.getKey(), nudata.deepcopy());
                    }
                }
                data.endpoints.put(item.getKey(), nuitems);
            }
        }
        return data;
    }

    @Override
    public String toString() {
        try {
            return JsonSerialization.writeValueAsPrettyString(this);
        } catch (IOException e) {
            return super.toString() + " - Error: " + e.toString();
        }
    }
}
