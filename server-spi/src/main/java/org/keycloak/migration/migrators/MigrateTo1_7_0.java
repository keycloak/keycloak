package org.keycloak.migration.migrators;

import java.util.List;

import org.keycloak.migration.MigrationProvider;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MigrateTo1_7_0 {

    public static final ModelVersion VERSION = new ModelVersion("1.7.0");

    public void migrate(KeycloakSession session) {
        List<RealmModel> realms = session.realms().getRealms();
        for (RealmModel realm : realms) {
            // Set default accessToken timeout for implicit flow
            realm.setAccessTokenLifespanForImplicitFlow(Constants.DEFAULT_ACCESS_TOKEN_LIFESPAN_FOR_IMPLICIT_FLOW_TIMEOUT);

            // Add 'admin-cli' builtin client
            MigrationProvider migrationProvider = session.getProvider(MigrationProvider.class);
            migrationProvider.setupAdminCli(realm);

            // add firstBrokerLogin flow and set it to all identityProviders
            DefaultAuthenticationFlows.migrateFlows(realm);
            AuthenticationFlowModel firstBrokerLoginFlow = realm.getFlowByAlias(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW);

            List<IdentityProviderModel> identityProviders = realm.getIdentityProviders();
            for (IdentityProviderModel identityProvider : identityProviders) {
                if (identityProvider.getFirstBrokerLoginFlowId() == null) {
                    identityProvider.setFirstBrokerLoginFlowId(firstBrokerLoginFlow.getId());
                    realm.updateIdentityProvider(identityProvider);
                }
            }
        }
    }
}
