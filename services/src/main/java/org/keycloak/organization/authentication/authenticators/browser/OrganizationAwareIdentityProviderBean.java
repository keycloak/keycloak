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

package org.keycloak.organization.authentication.authenticators.browser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.keycloak.forms.login.freemarker.model.IdentityProviderBean;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;

public class OrganizationAwareIdentityProviderBean extends IdentityProviderBean {

    private final IdentityProviderBean delegate;
    private final KeycloakSession session;

    public OrganizationAwareIdentityProviderBean(IdentityProviderBean delegate, KeycloakSession session) {
        this.delegate = delegate;
        this.session = session;
    }

    @Override
    public List<IdentityProvider> getProviders() {
        return Optional.ofNullable(delegate.getProviders()).orElse(List.of()).stream()
                .filter(this::filterOrganizationalIdentityProvider)
                .toList();
    }

    @Override
    public boolean isDisplayInfo() {
        return delegate.isDisplayInfo();
    }

    private boolean filterOrganizationalIdentityProvider(IdentityProvider idp) {
        RealmModel realm = session.getContext().getRealm();
        IdentityProviderModel model = realm.getIdentityProviderByAlias(idp.getAlias());
        Map<String, String> config = model.getConfig();
        return !config.containsKey(OrganizationModel.ORGANIZATION_ATTRIBUTE);
    }
}
