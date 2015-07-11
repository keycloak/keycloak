package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.ImpersonationConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.DefaultRequiredActions;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MigrateTo1_4_0 {
    public static final ModelVersion VERSION = new ModelVersion("1.4.0");

    public void migrate(KeycloakSession session) {
        List<RealmModel> realms = session.realms().getRealms();
        for (RealmModel realm : realms) {
            if (realm.getAuthenticationFlows().size() == 0) {
                DefaultAuthenticationFlows.addFlows(realm);
                DefaultRequiredActions.addActions(realm);
            }
            ImpersonationConstants.setupImpersonationService(session, realm);

        }

    }
}
