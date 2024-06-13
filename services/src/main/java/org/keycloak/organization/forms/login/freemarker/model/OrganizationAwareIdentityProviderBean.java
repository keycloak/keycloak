/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.organization.forms.login.freemarker.model;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.keycloak.forms.login.freemarker.model.IdentityProviderBean;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;

public class OrganizationAwareIdentityProviderBean extends IdentityProviderBean {

    private final KeycloakSession session;
    private final List<IdentityProvider> providers;

    public OrganizationAwareIdentityProviderBean(IdentityProviderBean delegate, KeycloakSession session, boolean onlyOrganizationBrokers) {
        this(delegate, session, onlyOrganizationBrokers, false);
    }

    public OrganizationAwareIdentityProviderBean(IdentityProviderBean delegate, KeycloakSession session, boolean onlyOrganizationBrokers, boolean onlyRealmBrokers) {
        this.session = session;
        if (onlyRealmBrokers) {
            providers = Optional.ofNullable(delegate.getProviders()).orElse(List.of()).stream()
                    .filter(Predicate.not(this::isPublicOrganizationBroker))
                    .toList();
        } else if (onlyOrganizationBrokers) {
            providers = Optional.ofNullable(delegate.getProviders()).orElse(List.of()).stream()
                    .filter(this::isPublicOrganizationBroker)
                    .toList();
        } else {
            providers = Optional.ofNullable(delegate.getProviders()).orElse(List.of()).stream()
                    .filter(p -> isRealmBroker(p) || isPublicOrganizationBroker(p))
                    .toList();
        }
    }
    public OrganizationAwareIdentityProviderBean(IdentityProviderBean delegate, KeycloakSession session) {
        this(delegate, session, false);
    }

    @Override
    public List<IdentityProvider> getProviders() {
        return providers;
    }

    @Override
    public boolean isDisplayInfo() {
        return !providers.isEmpty();
    }

    private boolean isPublicOrganizationBroker(IdentityProvider idp) {
        RealmModel realm = session.getContext().getRealm();
        IdentityProviderModel model = realm.getIdentityProviderByAlias(idp.getAlias());

        if (model.getOrganizationId() == null) {
            return false;
        }

        return Boolean.parseBoolean(model.getConfig().getOrDefault(OrganizationModel.BROKER_PUBLIC, Boolean.FALSE.toString()));
    }

    private boolean isRealmBroker(IdentityProvider idp) {
        RealmModel realm = session.getContext().getRealm();
        IdentityProviderModel model = realm.getIdentityProviderByAlias(idp.getAlias());

        return model.getOrganizationId() == null;
    }
}
