package org.keycloak.testsuite.runonserver;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.credential.CredentialModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * Created by st on 26.01.17.
 */
public class RunHelpers {

    public static FetchOnServerWrapper<RealmRepresentation> internalRealm() {
        return new FetchOnServerWrapper() {

            @Override
            public FetchOnServer getRunOnServer() {
                return (FetchOnServer) session -> ModelToRepresentation.toRepresentation(session, session.getContext().getRealm(), true);
            }

            @Override
            public Class<RealmRepresentation> getResultClass() {
                return RealmRepresentation.class;
            }

        };
    }

    public static FetchOnServerWrapper<ComponentRepresentation> internalComponent(String componentId) {
        return new FetchOnServerWrapper() {

            @Override
            public FetchOnServer getRunOnServer() {
                return (FetchOnServer) session -> ModelToRepresentation.toRepresentation(session, session.getContext().getRealm().getComponent(componentId), true);
            }

            @Override
            public Class<ComponentRepresentation> getResultClass() {
                return ComponentRepresentation.class;
            }

        };
    }

    public static FetchOnServerWrapper<CredentialModel> fetchCredentials(String username) {
        return new FetchOnServerWrapper() {

            @Override
            public FetchOnServer getRunOnServer() {
                return (FetchOnServer) session -> {
                    RealmModel realm = session.getContext().getRealm();
                    UserModel user = session.users().getUserByUsername(realm, username);
                    List<CredentialModel> storedCredentialsByType = user.credentialManager().getStoredCredentialsByTypeStream(CredentialRepresentation.PASSWORD)
                            .collect(Collectors.toList());
                    System.out.println(storedCredentialsByType.size());
                    return storedCredentialsByType.get(0);
                };
            }

            @Override
            public Class getResultClass() {
                return CredentialModel.class;
            }
        };
    }

}
