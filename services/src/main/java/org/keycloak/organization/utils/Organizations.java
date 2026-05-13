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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.TokenVerifier;
import org.keycloak.authentication.actiontoken.inviteorg.InviteOrgActionToken;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.CryptoUtils;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.Constants;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupModel.Type;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.protocol.mappers.oidc.OrganizationScope;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.EmailValidationUtil;

import static java.util.Optional.ofNullable;

import static org.keycloak.utils.StringUtil.isBlank;

public class Organizations {

    private static final String WILDCARD_PREFIX = "*.";
    private static final int MIN_DOMAIN_PARTS = 2;
    private static final int MAX_DOMAIN_PARTS = 10;

    public static boolean isOrganizationGroup(GroupModel group) {
        return Type.ORGANIZATION.equals(group.getType()) && group.getOrganization() != null;
    }

    public static boolean canManageOrganizationGroup(KeycloakSession session, GroupModel group) {
        //  if it's not an organization group OR the feature is disabled, we don't need further checks
        if (!isOrganizationGroup(group) || !Profile.isFeatureEnabled(Feature.ORGANIZATION)) {
            return true;
        }

        // if an organization is in context, allow management
        if (resolveOrganization(session) != null) {
            return true;
        }

        // no organization in context, but the group is the internal org group
        return getProvider(session).getById(group.getName()) == null;
    }

    public static List<IdentityProviderModel> resolveHomeBroker(KeycloakSession session, UserModel user) {
        OrganizationProvider provider = getProvider(session);
        RealmModel realm = session.getContext().getRealm();
        List<OrganizationModel> organizations = Optional.ofNullable(user).stream().flatMap(provider::getByMember)
                .filter(OrganizationModel::isEnabled)
                .filter((org) -> org.isManaged(user))
                .toList();

        if (organizations.isEmpty()) {
            return List.of();
        }

        List<IdentityProviderModel> brokers = new ArrayList<>();

        for (OrganizationModel organization : organizations) {
            // user is a managed member, try to resolve the origin broker and redirect automatically
            List<IdentityProviderModel> organizationBrokers = organization.getIdentityProviders().toList();
            session.users().getFederatedIdentitiesStream(realm, user)
                    .map(f -> {
                        IdentityProviderModel broker = session.identityProviders().getByAlias(f.getIdentityProvider());

                        if (!organizationBrokers.contains(broker)) {
                            return null;
                        }

                        FederatedIdentityModel identity = session.users().getFederatedIdentity(realm, user, broker.getAlias());

                        if (identity != null) {
                            return broker;
                        }

                        return null;
                    }).filter(Objects::nonNull)
                    .forEach(brokers::add);
        }

        return brokers;
    }

    public static Consumer<GroupModel> removeGroup(KeycloakSession session, RealmModel realm) {
        return group -> {
            if (!Type.ORGANIZATION.equals(group.getType())) {
                realm.removeGroup(group);
                return;
            }

            OrganizationModel current = resolveOrganization(session);

            try {
                OrganizationProvider provider = getProvider(session);

                session.getContext().setOrganization(provider.getById(group.getName()));

                realm.removeGroup(group);
            } finally {
                session.getContext().setOrganization(current);
            }
        };
    }

    public static boolean isEnabledAndOrganizationsPresent(OrganizationProvider orgProvider) {
        return orgProvider != null && orgProvider.isEnabled() && orgProvider.count() != 0;
    }

    public static boolean isEnabledAndOrganizationsPresent(KeycloakSession session) {
        if (!Profile.isFeatureEnabled(Feature.ORGANIZATION)) {
            return false;
        }

        OrganizationProvider provider = getProvider(session);

        return isEnabledAndOrganizationsPresent(provider);
    }

    public static void checkEnabled(OrganizationProvider provider, AdminPermissionEvaluator auth) {
        if (provider == null || !provider.isEnabled()) {
            throw auth.orgs().canQuery() ?
                    ErrorResponse.error("Organizations not enabled for this realm.", Response.Status.NOT_FOUND) :
                    new ForbiddenException();
        }
    }

