package org.keycloak.testsuite.util.runonserver;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.Config;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.email.EmailEventListenerProviderFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.encode.AccessTokenContext;
import org.keycloak.protocol.oidc.encode.TokenContextEncoderProvider;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
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

    public static RunOnServer removeUserSession(String sessionId) {
        return session -> {
            RealmModel realm = session.getContext().getRealm();

            UserSessionModel sessionModel = session.sessions().getUserSession(realm, sessionId);
            if (sessionModel == null) {
                throw new NotFoundException("Session not found");
            }

            session.sessions().removeUserSession(realm, sessionModel);
        };
    }

    public static RunOnServer removeUserSessions() {
        return session -> {
            RealmModel realm = session.getContext().getRealm();
            session.sessions().removeUserSessions(realm);
        };
    }

    public static FetchOnServerWrapper<Integer> getClientSessionsCountInUserSession(String sessionId) {
        return new FetchOnServerWrapper<>() {
            @Override
            public FetchOnServer getRunOnServer() {
                return session -> {
                    RealmModel realm = session.getContext().getRealm();

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

    public static RunOnServer removeExpired() {
        return session -> {
            RealmModel realm = session.getContext().getRealm();

            session.getProvider(UserSessionPersisterProvider.class).removeExpired(realm);
            session.realms().removeExpiredClientInitialAccess();
        };
    }

    /**
     * Adds the following types to the email event listener included list.
     * @param events The events to be included
     */
    public static RunOnServer addEventsToEmailEventListenerProvider(List<EventType> events) {
        return session -> {
            if (events != null && !events.isEmpty()) {
                EmailEventListenerProviderFactory prov = (EmailEventListenerProviderFactory) session.getKeycloakSessionFactory()
                        .getProviderFactory(EventListenerProvider.class, EmailEventListenerProviderFactory.ID);
                prov.addIncludedEvents(events.toArray(EventType[]::new));
            }
        };
    }

    /**
     * Removes the following types from the email event listener included list.
     * @param events The events to be removed
     */
    public static RunOnServer removeEventsToEmailEventListenerProvider(List<EventType> events) {
        return session -> {
            if (events != null && !events.isEmpty()) {
                EmailEventListenerProviderFactory prov = (EmailEventListenerProviderFactory) session.getKeycloakSessionFactory()
                        .getProviderFactory(EventListenerProvider.class, EmailEventListenerProviderFactory.ID);
                prov.removeIncludedEvents(events.toArray(EventType[]::new));
            }
        };
    }

    public static FetchOnServerWrapper<AccessTokenContext> getTokenContext(String tokenId) {
        return new FetchOnServerWrapper<>() {
            @Override
            public FetchOnServer getRunOnServer() {
                return session -> session.getProvider(TokenContextEncoderProvider.class).getTokenContextFromTokenId(tokenId);
            }

            @Override
            public Class<AccessTokenContext> getResultClass() {
                return AccessTokenContext.class;
            }
        };
    }

    /**
     * If property-value is null, the system property will be unset (removed) on the server
     */
    public static RunOnServer setSystemPropertyOnServer(String propertyName, String propertyValue) {
        return session -> {
            if (propertyValue == null) {
                System.getProperties().remove(propertyName);
            } else {
                System.setProperty(propertyName, propertyValue);
            }
        };
    }

    /**
     * Re-initialize specified provider factory with system properties scope. This will allow to change providerConfig in runtime with {@link #setSystemPropertyOnServer}
     * <p>
     * This works just for the provider factories, which can be re-initialized without any side-effects (EG. some functionality already dependent
     * on the previously initialized properties, which cannot be easily changed in runtime)
     *
     * @param providerType           fully qualified class name of provider (subclass of org.keycloak.provider.Provider)
     * @param providerId             provider Id
     * @param systemPropertiesPrefix prefix to be used for system properties
     */
    public static RunOnServer reinitializeProviderFactoryWithSystemPropertiesScope(String providerType, String providerId, String systemPropertiesPrefix) {
        return session -> {
            Class<? extends Provider> providerClass = (Class<? extends Provider>) Class.forName(providerType);
            ProviderFactory<?> factory = session.getKeycloakSessionFactory().getProviderFactory(providerClass, providerId);
            factory.init(new Config.SystemPropertiesScope(systemPropertiesPrefix));
        };
    }

}
