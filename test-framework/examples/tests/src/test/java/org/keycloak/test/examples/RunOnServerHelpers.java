package org.keycloak.test.examples;

import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServerWrapper;

/**
 * Created by st on 26.01.17.
 */
public class RunOnServerHelpers {

    public static InternalRealm internalRealm() {
        return new InternalRealm();
    }

    public static class InternalRealm implements FetchOnServerWrapper<RealmRepresentation> {

        private InternalRealm() {
        }

        @Override
        public FetchOnServer getRunOnServer() {
            return session -> ModelToRepresentation.toRepresentation(session, session.getContext().getRealm(), true);
        }

        @Override
        public Class<RealmRepresentation> getResultClass() {
            return RealmRepresentation.class;
        }

    }

}