    public static InviteOrgActionToken parseInvitationToken(KeycloakSession session, HttpRequest request) throws VerificationException {
        MultivaluedMap<String, String> queryParameters = request.getUri().getQueryParameters();
        String tokenFromQuery = queryParameters.getFirst(Constants.TOKEN);

        if (tokenFromQuery == null) {
            return null;
        }

        KeycloakContext context = session.getContext();
        RealmModel realm = session.getContext().getRealm();
        TokenVerifier<InviteOrgActionToken> verifier = TokenVerifier.create(tokenFromQuery, InviteOrgActionToken.class)
                .withChecks(TokenVerifier.IS_ACTIVE,
                        new TokenVerifier.RealmUrlCheck(Urls.realmIssuer(context.getUri().getBaseUri(), realm.getName())));

        SignatureVerifierContext verifierContext = CryptoUtils.getSignatureProvider(session, verifier.getHeader().getAlgorithm().name()).verifier(verifier.getHeader().getKeyId());
        verifier.verifierContext(verifierContext);

        return verifier.verify().getToken();
    }

    public static int getDomainPartsSize(String domain) {
        if (isBlank(domain)) {
            return 0;
        }
        return Math.toIntExact(domain.chars().filter(c -> c == '.').count()) + 1;
    }

    public static void validateDomain(String rawDomain) {
        if (rawDomain == null) {
            return;
        }

        String domain = rawDomain;

        if (rawDomain.contains(WILDCARD_PREFIX)) {
            if (rawDomain.length() == WILDCARD_PREFIX.length()) {
                throw new ModelValidationException("Wildcard domain must specify a base domain: " + rawDomain);
            }

            if (!rawDomain.startsWith(WILDCARD_PREFIX)) {
                throw new ModelValidationException("Wildcard domain must start with the wildcard");
            }

            domain = rawDomain.substring(2);

            if (domain.contains("*")) {
                throw new ModelValidationException("Multiple wildcards are not allowed: " + rawDomain);
            }

            int parts = getDomainPartsSize(domain);

            if (parts < MIN_DOMAIN_PARTS) {
                throw new ModelValidationException("Domain must have at least " + MIN_DOMAIN_PARTS + " parts (e.g. 'example.com'): " + domain);
            }

            if (parts > MAX_DOMAIN_PARTS) {
                throw new ModelValidationException("Domain has too many parts (max " + MAX_DOMAIN_PARTS + " allowed): " + domain);
            }
        }

        if (isBlank(domain) || !EmailValidationUtil.isValidEmail("user@" + domain)) {
            throw new ModelValidationException("Invalid domain format: " + rawDomain);
        }
    }


    /**
     * Returns the most specific matching organization domain for the given {@code domain} and
     * {@code organization}. When several domains of the organization match (e.g. an exact domain
     * and a parent wildcard, or nested wildcards), the one with the largest number of parts wins.
     *
     * @param domain the domain
     * @param organization the organization
     * @return the most specific matching organization domain, or {@code null} if no match is found
     */
    public static OrganizationDomainModel getMatchingDomain(String domain, OrganizationModel organization) {
        if (domain == null || organization == null) {
            return null;
        }

        List<OrganizationDomainModel> domains = organization.getDomains().filter(model -> isSameDomain(domain, model))
                // sorted ascending by number of domain parts so the most specific match is the last element
                .sorted(Comparator.comparingInt(o -> getDomainPartsSize(o.getName())))
                .toList();

        if (domains.isEmpty()) {
            return null;
        }

        return domains.get(domains.size() - 1);
    }

    public static boolean isSameDomain(String domain, OrganizationDomainModel model) {
        return isSameDomain(domain, ofNullable(model).map(OrganizationDomainModel::getName).orElse(null));
    }

    public static boolean isSameDomain(String domain, String expectedDomain) {
        if (domain == null || expectedDomain == null) {
            return false;
        }

        String canonicalDomain = domain.toLowerCase();
        String pattern = expectedDomain.toLowerCase();

        if (canonicalDomain.equals(pattern)) {
            return true;
        }

        if (pattern.startsWith(WILDCARD_PREFIX)) {
            String baseDomain = pattern.substring(2);
            return canonicalDomain.equals(baseDomain) || canonicalDomain.endsWith("." + baseDomain);
        }

        return false;
    }

