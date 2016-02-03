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

package org.keycloak.models.entities;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FederatedIdentityEntity {

    private String userId;
    private String userName;
    private String identityProvider;
    private String token;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getIdentityProvider() {
        return identityProvider;
    }

    public void setIdentityProvider(String identityProvider) {
        this.identityProvider = identityProvider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FederatedIdentityEntity that = (FederatedIdentityEntity) o;

        if (identityProvider != null && (that.identityProvider == null || !identityProvider.equals(that.identityProvider))) return false;
        if (userId != null && (that.userId == null || !userId.equals(that.userId))) return false;
        if (identityProvider == null && that.identityProvider != null)return false;
        if (userId == null && that.userId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int code = 1;
        if (userId != null) {
            code = code * userId.hashCode() * 13;
        }
        if (identityProvider != null) {
            code = code * identityProvider.hashCode() * 17;
        }
        return code;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
