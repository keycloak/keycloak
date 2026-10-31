package org.keycloak.migration.migrators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.common.Profile;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.migration.ModelVersion;
import org.keycloak.migration.migrators.DomainIdpRoutingMigration.Domain;
import org.keycloak.migration.migrators.DomainIdpRoutingMigration.IdentityProvider;
import org.keycloak.migration.migrators.DomainIdpRoutingMigration.Result;
import org.keycloak.migration.migrators.DomainIdpRoutingMigration.Routing;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
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
        migrateIdpMapperOrganization(session, realm);
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

    private void migrateIdpMapperOrganization(KeycloakSession session, RealmModel realm) {
        RealmModel oldRealm = session.getContext().getRealm();
        try {
            session.getContext().setRealm(realm);
            doMigrateIdpMapperOrganization(session);
        } finally {
            session.getContext().setRealm(oldRealm);
        }
    }

    private void doMigrateIdpMapperOrganization(KeycloakSession session) {
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        if (orgProvider == null) {
            return;
        }

        orgProvider.getAllStream().forEach(org -> {
            org.getIdentityProviders().forEach(idp -> {
                if (idp.getOrganizationIds().size() != 1) {
                    return;
                }

                List<IdentityProviderMapperModel> mappers = session.identityProviders()
                        .getMappersByAliasStream(idp.getAlias()).toList();

                for (IdentityProviderMapperModel mapper : mappers) {
                    Map<String, String> config = mapper.getConfig();

                    if (config == null
                            || !GroupModel.Type.ORGANIZATION.name().equals(config.get(ConfigConstants.GROUP_TYPE))
                            || config.get(ConfigConstants.ORGANIZATION_ID) != null) {
                        continue;
                    }

                    config.put(ConfigConstants.ORGANIZATION_ID, org.getId());
                    session.identityProviders().updateMapper(mapper);
                }
            });
        });
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

        Map<String, OrganizationModel> organizations = new LinkedHashMap<>();
        Map<String, String> organizationNames = new HashMap<>();
        List<Domain> domains = new ArrayList<>();
        List<IdentityProvider> identityProviders = new ArrayList<>();
        Map<String, IdentityProviderModel> identityProviderModels = new LinkedHashMap<>();

        orgProvider.getAllStream().forEach(org -> {
            organizations.put(org.getId(), org);
            organizationNames.put(org.getId(), org.getName());
            org.getDomains().forEach(domain -> domains.add(new Domain(org.getId(), domain.getName())));
            org.getIdentityProviders().forEach(idp -> {
                Map<String, String> config = idp.getConfig();
                String orgDomain = config.get(MigrationUtils.ORGANIZATION_DOMAIN_ATTRIBUTE);
                if (orgDomain == null) {
                    return;
                }
                identityProviders.add(new IdentityProvider(idp.getInternalId(), idp.getAlias(), org.getId(), orgDomain,
                        config.get(MigrationUtils.ORGANIZATION_EXCLUDED_DOMAIN_ATTRIBUTE),
                        Boolean.parseBoolean(config.get(MigrationUtils.ORGANIZATION_REDIRECT_MODE_ATTRIBUTE))));
                identityProviderModels.putIfAbsent(idp.getInternalId(), idp);
            });
        });

        // the organization provider cannot look up domains by name, so names that may be the same are not added
        Result result = DomainIdpRoutingMigration.migrate(organizationNames, domains, identityProviders, null);

        // organization ID -> domain name -> domain
        Map<String, Map<String, OrganizationDomainModel>> changedDomains = new LinkedHashMap<>();

        for (Routing routing : result.routings()) {
            Map<String, OrganizationDomainModel> orgDomains = getDomains(organizations.get(routing.organizationId()), changedDomains);
            OrganizationDomainModel domain = orgDomains.get(routing.name());
            orgDomains.put(domain.getName(), new OrganizationDomainModel(domain.getName(), domain.isVerified(),
                    routing.identityProvider().alias(), routing.identityProvider().autoRedirect()));
        }

        for (Routing domain : result.newDomains()) {
            IdentityProvider idp = domain.identityProvider();
            getDomains(organizations.get(domain.organizationId()), changedDomains)
                    .put(domain.name(), new OrganizationDomainModel(domain.name(), false,
                            idp == null ? null : idp.alias(), idp != null && idp.autoRedirect()));
        }

        changedDomains.forEach((orgId, orgDomains) -> organizations.get(orgId).setDomains(new HashSet<>(orgDomains.values())));

        for (IdentityProviderModel idp : identityProviderModels.values()) {
            Map<String, String> config = idp.getConfig();
            config.remove(MigrationUtils.ORGANIZATION_DOMAIN_ATTRIBUTE);
            config.remove(MigrationUtils.ORGANIZATION_EXCLUDED_DOMAIN_ATTRIBUTE);
            config.remove(MigrationUtils.ORGANIZATION_REDIRECT_MODE_ATTRIBUTE);
            session.identityProviders().update(idp);
        }
    }

    private static Map<String, OrganizationDomainModel> getDomains(OrganizationModel org, Map<String, Map<String, OrganizationDomainModel>> changedDomains) {
        return changedDomains.computeIfAbsent(org.getId(), id -> org.getDomains()
                .collect(Collectors.toMap(OrganizationDomainModel::getName, Function.identity(), (a, b) -> a, LinkedHashMap::new)));
    }
}