    public static String getEmailDomain(String email) {
        if (email == null) {
            return null;
        }

        int domainSeparator = email.indexOf('@');

        if (domainSeparator == -1) {
            return null;
        }

        return email.substring(domainSeparator + 1);
    }

    public static String getEmailDomain(UserModel user) {
        if (user == null) {
            return null;
        }
        return getEmailDomain(user.getEmail());
    }

    public static OrganizationModel resolveOrganization(KeycloakSession session) {
        return resolveOrganization(session, null, null);
    }

    public static OrganizationModel resolveOrganization(KeycloakSession session, UserModel user) {
        return resolveOrganization(session, user, null);
    }

    public static OrganizationModel resolveOrganization(KeycloakSession session, UserModel user, String domain) {
        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();

        if (!realm.isOrganizationsEnabled()) {
            return null;
        }

        OrganizationModel current = context.getOrganization();

        if (current != null) {
            // resolved from current keycloak session
            return current;
        }

        OrganizationProvider provider = getProvider(session);

        if (provider.count() == 0) {
            return null;
        }

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String emailDomain = ofNullable(domain).orElseGet(() -> getEmailDomain(user));

        if (authSession != null) {
            OrganizationScope scope = OrganizationScope.valueOfScope(session);
            List<OrganizationModel> organizations = ofNullable(authSession.getAuthNote(OrganizationModel.ORGANIZATION_ATTRIBUTE))
                    .map(provider::getById)
                    .map(List::of)
                    .orElseGet(() -> scope == null ? List.of() : scope.resolveOrganizations(user, session).toList());

            if (organizations.size() == 1) {
                OrganizationModel organization = organizations.get(0);

                if (user == null) {
                    return organization;
                }

                // make sure the user still maps to the organization from the authentication session
                if (organization.isMember(user)) {
                    return organization;
                }

                return resolveByDomain(organizations, emailDomain);
            } else if (scope != null && user != null) {
                return resolveByDomain(organizations, emailDomain);
            }
        }

        List<OrganizationModel> organizations = ofNullable(user).stream()
                .flatMap(provider::getByMember)
                .filter(OrganizationModel::isEnabled)
                .toList();

        if (organizations.size() == 1) {
            // single membership found, return the org
            return organizations.get(0);
        }

        if (organizations.isEmpty()) {
            // no membership, any org that matches the domain
            return resolveByDomain(ofNullable(emailDomain)
                    .map(provider::getByDomainName)
                    .map(List::of)
                    .orElse(List.of()), emailDomain);
        }

        for (OrganizationModel organization : organizations) {
            if (organization.isManaged(user)) {
                return organization;
            }
        }

        return resolveByDomain(organizations, emailDomain);
    }

    public static OrganizationProvider getProvider(KeycloakSession session) {
        return session.getProvider(OrganizationProvider.class);
    }

    public static boolean isRegistrationAllowed(KeycloakSession session, RealmModel realm) {
        if (session.getContext().getOrganization() != null) return true;
        return realm.isRegistrationAllowed();
    }

    public static boolean isReadOnlyOrganizationMember(KeycloakSession session, UserModel delegate) {
        if (delegate == null) {
            return false;
        }

        if (!Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
            return false;
        }

        var organizationProvider = getProvider(session);

        if (organizationProvider.count() == 0) {
            return false;
        }

        // check if provider is enabled and user is managed member of a disabled organization OR provider is disabled and user is managed member
        return organizationProvider.getByMember(delegate)
                .anyMatch((org) -> (organizationProvider.isEnabled() && org.isManaged(delegate) && !org.isEnabled()) ||
                        (!organizationProvider.isEnabled() && org.isManaged(delegate)));
    }

    public static OrganizationModel resolveByDomain(List<OrganizationModel> organizations, String domain) {
        int bestParts = -1;
        OrganizationModel organization = null;

        for (OrganizationModel model : organizations) {
            OrganizationDomainModel bestMatch = getMatchingDomain(domain, model);

            if (bestMatch == null) {
                continue;
            }

            if (organizations.size() == 1) {
                // only one organization, any domain match is enough
                return model;
            }

            int mostSpecificParts = getDomainPartsSize(bestMatch.getName());

            if (mostSpecificParts > bestParts) {
                bestParts = mostSpecificParts;
                organization = model;
            }
        }

        return organization;
    }
}
