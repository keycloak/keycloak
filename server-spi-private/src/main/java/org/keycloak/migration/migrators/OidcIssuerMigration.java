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
package org.keycloak.migration.migrators;

import java.util.Arrays;

import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.provider.SingleIssuerIdentityProviderConfig;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.RealmRepresentation;

import org.jboss.logging.Logger;

/**
 * Migration of legacy issuer lists for providers with single-issuer configurations. Federated links do not record an issuer,
 * so a migrated provider must be reviewed before it can safely be enabled again.
 * <p>
 * This intentionally does not implement {@link Migration} and is not registered in {@code DefaultMigrationManager}.
 * Identity providers are validated eagerly while a realm representation is imported (see
 * {@code OIDCIdentityProviderConfig#validate}), before the standard post-import {@code migrateImport} hooks ever run,
 * so a multi-issuer configuration would fail validation before any registered migrator got a chance to fix it.
 * Instead, {@link #migrateImport(KeycloakSession, RealmRepresentation)} is called directly against the representation
 * before identity providers are created.
 */
public final class OidcIssuerMigration {

    public static final ModelVersion VERSION = new ModelVersion("27.0.0");
    private static final Logger LOG = Logger.getLogger(OidcIssuerMigration.class);

    private OidcIssuerMigration() {
    }

    /**
     * Resolves the factory for the given provider id, or {@code null} when that provider is not installed.
     */
    public static IdentityProviderFactory<?> getFactory(KeycloakSession session, String providerId) {
        IdentityProviderFactory<?> factory = (IdentityProviderFactory<?>) session.getKeycloakSessionFactory()
                .getProviderFactory(IdentityProvider.class, providerId);
        if (factory == null) {
            factory = (IdentityProviderFactory<?>) session.getKeycloakSessionFactory()
                    .getProviderFactory(SocialIdentityProvider.class, providerId);
        }
        return factory;
    }

    public static boolean isMultiIssuerProvider(KeycloakSession session, String providerId, String issuer) {
        if (issuer == null || !issuer.contains(",")) {
            return false;
        }
        IdentityProviderFactory<?> factory = getFactory(session, providerId);
        return factory != null && factory.createConfig() instanceof SingleIssuerIdentityProviderConfig;
    }

    public static String firstIssuer(String issuers) {
        return Arrays.stream(issuers.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElse(null);
    }

    public static void migrateImport(KeycloakSession session, RealmRepresentation realm) {
        if (realm.getIdentityProviders() == null) {
            return;
        }
        realm.getIdentityProviders().forEach(idp -> {
            if (idp.getConfig() == null) {
                return;
            }
            String issuers = idp.getConfig().get(IdentityProviderModel.ISSUER);
            if (isMultiIssuerProvider(session, idp.getProviderId(), issuers)) {
                String first = firstIssuer(issuers);
                if (first != null) {
                    idp.getConfig().put(IdentityProviderModel.ISSUER, first);
                    LOG.warnf("Disabled identity provider '%s' in imported realm '%s' because it configured multiple issuers (%s). "
                            + "The same subject from different issuers could authenticate as the wrong user. Only the first issuer was retained. "
                            + "Review existing federated links before enabling the provider. Configure a separate identity provider for each additional issuer; "
                            + "existing user links cannot be split automatically.",
                            idp.getAlias(), realm.getRealm(), issuers);
                } else {
                    LOG.warnf("Disabled identity provider '%s' in imported realm '%s' because its issuer value (%s) "
                            + "contains commas but no valid issuer. Correct the issuer before enabling the provider.",
                            idp.getAlias(), realm.getRealm(), issuers);
                }
                idp.setEnabled(false);
            }
        });
    }
}
