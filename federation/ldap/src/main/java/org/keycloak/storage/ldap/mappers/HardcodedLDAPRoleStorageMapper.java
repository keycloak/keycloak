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

package org.keycloak.storage.ldap.mappers;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;

import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class HardcodedLDAPRoleStorageMapper extends AbstractLDAPStorageMapper {

    private static final Logger logger = Logger.getLogger(HardcodedLDAPRoleStorageMapper.class);

    public static final String ROLE = "role";

    public HardcodedLDAPRoleStorageMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider) {
        super(mapperModel, ldapProvider);
    }

    @Override
    public void beforeLDAPQuery(LDAPQuery query) {
    }

    @Override
    public UserModel proxy(LDAPObject ldapUser, UserModel delegate, RealmModel realm) {
        return new UserModelDelegate(delegate) {

            @Override
            public Set<RoleModel> getRealmRoleMappings() {
                Set<RoleModel> roles = super.getRealmRoleMappings();

                RoleModel role = getRole(realm);
                if (role != null && role.getContainer().equals(realm)) {
                    roles.add(role);
                }

                return roles;
            }

            @Override
            public Set<RoleModel> getClientRoleMappings(ClientModel app) {
                Set<RoleModel> roles = super.getClientRoleMappings(app);

                RoleModel role = getRole(realm);
                if (role != null && role.getContainer().equals(app)) {
                    roles.add(role);
                }

                return roles;
            }

            @Override
            public boolean hasRole(RoleModel role) {
                return super.hasRole(role) || role.equals(getRole(realm));
            }

            @Override
            public Set<RoleModel> getRoleMappings() {
                Set<RoleModel> roles = super.getRoleMappings();

                RoleModel role = getRole(realm);
                if (role != null) {
                    roles.add(role);
                }

                return roles;
            }

            @Override
            public void deleteRoleMapping(RoleModel role) {
                if (role.equals(getRole(realm))) {
                    throw new ModelException("Not possible to delete role. It's hardcoded by LDAP mapper");
                } else {
                    super.deleteRoleMapping(role);
                }
            }
        };
    }

    @Override
    public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser, RealmModel realm) {

    }

    @Override
    public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate) {

    }

    private RoleModel getRole(RealmModel realm) {
        String roleName = mapperModel.getConfig().getFirst(HardcodedLDAPRoleStorageMapper.ROLE);
        RoleModel role = KeycloakModelUtils.getRoleFromString(realm, roleName);
        if (role == null) {
            logger.warnf("Hardcoded role '%s' configured in mapper '%s' is not available anymore");
        }
        return role;
    }
}
