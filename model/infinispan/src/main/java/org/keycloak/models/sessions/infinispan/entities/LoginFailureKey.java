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

package org.keycloak.models.sessions.infinispan.entities;

import java.io.Serializable;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginFailureKey implements Serializable {

    private final String realm;
    private final String userId;

    public LoginFailureKey(String realm, String userId) {
        this.realm = realm;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LoginFailureKey key = (LoginFailureKey) o;

        if (realm != null ? !realm.equals(key.realm) : key.realm != null) return false;
        if (userId != null ? !userId.equals(key.userId) : key.userId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = realm != null ? realm.hashCode() : 0;
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        return result;
    }

}
