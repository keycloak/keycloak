package org.keycloak.scim.services.admin;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;

/**
 * Registers the SCIM admin REST resource under {@code /admin/realms/{realm}/scim}.
 */
public class ScimAdminRealmResourceProviderFactory
        implements AdminRealmResourceProviderFactory, EnvironmentDependentProviderFactory {

    @Override
    public String getId() {
        return "scim";
    }

    @Override
    public AdminRealmResourceProvider create(KeycloakSession session) {
        return new ScimAdminRealmResourceProvider();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Feature.SCIM_API);
    }
}
