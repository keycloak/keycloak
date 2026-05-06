package org.keycloak.testsuite.util.runonserver;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.credential.CredentialModel;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.email.EmailEventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServerWrapper;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;

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
                    return storedCredentialsByType.get(0);
                };
            }

            @Override
            public Class getResultClass() {
                return CredentialModel.class;
            }
        };
    }

    public static RunOnServer removeUserSession(String realmName, String sessionId) {
        return session -> {
            RealmModel realm = getRealmByName(session, realmName);

            UserSessionModel sessionModel = session.sessions().getUserSession(realm, sessionId);
            if (sessionModel == null) {
                throw new NotFoundException("Session not found");
            }

            session.sessions().removeUserSession(realm, sessionModel);
        };
    }

    public static RunOnServer removeUserSessions(String realmName) {
        return session -> {
            RealmModel realm = getRealmByName(session, realmName);

            session.sessions().removeUserSessions(realm);
        };
    }

    public static FetchOnServerWrapper<Integer> getClientSessionsCountInUserSession(String realmName, String sessionId) {
        return new FetchOnServerWrapper<>() {
            @Override
            public FetchOnServer getRunOnServer() {
                return session -> {
                    RealmModel realm = getRealmByName(session, realmName);

                    UserSessionModel sessionModel = session.sessions().getUserSession(realm, sessionId);
                    if (sessionModel == null) {
                        throw new NotFoundException("Session not found");
                    }

                    // TODO: Might need optimization to prevent loading client sessions from cache
                    return sessionModel.getAuthenticatedClientSessions().size();
                };
            }

            @Override
            public Class<Integer> getResultClass() {
                return Integer.class;
            }
        };
    }

    public static RunOnServer removeExpired(String realmName) {
        return session -> {
            RealmModel realm = getRealmByName(session, realmName);

            session.getProvider(UserSessionPersisterProvider.class).removeExpired(realm);
            session.realms().removeExpiredClientInitialAccess();
        };
    }

    public static RunOnServer addEventsToEmailEventListenerProvider(List<EventType> events) {
        return session -> {
            if (events != null && !events.isEmpty()) {
                EmailEventListenerProviderFactory prov = (EmailEventListenerProviderFactory) session.getKeycloakSessionFactory()
                        .getProviderFactory(EventListenerProvider.class, EmailEventListenerProviderFactory.ID);
                prov.addIncludedEvents(events.toArray(EventType[]::new));
            }
        };
    }

    public static RunOnServer removeEventsToEmailEventListenerProvider(List<EventType> events) {
        return session -> {
            if (events != null && !events.isEmpty()) {
                EmailEventListenerProviderFactory prov = (EmailEventListenerProviderFactory) session.getKeycloakSessionFactory()
                        .getProviderFactory(EventListenerProvider.class, EmailEventListenerProviderFactory.ID);
                prov.removeIncludedEvents(events.toArray(EventType[]::new));
            }
        };
    }

    private static RealmModel getRealmByName(KeycloakSession session, String realmName) {
        RealmProvider realmProvider = session.getProvider(RealmProvider.class);
        RealmModel realm = realmProvider.getRealmByName(realmName);
        if (realm == null) {
            throw new NotFoundException("Realm not found");
        }
        return realm;
    }

}
