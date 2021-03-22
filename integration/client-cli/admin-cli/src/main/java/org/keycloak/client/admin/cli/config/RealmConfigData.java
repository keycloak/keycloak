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
package org.keycloak.client.admin.cli.config;

import org.keycloak.util.JsonSerialization;

import java.io.IOException;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class RealmConfigData {

    private String serverUrl;

    private String realm;

    private String clientId;

    private String token;

    private String refreshToken;

    private String signingToken;

    private String secret;

    private Long expiresAt;

    private Long refreshExpiresAt;

    private Long sigExpiresAt;


    public String serverUrl() {
        return serverUrl;
    }

    public void serverUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String realm() {
        return realm;
    }

    public void realm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getSigningToken() {
        return signingToken;
    }

    public void setSigningToken(String signingToken) {
        this.signingToken = signingToken;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getRefreshExpiresAt() {
        return refreshExpiresAt;
    }

    public void setRefreshExpiresAt(Long refreshExpiresAt) {
        this.refreshExpiresAt = refreshExpiresAt;
    }

    public Long getSigExpiresAt() {
        return sigExpiresAt;
    }

    public void setSigExpiresAt(Long sigExpiresAt) {
        this.sigExpiresAt = sigExpiresAt;
    }

    public void merge(RealmConfigData source) {
        serverUrl = source.serverUrl;
        realm = source.realm;
        clientId = source.clientId;
        token = source.token;
        refreshToken = source.refreshToken;
        signingToken = source.signingToken;
        secret = source.secret;
        expiresAt = source.expiresAt;
        refreshExpiresAt = source.refreshExpiresAt;
        sigExpiresAt = source.sigExpiresAt;
    }

    public void mergeRefreshTokens(RealmConfigData source) {
        token = source.token;
        refreshToken = source.refreshToken;
        expiresAt = source.expiresAt;
        refreshExpiresAt = source.refreshExpiresAt;
    }

    @Override
    public String toString() {
        try {
            return JsonSerialization.writeValueAsPrettyString(this);
        } catch (IOException e) {
            return super.toString() + " - Error: " + e.toString();
        }
    }

    public RealmConfigData deepcopy() {
        RealmConfigData data = new RealmConfigData();
        data.serverUrl = serverUrl;
        data.realm = realm;
        data.clientId = clientId;
        data.token = token;
        data.refreshToken = refreshToken;
        data.signingToken = signingToken;
        data.secret = secret;
        data.expiresAt = expiresAt;
        data.refreshExpiresAt = refreshExpiresAt;
        data.sigExpiresAt = sigExpiresAt;
        return data;
    }
}
