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
package org.keycloak.models.utils;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DefaultRoles {
    public static Set<RoleModel> getDefaultRoles(RealmModel realm) {
        Set<RoleModel> set = new HashSet<>();
        for (String r : realm.getDefaultRoles()) {
            set.add(realm.getRole(r));
        }

        Function<ClientModel, Set<RoleModel>> defaultRoles = i -> i.getDefaultRoles().stream().map(i::getRole).collect(Collectors.toSet());
        realm.getClientsStream().map(defaultRoles).forEach(set::addAll);

        return set;
    }
    public static void addDefaultRoles(RealmModel realm, UserModel userModel) {
        for (RoleModel role : getDefaultRoles(realm)) {
            userModel.grantRole(role);
        }
    }
}
