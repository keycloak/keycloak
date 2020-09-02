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

import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DefaultRoles {
    public static Stream<RoleModel> getDefaultRoles(RealmModel realm) {
        Stream<RoleModel> realmDefaultRoles = realm.getDefaultRolesStream().map(realm::getRole);
        Stream<RoleModel> clientDefaultRoles = realm.getClientsStream().flatMap(DefaultRoles::toClientDefaultRoles);
        return Stream.concat(realmDefaultRoles, clientDefaultRoles);
    }
    public static void addDefaultRoles(RealmModel realm, UserModel userModel) {
        getDefaultRoles(realm).forEach(userModel::grantRole);
    }

    private static Stream<RoleModel> toClientDefaultRoles(ClientModel c) {
        return c.getDefaultRolesStream().map(c::getRole);
    }
}
