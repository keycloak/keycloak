/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.loginFailure;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureProviderFactory;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.map.common.AbstractMapProviderFactory;
import org.keycloak.provider.InvalidationHandler;

import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.REALM_BEFORE_REMOVE;
import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.USER_BEFORE_REMOVE;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapUserLoginFailureProviderFactory extends AbstractMapProviderFactory<MapUserLoginFailureProvider, MapUserLoginFailureEntity, UserLoginFailureModel>
        implements UserLoginFailureProviderFactory<MapUserLoginFailureProvider>, InvalidationHandler {

    public MapUserLoginFailureProviderFactory() {
        super(UserLoginFailureModel.class, MapUserLoginFailureProvider.class);
    }

    @Override
    public MapUserLoginFailureProvider createNew(KeycloakSession session) {
        return new MapUserLoginFailureProvider(session, getStorage(session));
    }

    @Override
    public String getHelpText() {
        return "User login failure provider";
    }

    @Override
    public void invalidate(KeycloakSession session, InvalidableObjectType type, Object... params) {
        if (type == REALM_BEFORE_REMOVE) {
            create(session).removeAllUserLoginFailures((RealmModel) params[0]);
        } else if (type == USER_BEFORE_REMOVE) {
            create(session).removeUserLoginFailure((RealmModel) params[0], ((UserModel) params[1]).getId());
        }
    }

}
