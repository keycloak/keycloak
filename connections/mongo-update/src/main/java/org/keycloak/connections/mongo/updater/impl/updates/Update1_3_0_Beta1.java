package org.keycloak.connections.mongo.updater.impl.updates;

import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Update1_3_0_Beta1 extends Update {

    @Override
    public String getId() {
        return "1.3.0.Beta1";
    }

    @Override
    public void update(KeycloakSession session) {
        removeField("realms", "passwordCredentialGrantAllowed");
    }

}
