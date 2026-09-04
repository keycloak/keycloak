package org.keycloak.migration.migrators;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.Profile;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.RealmModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.RealmRepresentation;

public class MigrateTo26_8_0 extends RealmMigration {

    public static final ModelVersion VERSION = new ModelVersion("26.8.0");

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        migrateDomainIdpRouting(session, realm);
        super.migrateImport(session, realm, rep, skipUserDependent);
    }

    @Override
    public void migrateRealm(KeycloakSession session, RealmModel realm) {
        AdminPermissionsSchema.SCHEMA.addResourceTypeScope(session, realm, AdminPermissionsSchema.USERS_RESOURCE_TYPE, AdminPermissionsSchema.DELEGATE);
        AdminPermissionsSchema.SCHEMA.addResourceTypeScope(session, realm, AdminPermissionsSchema.GROUPS_RESOURCE_TYPE, AdminPermissionsSchema.DELEGATE_MEMBERS);

        if (Profile.isFeatureEnabled(Profile.Feature.TOKEN_EXCHANGE_DELEGATION)) {
            MigrationProvider migrationProvider = session.getProvider(MigrationProvider.class);
            // FGAP guards delegation permission evaluation, so scopes can safely be optional
            ClientScopeModel userDelegation = migrationProvider.addOIDCUserDelegationClientScope(realm);
            realm.addDefaultClientScope(userDelegation, false);
            ClientScopeModel clientDelegation = migrationProvider.addOIDCClientDelegationClientScope(realm);
            realm.addDefaultClientScope(clientDelegation, false);
        }
    }

    private void migrateDomainIdpRouting(KeycloakSession session, RealmModel realm) {
        RealmModel oldRealm = session.getContext().getRealm();
        try {
            session.getContext().setRealm(realm);
            doMigrateDomainIdpRouting(session);
        } finally {
            session.getContext().setRealm(oldRealm);
        }
    }

    private void doMigrateDomainIdpRouting(KeycloakSession session) {
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        if (orgProvider == null) {
            return;
        }

        orgProvider.getAllStream().forEach(org -> {
            org.getIdentityProviders().forEach(idp -> {
                Map<String, String> config = idp.getConfig();
                String orgDomain = config.get(MigrationUtils.ORGANIZATION_DOMAIN_ATTRIBUTE);
                if (orgDomain == null) {
                    return;
                }

                Set<OrganizationDomainModel> currentDomains = org.getDomains().collect(Collectors.toSet());
                String excludedDomainsValue = config.get(MigrationUtils.ORGANIZATION_EXCLUDED_DOMAIN_ATTRIBUTE);
                Set<String> excludedDomains = new HashSet<>();
                if (excludedDomainsValue != null) {
                    for (String d : excludedDomainsValue.split(",")) {
                        excludedDomains.add(d.trim());
                    }
                }

                boolean autoRedirect = Boolean.parseBoolean(config.get(MigrationUtils.ORGANIZATION_REDIRECT_MODE_ATTRIBUTE));

                Set<OrganizationDomainModel> result = new HashSet<>();
                for (OrganizationDomainModel domain : currentDomains) {
                    if ("ANY".equals(orgDomain)) {
                        if (!excludedDomains.contains(domain.getName())) {
                            result.add(new OrganizationDomainModel(domain.getName(), domain.isVerified(), idp.getAlias(), autoRedirect));
                        } else {
                            result.add(domain);
                        }
                    } else if (orgDomain.equals(domain.getName())) {
                        result.add(new OrganizationDomainModel(domain.getName(), domain.isVerified(), idp.getAlias(), autoRedirect));
                    } else {
                        result.add(domain);
                    }
                }

                if ("ANY".equals(orgDomain)) {
                    Set<String> existingNames = result.stream()
                            .map(OrganizationDomainModel::getName)
                            .collect(Collectors.toSet());
                    for (String excluded : excludedDomains) {
                        if (!existingNames.contains(excluded)) {
                            result.add(new OrganizationDomainModel(excluded, false, null, false));
                        }
                    }
                }

                org.setDomains(result);

                config.remove(MigrationUtils.ORGANIZATION_DOMAIN_ATTRIBUTE);
                config.remove(MigrationUtils.ORGANIZATION_EXCLUDED_DOMAIN_ATTRIBUTE);
                config.remove(MigrationUtils.ORGANIZATION_REDIRECT_MODE_ATTRIBUTE);
                session.identityProviders().update(idp);
            });
        });
    }
}
