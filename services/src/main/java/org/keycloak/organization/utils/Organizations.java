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

package org.keycloak.organization.utils;

import static java.util.Optional.ofNullable;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.keycloak.TokenVerifier;
import org.keycloak.authentication.actiontoken.inviteorg.InviteOrgActionToken;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.VerificationException;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.Constants;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.utils.StringUtil;

public class Organizations {

    public static boolean canManageOrganizationGroup(KeycloakSession session, GroupModel group) {
        if (Profile.isFeatureEnabled(Feature.ORGANIZATION)) {
            Object organization = session.getAttribute(OrganizationModel.class.getName());

            if (organization != null) {
                return true;
            }

            String orgId = group.getFirstAttribute(OrganizationModel.ORGANIZATION_ATTRIBUTE);

            return StringUtil.isBlank(orgId);
        }

        return true;
    }

    public static List<IdentityProviderModel> resolveBroker(KeycloakSession session, UserModel user) {
        OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
        RealmModel realm = session.getContext().getRealm();
        OrganizationModel organization = provider.getByMember(user);

        if (organization == null || !organization.isEnabled()) {
            return List.of();
        }

        if (provider.isManagedMember(organization, user)) {
            List<IdentityProviderModel> organizationBrokers = organization.getIdentityProviders().toList();
            return session.users().getFederatedIdentitiesStream(realm, user)
                    .map(f -> {
                        IdentityProviderModel broker = realm.getIdentityProviderByAlias(f.getIdentityProvider());

                        if (!organizationBrokers.contains(broker)) {
                            return null;
                        }

                        FederatedIdentityModel identity = session.users().getFederatedIdentity(realm, user, broker.getAlias());

                        if (identity != null) {
                            return broker;
                        }

                        return null;
                    }).filter(Objects::nonNull)
                    .toList();
        }

        return List.of();
    }

    public static Consumer<GroupModel> removeGroup(KeycloakSession session, RealmModel realm) {
        return group -> {
            if (!Profile.isFeatureEnabled(Feature.ORGANIZATION)) {
                realm.removeGroup(group);
                return;
            }

            OrganizationModel current = (OrganizationModel) session.getAttribute(OrganizationModel.class.getName());

            try {
                String orgId = group.getFirstAttribute(OrganizationModel.ORGANIZATION_ATTRIBUTE);
                OrganizationProvider provider = session.getProvider(OrganizationProvider.class);

                if (orgId != null) {
                    session.setAttribute(OrganizationModel.class.getName(), provider.getById(orgId));
                }

                realm.removeGroup(group);
            } finally {
                if (current == null) {
                    session.removeAttribute(OrganizationModel.class.getName());
                } else {
                    session.setAttribute(OrganizationModel.class.getName(), current);
                }
            }
        };
    }

    public static boolean isEnabledAndOrganizationsPresent(OrganizationProvider orgProvider) {
        return orgProvider != null && orgProvider.isEnabled() && orgProvider.count() != 0;
    }

    public static void checkEnabled(OrganizationProvider provider) {
        if (provider == null || !provider.isEnabled()) {
            throw ErrorResponse.error("Organizations not enabled for this realm.", Response.Status.NOT_FOUND);
        }
    }

    public static OrganizationRepresentation toRepresentation(OrganizationModel model) {
        if (model == null) {
            return null;
        }

        OrganizationRepresentation rep = new OrganizationRepresentation();

        rep.setId(model.getId());
        rep.setName(model.getName());
        rep.setEnabled(model.isEnabled());
        rep.setDescription(model.getDescription());
        rep.setAttributes(model.getAttributes());
        model.getDomains().filter(Objects::nonNull).map(Organizations::toRepresentation)
                .forEach(rep::addDomain);

        return rep;
    }

    public static OrganizationDomainRepresentation toRepresentation(OrganizationDomainModel model) {
        OrganizationDomainRepresentation representation = new OrganizationDomainRepresentation();
        representation.setName(model.getName());
        representation.setVerified(model.isVerified());
        return representation;
    }

    public static OrganizationModel toModel(OrganizationRepresentation rep, OrganizationModel model) {
        if (rep == null) {
            return null;
        }

        model.setName(rep.getName());
        model.setEnabled(rep.isEnabled());
        model.setDescription(rep.getDescription());
        model.setAttributes(rep.getAttributes());
        model.setDomains(ofNullable(rep.getDomains()).orElse(Set.of()).stream()
                    .filter(Objects::nonNull)
                    .filter(domain -> StringUtil.isNotBlank(domain.getName()))
                    .map(Organizations::toModel)
                    .collect(Collectors.toSet()));

        return model;
    }

    public static OrganizationDomainModel toModel(OrganizationDomainRepresentation domainRepresentation) {
        return new OrganizationDomainModel(domainRepresentation.getName(), domainRepresentation.isVerified());
    }

    public static InviteOrgActionToken parseInvitationToken(HttpRequest request) throws VerificationException {
        MultivaluedMap<String, String> queryParameters = request.getUri().getQueryParameters();
        String tokenFromQuery = queryParameters.getFirst(Constants.TOKEN);

        if (tokenFromQuery == null) {
            return null;
        }

        return TokenVerifier.create(tokenFromQuery, InviteOrgActionToken.class).getToken();
    }
}
