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

package org.keycloak.broker.oidc.mappers;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:artur.baltabayev@bosch.io">Artur Baltabayev</a>,
 * <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public abstract class AbstractClaimToGroupMapper extends AbstractClaimMapper {

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {

        GroupModel group = KeycloakModelUtils.getGroupForIdpMapper(session, realm, mapperModel, context);
        if (group == null) {
            return;
        }

        if (applies(mapperModel, context)) {
            user.joinGroup(group);
        }
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {

        GroupModel group = KeycloakModelUtils.getGroupForIdpMapper(session, realm, mapperModel, context);
        if (group == null) {
            return;
        }

        String groupId = group.getId();
        if (!context.hasMapperAssignedGroup(groupId)) {
            if (applies(mapperModel, context)) {
                context.addMapperAssignedGroup(groupId);
                user.joinGroup(group);
            } else {
                user.leaveGroup(group);
            }
        }
    }

    /**
     * This method must be implemented by subclasses and they must return {@code true} if their mapping can be applied
     * (i.e. user has the OIDC claim that should be mapped) or {@code false} otherwise.
     *
     * @param mapperModel a reference to the {@link IdentityProviderMapperModel}.
     * @param context a reference to the {@link BrokeredIdentityContext}.
     * @return {@code true} if the mapping can be applied or {@code false} otherwise.*
     */
    protected abstract boolean applies(final IdentityProviderMapperModel mapperModel,
            final BrokeredIdentityContext context);

}
