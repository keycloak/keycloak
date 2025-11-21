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
import java.util.Objects;
import java.util.function.Predicate;

import org.keycloak.forms.login.freemarker.model.IdentityProviderBean;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.util.Booleans;

import static org.keycloak.models.IdentityProviderStorageProvider.FetchMode.ALL;
import static org.keycloak.models.IdentityProviderStorageProvider.FetchMode.ORG_ONLY;
import static org.keycloak.models.IdentityProviderStorageProvider.FetchMode.REALM_ONLY;

public class OrganizationAwareIdentityProviderBean extends IdentityProviderBean {

    private final OrganizationModel organization;
    private final boolean onlyRealmBrokers;
    private final boolean onlyOrganizationBrokers;

    public OrganizationAwareIdentityProviderBean(IdentityProviderBean delegate) {
        this(delegate, false);
    }

    public OrganizationAwareIdentityProviderBean(IdentityProviderBean delegate,  boolean onlyOrganizationBrokers) {
        this(delegate, onlyOrganizationBrokers, false);
    }

    public OrganizationAwareIdentityProviderBean(IdentityProviderBean delegate, boolean onlyOrganizationBrokers, boolean onlyRealmBrokers) {
        super(delegate.getSession(), delegate.getRealm(), delegate.getBaseURI(), delegate.getFlowContext());
        this.organization = Organizations.resolveOrganization(super.session);
        this.onlyRealmBrokers = onlyRealmBrokers;
        this.onlyOrganizationBrokers = onlyOrganizationBrokers;
    }

    @Override
    protected List<IdentityProvider> searchForIdentityProviders(String existingIDP) {
        if (onlyRealmBrokers) {
            // we only want the realm-level IDPs - i.e. those not associated with any orgs.
            return session.identityProviders().getForLogin(REALM_ONLY, null)
                    .filter(idp -> !Objects.equals(existingIDP, idp.getAlias()))
                    .map(idp -> createIdentityProvider(this.realm, this.baseURI, idp))
                    .sorted(IDP_COMPARATOR_INSTANCE).toList();
        }
        if (onlyOrganizationBrokers) {
            // we already have the organization, just fetch the organization's public enabled IDPs.
            if (this.organization != null) {
                return organization.getIdentityProviders()
                        .filter(idp -> idp.isEnabled() && Booleans.isFalse(idp.isLinkOnly()) && Booleans.isFalse(idp.isHideOnLogin()))
                        .filter(idp -> !Objects.equals(existingIDP, idp.getAlias()))
                        .map(idp -> createIdentityProvider(super.realm, super.baseURI, idp))
                        .sorted(IDP_COMPARATOR_INSTANCE).toList();
            }
            // we don't have a specific organization - fetch public enabled IDPs linked to any org.
            return session.identityProviders().getForLogin(ORG_ONLY, null)
                    .filter(idp -> idp.isEnabled() && !Objects.equals(existingIDP, idp.getAlias())) // re-check isEnabled as idp might have been wrapped.
                    .map(idp -> createIdentityProvider(this.realm, this.baseURI, idp))
                    .sorted(IDP_COMPARATOR_INSTANCE).toList();
        }
        return session.identityProviders().getForLogin(ALL, this.organization != null ? this.organization.getId() : null)
                .filter(idp -> idp.isEnabled() && !Objects.equals(existingIDP, idp.getAlias())) // re-check isEnabled as idp might have been wrapped.
                .map(idp -> createIdentityProvider(this.realm, this.baseURI, idp))
                .sorted(IDP_COMPARATOR_INSTANCE).toList();
    }

    @Override
    protected Predicate<IdentityProviderModel> federatedProviderPredicate() {
        // use the predicate from the superclass combined with the organization filter.
        return super.federatedProviderPredicate().and(idp -> {
            if (onlyRealmBrokers) {
                return idp.getOrganizationId() == null;
            } else if (onlyOrganizationBrokers) {
                return isPublicOrganizationBroker(idp);
            } else {
                return idp.getOrganizationId() == null || isPublicOrganizationBroker(idp);
            }
        });
    }

    private boolean isPublicOrganizationBroker(IdentityProviderModel idp) {

        if (idp.getOrganizationId() == null) {
            return false;
        }
        if (organization != null && !Objects.equals(organization.getId(),idp.getOrganizationId())) {
            return false;
        }
        return Booleans.isFalse(idp.isHideOnLogin());
    }
}
