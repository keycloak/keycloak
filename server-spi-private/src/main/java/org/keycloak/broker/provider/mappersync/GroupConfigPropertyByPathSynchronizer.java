/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.broker.provider.mappersync;

import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.function.Consumer;

/**
 * Updates a group reference in a mapper config, when the path of a group changes.
 *
 * @author <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public class GroupConfigPropertyByPathSynchronizer extends AbstractConfigPropertySynchronizer<GroupModel.GroupPathChangeEvent> {

    public static final GroupConfigPropertyByPathSynchronizer INSTANCE = new GroupConfigPropertyByPathSynchronizer();

    private GroupConfigPropertyByPathSynchronizer() {
        // noop
    }

    @Override
    public Class<GroupModel.GroupPathChangeEvent> getEventClass() {
        return GroupModel.GroupPathChangeEvent.class;
    }

    @Override
    public RealmModel extractRealm(GroupModel.GroupPathChangeEvent event) {
        return event.getRealm();
    }

    @Override
    public String getConfigPropertyName() {
        return ConfigConstants.GROUP;
    }

    @Override
    protected void updateConfigPropertyIfNecessary(GroupModel.GroupPathChangeEvent event,
            String currentPropertyValue, Consumer<String> propertyUpdater) {
        String configuredGroupPath = KeycloakModelUtils.normalizeGroupPath(currentPropertyValue);

        String previousGroupPath = event.getPreviousPath();
        if (previousGroupPath.equals(configuredGroupPath)) {
            String newPath = event.getNewPath();
            propertyUpdater.accept(newPath);
        }
    }

}
