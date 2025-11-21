/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.broker.saml.mappers;

import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import org.jboss.logging.Logger;

/**
 * Abstract class that handles the logic for importing and updating brokered users for all mappers that map a SAML
 * attribute into a {@code Keycloak} group.
 *
 * @author <a href="mailto:denis.bernard@avanade.com">Denis Bernard</a>,
 */
public abstract class AbstractAttributeToGroupMapper extends AbstractIdentityProviderMapper {

    private static final Logger LOG = Logger.getLogger(AbstractAttributeToGroupMapper.class);


    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        GroupModel group = this.getGroup(session, realm, mapperModel);
        if (group == null) {
            return;
        }

        if (this.applies(mapperModel, context)) {
            user.joinGroup(group);
        }
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        GroupModel group = this.getGroup(session, realm, mapperModel);
        if (group == null) {
            return;
        }

        String groupId = group.getId();
        if (!context.hasMapperAssignedGroup(groupId)) {
            if (this.applies(mapperModel, context)) {
                context.addMapperAssignedGroup(groupId);
                user.joinGroup(group);
            } else {
                user.leaveGroup(group);
            }
        }
    }

    /**
     * This method must be implemented by subclasses and they must return {@code true} if their mapping can be applied
     * (i.e. user has the SAML attribute that should be mapped) or {@code false} otherwise.
     *
     * @param mapperModel a reference to the {@link IdentityProviderMapperModel}.
     * @param context a reference to the {@link BrokeredIdentityContext}.
     * @return {@code true} if the mapping can be applied or {@code false} otherwise.
     */
    protected abstract boolean applies(final IdentityProviderMapperModel mapperModel, final BrokeredIdentityContext context);

    /**
     * Obtains the {@link GroupModel} corresponding the group configured in the specified
     * {@link IdentityProviderMapperModel}.
     * If the group doesn't correspond to one of the realm's client group or to one of the realm's group, this method
     * returns {@code null}.
     *
     * @param session
     * @param realm       a reference to the realm.
     * @param mapperModel a reference to the {@link IdentityProviderMapperModel} containing the configured group.
     * @return the {@link GroupModel} that corresponds to the mapper model group or {@code null}, if the group could not be found
     */

    private GroupModel getGroup(KeycloakSession session, final RealmModel realm, final IdentityProviderMapperModel mapperModel) {
        String groupPath = mapperModel.getConfig().get(ConfigConstants.GROUP);
        GroupModel group = KeycloakModelUtils.findGroupByPath(session, realm, groupPath);

        if (group == null) {
            LOG.warnf("Unable to find group by path '%s' referenced by mapper '%s' on realm '%s'.", groupPath, mapperModel.getName(), realm.getName());
        }
        return group;
    }
}
