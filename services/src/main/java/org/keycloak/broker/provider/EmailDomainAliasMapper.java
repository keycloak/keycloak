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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jboss.logging.Logger;

import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Rewrites the brokered email from an alias domain to one canonical domain before the first broker login flow runs,
 * so that the same mailbox under two domains (for example after a company rename) resolves to one Keycloak user.
 * Add it to every identity provider whose users should share accounts.
 */
public class EmailDomainAliasMapper extends AbstractIdentityProviderMapper {

    private static final Logger logger = Logger.getLogger(EmailDomainAliasMapper.class);

    public static final String PROVIDER_ID = "email-domain-alias-idp-mapper";
    public static final String CANONICAL_DOMAIN = "canonical.domain";
    public static final String ALIAS_DOMAINS = "alias.domains";

    public static final String[] COMPATIBLE_PROVIDERS = {ANY_PROVIDER};

    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(CANONICAL_DOMAIN);
        property.setLabel("Canonical domain");
        property.setHelpText("Domain the email is rewritten to, for example example.com.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(ALIAS_DOMAINS);
        property.setLabel("Alias domains");
        property.setHelpText("Comma-separated domains whose addresses are rewritten to the canonical domain, for example example.org. "
                + "Only list domains whose mailboxes map one-to-one onto the canonical domain.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getDisplayCategory() {
        return "Attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "Email Domain Alias";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getHelpText() {
        return "Rewrite the email from an alias domain to the canonical domain, so one person logging in through "
                + "different identity providers or addresses is matched to one user. With sync mode force, an existing "
                + "user's address in an alias domain is also moved to the canonical domain.";
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        // Runs before the first broker login flow looks up existing users by email, so new users are created,
        // and existing ones found, with the canonical address.
        context.setEmail(canonicalize(context.getEmail(), mapperModel.getConfig().get(CANONICAL_DOMAIN), aliasDomains(mapperModel)));
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        // Sync mode force: the broker does not write the email to existing users, so move the stored address from
        // an alias domain to the canonical domain here. Only the domain changes, and the domains map one-to-one onto
        // the same mailboxes, so the email verified flag is kept.
        String current = user.getEmail();
        String canonical = canonicalize(current, mapperModel.getConfig().get(CANONICAL_DOMAIN), aliasDomains(mapperModel));
        if (canonical == null || canonical.equalsIgnoreCase(current)) {
            return;
        }
        UserModel existing;
        try {
            existing = session.users().getUserByEmail(realm, canonical);
        } catch (ModelDuplicateException e) {
            // Several users already have the address (realm allows duplicate emails): leave this one as it is.
            logger.warnf("Not changing the email of user '%s' to '%s': %s", user.getUsername(), canonical, e.getMessage());
            return;
        }
        if (existing != null && !existing.getId().equals(user.getId())) {
            logger.warnf("Not changing the email of user '%s' to '%s': it belongs to user '%s'",
                    user.getUsername(), canonical, existing.getUsername());
            return;
        }
        user.setEmail(canonical);
    }

    private static List<String> aliasDomains(IdentityProviderMapperModel mapperModel) {
        List<String> aliasDomains = new ArrayList<>();
        String aliases = mapperModel.getConfig().get(ALIAS_DOMAINS);
        if (aliases != null) {
            for (String alias : aliases.split(",")) {
                aliasDomains.add(alias.trim());
            }
        }
        return aliasDomains;
    }

    /**
     * Return {@code email} with its domain replaced by {@code canonicalDomain} when its domain is exactly one of
     * {@code aliasDomains} (case-insensitive). Any other value is returned unchanged.
     */
    static String canonicalize(String email, String canonicalDomain, Collection<String> aliasDomains) {
        if (email == null || canonicalDomain == null || canonicalDomain.trim().isEmpty()) {
            return email;
        }
        int at = email.lastIndexOf('@');
        if (at < 0) {
            return email;
        }
        String domain = email.substring(at + 1).toLowerCase(Locale.ROOT);
        for (String alias : aliasDomains) {
            if (domain.equals(alias.trim().toLowerCase(Locale.ROOT))) {
                return email.substring(0, at + 1) + canonicalDomain.trim().toLowerCase(Locale.ROOT);
            }
        }
        return email;
    }
}
