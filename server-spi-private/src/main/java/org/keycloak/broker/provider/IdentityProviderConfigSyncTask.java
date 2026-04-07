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
package org.keycloak.broker.provider;

import java.util.Objects;
import java.util.stream.Stream;

import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderQuery;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.timer.ScheduledTask;

public class IdentityProviderConfigSyncTask implements ScheduledTask {

    @Override
    public String getTaskName() {
        return "idp-well-known-sync";
    }

    @Override
    public void run(KeycloakSession session) {
        session.realms().getRealmsStream()
            .map(RealmModel::getId)
            .forEach(realmId -> syncRealm(realmId, session.getKeycloakSessionFactory()));
    }

    private void syncRealm(String realmId, KeycloakSessionFactory factory) {
        KeycloakModelUtils.runJobInTransaction(factory, session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            if (realm == null) {
                return;
            }
            session.getContext().setRealm(realm);
            session.identityProviders().getAllStream(IdentityProviderQuery.any())
                .forEach(idp -> syncIdp(idp, session));
        });
    }

    private void syncIdp(IdentityProviderModel idp, KeycloakSession session) {
        IdentityProviderFactory<?> factory = Stream.concat(
                session.getKeycloakSessionFactory().getProviderFactoriesStream(IdentityProvider.class),
                session.getKeycloakSessionFactory().getProviderFactoriesStream(SocialIdentityProvider.class))
            .filter(f -> Objects.equals(f.getId(), idp.getProviderId()))
            .map(IdentityProviderFactory.class::cast)
            .findFirst()
            .orElse(null);
        if (factory == null) {
            return;
        }
        factory.create(session, idp).reloadConfig();
    }
}
