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

package org.keycloak.models.sessions.infinispan.stream;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

import org.keycloak.models.sessions.infinispan.entities.ClientRegistrationTrustedHostEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientRegistrationTrustedHostPredicate implements Predicate<Map.Entry<String, SessionEntity>>, Serializable {

    public static ClientRegistrationTrustedHostPredicate create(String realm) {
        return new ClientRegistrationTrustedHostPredicate(realm);
    }

    private ClientRegistrationTrustedHostPredicate(String realm) {
        this.realm = realm;
    }

    private String realm;


    @Override
    public boolean test(Map.Entry<String, SessionEntity> entry) {
        SessionEntity e = entry.getValue();

        if (!realm.equals(e.getRealm())) {
            return false;
        }

        if (!(e instanceof ClientRegistrationTrustedHostEntity)) {
            return false;
        }

        return true;
    }

}
