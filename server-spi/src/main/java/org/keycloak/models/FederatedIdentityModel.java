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

package org.keycloak.models;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FederatedIdentityModel {

    private String token;
    private final String userId;
    private final String identityProvider;
    private final String userName;

    public FederatedIdentityModel(String identityProvider, String userId, String userName) {
        this(identityProvider, userId, userName, null);
    }

    public FederatedIdentityModel(String providerId, String userId, String userName, String token) {
        this.identityProvider = providerId;
        this.userId = userId;
        this.userName = userName;
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public String getIdentityProvider() {
        return identityProvider;
    }

    public String getUserName() {
        return userName;
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FederatedIdentityModel that = (FederatedIdentityModel) o;

        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (!identityProvider.equals(that.identityProvider)) return false;
        return userName != null ? userName.equals(that.userName) : that.userName == null;

    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + identityProvider.hashCode();
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        return result;
    }
}
