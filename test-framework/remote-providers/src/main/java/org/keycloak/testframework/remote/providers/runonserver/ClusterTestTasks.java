package org.keycloak.testframework.remote.providers.runonserver;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.AuthorizationProviderFactory;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.jgroups.certificates.CertificateReloadManager;
import org.keycloak.jgroups.certificates.DatabaseJGroupsCertificateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.spi.infinispan.JGroupsCertificateProvider;

import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * Serializable server-side tasks used by cluster tests.
 */
public final class ClusterTestTasks {

    private ClusterTestTasks() {
    }

    public static final class HasCertificateReloadManager implements FetchOnServer {
        @Override
        public Object run(KeycloakSession session) {
            return certificateReloadManager(session) != null;
        }
    }

    public static final class CurrentCertificateAlias implements FetchOnServer {
        @Override
        public Object run(KeycloakSession session) {
            return databaseJGroupsCertificateProvider(session).getCurrentCertificate().getAlias();
        }
    }

    public static final class RotateCertificate implements RunOnServer {
        @Override
        public void run(KeycloakSession session) {
            certificateReloadManager(session).rotateCertificate();
        }
    }

    public static final class IsCoordinator implements FetchOnServer {
        @Override
        public Object run(KeycloakSession session) {
            return certificateReloadManager(session).isCoordinator();
        }
    }

    public static final class HasRotationTask implements FetchOnServer {
        @Override
        public Object run(KeycloakSession session) {
            return certificateReloadManager(session).hasRotationTask();
        }
    }

    public static final class ClusterMembersCount implements FetchOnServer {
        @Override
        public Object run(KeycloakSession session) {
            return cacheManager(session).getMembers().size();
        }
    }

    public static final class OverwriteRotationPeriod implements FetchOnServer {
        private final long amount;
        private final ChronoUnit timeUnit;

        public OverwriteRotationPeriod(long amount, ChronoUnit timeUnit) {
            this.amount = amount;
            this.timeUnit = timeUnit;
        }

        @Override
        public Object run(KeycloakSession session) {
            var reloadManager = certificateReloadManager(session);
            if (reloadManager == null) {
                throw new RuntimeException("MTLS is not enabled");
            }
            var provider = databaseJGroupsCertificateProvider(session);
            var originalRotation = provider.getRotationPeriod();
            provider.setRotationPeriod(Duration.of(amount, timeUnit));
            if (reloadManager.isCoordinator()) {
                reloadManager.rotateCertificate();
            }
            return originalRotation.toSeconds();
        }
    }

    public static final class RestoreRotationPeriod implements RunOnServer {
        private final long rotationSeconds;

        public RestoreRotationPeriod(long rotationSeconds) {
            this.rotationSeconds = rotationSeconds;
        }

        @Override
        public void run(KeycloakSession session) {
            var reloadManager = certificateReloadManager(session);
            if (reloadManager == null) {
                throw new RuntimeException("MTLS is not enabled");
            }
            databaseJGroupsCertificateProvider(session).setRotationPeriod(Duration.ofSeconds(rotationSeconds));
            if (reloadManager.isCoordinator()) {
                reloadManager.rotateCertificate();
            }
        }
    }

    public static final class CreateResource implements FetchOnServer {
        private final String realmName;
        private final String clientId;
        private final String resourceName;

        public CreateResource(String realmName, String clientId, String resourceName) {
            this.realmName = realmName;
            this.clientId = clientId;
            this.resourceName = resourceName;
        }

        @Override
        public Object run(KeycloakSession session) throws IOException {
            var realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);
            var factory = (AuthorizationProviderFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(AuthorizationProvider.class);
            var storeFactory = factory.create(session, realm).getStoreFactory();
            var client = session.clients().getClientById(realm, clientId);

            var resourceServer = storeFactory.getResourceServerStore().findByClient(client);
            if (resourceServer == null) {
                resourceServer = storeFactory.getResourceServerStore().create(client);
            }
            return storeFactory.getResourceStore().create(resourceServer, resourceName, clientId).getId();
        }
    }

    public static final class CreateScope implements FetchOnServer {
        private final String realmName;
        private final String clientId;
        private final String scopeName;

        public CreateScope(String realmName, String clientId, String scopeName) {
            this.realmName = realmName;
            this.clientId = clientId;
            this.scopeName = scopeName;
        }

        @Override
        public Object run(KeycloakSession session) throws IOException {
            var realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);
            var storeFactory = session.getProvider(AuthorizationProvider.class).getStoreFactory();
            var client = session.clients().getClientById(realm, clientId);

