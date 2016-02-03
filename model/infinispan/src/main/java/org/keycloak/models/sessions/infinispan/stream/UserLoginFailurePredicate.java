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

import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserLoginFailurePredicate implements Predicate<Map.Entry<LoginFailureKey, LoginFailureEntity>>, Serializable {

    private String realm;

    private UserLoginFailurePredicate(String realm) {
        this.realm = realm;
    }

    public static UserLoginFailurePredicate create(String realm) {
        return new UserLoginFailurePredicate(realm);
    }

    @Override
    public boolean test(Map.Entry<LoginFailureKey, LoginFailureEntity> entry) {
        LoginFailureEntity e = entry.getValue();
        return realm.equals(e.getRealm());
    }

}
