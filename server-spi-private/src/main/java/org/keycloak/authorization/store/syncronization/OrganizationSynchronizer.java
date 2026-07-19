package org.keycloak.authorization.store.syncronization;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.OrganizationModel.OrganizationRemovedEvent;
import org.keycloak.provider.ProviderFactory;

public class OrganizationSynchronizer implements Synchronizer<OrganizationRemovedEvent> {

    @Override
    public void synchronize(OrganizationRemovedEvent event, KeycloakSessionFactory factory) {
        ProviderFactory<AuthorizationProvider> providerFactory = factory.getProviderFactory(AuthorizationProvider.class);
        AuthorizationProvider authorizationProvider = providerFactory.create(event.getKeycloakSession());

        AdminPermissionsSchema.SCHEMA.removeResourceObject(authorizationProvider, event);
    }
}