            var resourceServer = storeFactory.getResourceServerStore().findByClient(client);
            if (resourceServer == null) {
                resourceServer = storeFactory.getResourceServerStore().create(client);
            }
            return storeFactory.getScopeStore().create(resourceServer, scopeName).getId();
        }
    }

    public static final class CreatePermissionTicket implements FetchOnServer {
        private final String realmName;
        private final String clientId;
        private final String scopeId;
        private final String resourceId;
        private final String userId;

        public CreatePermissionTicket(String realmName, String clientId, String scopeId, String resourceId, String userId) {
            this.realmName = realmName;
            this.clientId = clientId;
            this.scopeId = scopeId;
            this.resourceId = resourceId;
            this.userId = userId;
        }

        @Override
        public Object run(KeycloakSession session) {
            var realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);
            var factory = (AuthorizationProviderFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(AuthorizationProvider.class);
            var provider = factory.create(session, realm);
            var storeFactory = provider.getStoreFactory();
            var client = session.clients().getClientById(realm, clientId);

            var resourceServer = storeFactory.getResourceServerStore().findByClient(client);
            var resource = storeFactory.getResourceStore().findById(resourceServer, resourceId);
            var scope = storeFactory.getScopeStore().findById(resourceServer, scopeId);
            var ticket = storeFactory.getPermissionTicketStore().create(resourceServer, resource, scope, userId);
            return ModelToRepresentation.toRepresentation(ticket, provider, false);
        }
    }

    public static final class ReadPermissionTicket implements FetchOnServer {
        private final String realmName;
        private final String clientId;
        private final String ticketId;

        public ReadPermissionTicket(String realmName, String clientId, String ticketId) {
            this.realmName = realmName;
            this.clientId = clientId;
            this.ticketId = ticketId;
        }

        @Override
        public Object run(KeycloakSession session) {
            var realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);
            var factory = (AuthorizationProviderFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(AuthorizationProvider.class);
            var provider = factory.create(session, realm);
            var storeFactory = provider.getStoreFactory();
            var client = session.clients().getClientById(realm, clientId);

            var resourceServer = storeFactory.getResourceServerStore().findByClient(client);
            var ticket = storeFactory.getPermissionTicketStore().findById(resourceServer, ticketId);
            return ticket == null ? null : ModelToRepresentation.toRepresentation(ticket, provider, false);
        }
    }

    public static final class DeletePermissionTicket implements RunOnServer {
        private final String realmName;
        private final String ticketId;

        public DeletePermissionTicket(String realmName, String ticketId) {
            this.realmName = realmName;
            this.ticketId = ticketId;
        }

        @Override
        public void run(KeycloakSession session) {
            var realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);
            var factory = (AuthorizationProviderFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(AuthorizationProvider.class);
            var storeFactory = factory.create(session, realm).getStoreFactory();
            storeFactory.getPermissionTicketStore().delete(ticketId);
        }
    }

    public static final class UpdatePermissionTicketTimestamp implements RunOnServer {
        private final String realmName;
        private final String clientId;
        private final String ticketId;
        private final long grantedTimestamp;

        public UpdatePermissionTicketTimestamp(String realmName, String clientId, String ticketId, long grantedTimestamp) {
            this.realmName = realmName;
            this.clientId = clientId;
            this.ticketId = ticketId;
            this.grantedTimestamp = grantedTimestamp;
        }

        @Override
        public void run(KeycloakSession session) {
            var realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);
            var factory = (AuthorizationProviderFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(AuthorizationProvider.class);
            var storeFactory = factory.create(session, realm).getStoreFactory();
            var client = session.clients().getClientById(realm, clientId);

            var resourceServer = storeFactory.getResourceServerStore().findByClient(client);
            var ticket = storeFactory.getPermissionTicketStore().findById(resourceServer, ticketId);
            ticket.setGrantedTimestamp(grantedTimestamp);
        }
    }

    public static final class ReadPermissionTicketTimestamp implements FetchOnServer {
        private final String realmName;
        private final String clientId;
        private final String ticketId;

        public ReadPermissionTicketTimestamp(String realmName, String clientId, String ticketId) {
            this.realmName = realmName;
            this.clientId = clientId;
            this.ticketId = ticketId;
        }

        @Override
        public Object run(KeycloakSession session) {
            var realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);
            var factory = (AuthorizationProviderFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(AuthorizationProvider.class);
            var storeFactory = factory.create(session, realm).getStoreFactory();
            var client = session.clients().getClientById(realm, clientId);

            var resourceServer = storeFactory.getResourceServerStore().findByClient(client);
            var ticket = storeFactory.getPermissionTicketStore().findById(resourceServer, ticketId);
            return Long.toString(ticket.getGrantedTimestamp());
        }
    }

    private static CertificateReloadManager certificateReloadManager(KeycloakSession session) {
        return GlobalComponentRegistry.componentOf(cacheManager(session), CertificateReloadManager.class);
    }

    private static DatabaseJGroupsCertificateProvider databaseJGroupsCertificateProvider(KeycloakSession session) {
        return (DatabaseJGroupsCertificateProvider) session.getProvider(JGroupsCertificateProvider.class);
    }

    private static EmbeddedCacheManager cacheManager(KeycloakSession session) {
        return session.getProvider(InfinispanConnectionProvider.class)
                .getCache(InfinispanConnectionProvider.USER_CACHE_NAME)
                .getCacheManager();
    }
}
