/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oidc.scope;

import jakarta.annotation.Nonnull;

import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;

/**
 *
 * @author rmartinc
 */
public class DelegationScopeType extends UsernameScopeType {

    public static final String TYPE = "delegation";

    public DelegationScopeType() {
    }

    public DelegationScopeType(KeycloakSession session) {
        super(session);
    }

    @Override
    public String getTypeName() {
        return TYPE;
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public ParameterizedScopeTypeProvider create(KeycloakSession session) {
        return new DelegationScopeType(session);
    }

    @Override
    public void validateParameterWithUser(@Nonnull UserModel currentUser, @Nonnull ClientScopeModel scope, @Nonnull String parameter) throws InvalidScopeParameterException {
        UserModel targetUser = resolveUser(scope, parameter);
        if (targetUser.getId().equals(currentUser.getId())) {
            throw new InvalidScopeParameterException("User cannot target themselves");
        }
        RealmModel realm = scope.getRealm();
        AdminPermissionEvaluator evaluator = AdminPermissions.evaluator(session, realm, realm, targetUser);
        if (!evaluator.users().canImpersonate(currentUser, session.getContext().getClient())) {
            throw new InvalidScopeParameterException(String.format("User '%s' cannot be impersonated by the administrator '%s' in realm '%s'",
                    currentUser.getUsername(), targetUser.getUsername(), realm.getName()));
        }
    }

}
