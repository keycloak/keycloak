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
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureProvider;
import org.keycloak.models.UserLoginFailureProviderFactory;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.AbstractMapProviderFactory;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapUserLoginFailureProviderFactory extends AbstractMapProviderFactory<UserLoginFailureProvider, MapUserLoginFailureEntity, UserLoginFailureModel>
        implements UserLoginFailureProviderFactory, ProviderEventListener {

    private Runnable onClose;

    public MapUserLoginFailureProviderFactory() {
        super(UserLoginFailureModel.class);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(this);
        onClose = () -> factory.unregister(this);
    }

    @Override
    public void close() {
        super.close();
        onClose.run();
    }

    @Override
    public MapUserLoginFailureProvider create(KeycloakSession session) {
        return new MapUserLoginFailureProvider(session, getStorage(session));
    }

    @Override
    public String getHelpText() {
        return "User login failure provider";
    }

    @Override
    public void onEvent(ProviderEvent event) {
        if (event instanceof UserModel.UserRemovedEvent) {
            UserModel.UserRemovedEvent userRemovedEvent = (UserModel.UserRemovedEvent) event;

            MapUserLoginFailureProvider provider = MapUserLoginFailureProviderFactory.this.create(userRemovedEvent.getKeycloakSession());
            provider.removeUserLoginFailure(userRemovedEvent.getRealm(), userRemovedEvent.getUser().getId());
        } else if (event instanceof RealmModel.RealmRemovedEvent) {
            RealmModel.RealmRemovedEvent realmRemovedEvent = (RealmModel.RealmRemovedEvent) event;

            MapUserLoginFailureProvider provider = MapUserLoginFailureProviderFactory.this.create(realmRemovedEvent.getKeycloakSession());
            provider.removeAllUserLoginFailures(realmRemovedEvent.getRealm());
        }
    }

}
