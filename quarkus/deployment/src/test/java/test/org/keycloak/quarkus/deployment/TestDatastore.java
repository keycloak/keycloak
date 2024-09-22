package test.org.keycloak.quarkus.deployment;

import org.keycloak.models.*;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.ExportImportManager;
import org.keycloak.storage.datastore.DefaultDatastoreProvider;
import org.keycloak.storage.datastore.DefaultDatastoreProviderFactory;
import org.keycloak.storage.datastore.DefaultExportImportManager;

public class TestDatastore extends DefaultDatastoreProvider implements DatastoreProvider {
    private KeycloakSession session;

    public TestDatastore(DefaultDatastoreProviderFactory factory, KeycloakSession session) {
        super(factory, session);
        this.session = session;
    }

    @Override
    public AuthenticationSessionProvider authSessions() {
        return session.getProvider(AuthenticationSessionProvider.class);
    }

    @Override
    public ClientScopeProvider clientScopes() {
        return session.getProvider(ClientScopeProvider.class);
    }

    @Override
    public ClientProvider clients() {
        return session.getProvider(ClientProvider.class);
    }

    @Override
    public GroupProvider groups() {
        return session.getProvider(GroupProvider.class);
    }

    @Override
    public UserLoginFailureProvider loginFailures() {
        return session.getProvider(UserLoginFailureProvider.class);
    }

    @Override
    public RealmProvider realms() {
        return session.getProvider(RealmProvider.class);
    }

    @Override
    public RoleProvider roles() {
        return session.getProvider(RoleProvider.class);
    }

    @Override
    public SingleUseObjectProvider singleUseObjects() {
        return session.getProvider(SingleUseObjectProvider.class);
    }

    @Override
    public UserProvider users() {
        return session.getProvider(UserProvider.class);
    }

    @Override
    public UserSessionProvider userSessions() {
        return session.getProvider(UserSessionProvider.class);
    }

    @Override
    public ExportImportManager getExportImportManager() {
        return new DefaultExportImportManager(session);
    }

}
