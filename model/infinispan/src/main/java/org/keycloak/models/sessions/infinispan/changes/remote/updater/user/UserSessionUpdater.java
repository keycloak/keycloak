package org.keycloak.models.sessions.infinispan.changes.remote.updater.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.infinispan.client.hotrod.MetadataValue;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.BaseUpdater;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.Expiration;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.Updater;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.UpdaterFactory;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.helper.MapUpdater;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionStore;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;

/**
 * The {@link Updater} implementation to keep track of modifications for {@link UserSessionModel}.
 */
public class UserSessionUpdater extends BaseUpdater<String, UserSessionEntity> implements UserSessionModel {

    private static final Factory ONLINE = new Factory(false);
    private static final Factory OFFLINE = new Factory(true);

    private final MapUpdater<String, String> notesUpdater;
    private final List<Consumer<UserSessionEntity>> changes;
    private final boolean offline;
    private RealmModel realm;
    private UserModel user;
    private ClientSessionMappingAdapter clientSessionMappingAdapter;
    private SessionPersistenceState persistenceState = SessionPersistenceState.PERSISTENT;

    private UserSessionUpdater(String cacheKey, UserSessionEntity cacheValue, long version, boolean offline, UpdaterState initialState) {
        super(cacheKey, cacheValue, version, initialState);
        this.offline = offline;
        if (cacheValue == null) {
            assert initialState == UpdaterState.DELETED;
            // cannot undelete
            changes = List.of();
            notesUpdater = null;
            return;
        }
        initNotes(cacheValue);
        notesUpdater = new MapUpdater<>(cacheValue.getNotes());
        changes = new ArrayList<>(4);
    }

    /**
     * @param offline If {@code true}, it creates offline {@link UserSessionModel}.
     * @return The {@link UpdaterFactory} implementation to create instances of {@link UserSessionModel}.
     */
    public static UpdaterFactory<String, UserSessionEntity, UserSessionUpdater> factory(boolean offline) {
        return offline ? OFFLINE : ONLINE;
    }

    @Override
    public UserSessionEntity apply(String ignored, UserSessionEntity userSessionEntity) {
        initNotes(userSessionEntity);
        initStore(userSessionEntity);
        changes.forEach(change -> change.accept(userSessionEntity));
        notesUpdater.applyChanges(userSessionEntity.getNotes());
        clientSessionMappingAdapter.applyChanges(userSessionEntity.getAuthenticatedClientSessions());
        return userSessionEntity;
    }

    @Override
    public Expiration computeExpiration() {
        long maxIdle;
        long lifespan;
        if (offline) {
            maxIdle = SessionTimeouts.getOfflineSessionMaxIdleMs(realm, null, getValue());
            lifespan = SessionTimeouts.getOfflineSessionLifespanMs(realm, null, getValue());
        } else {
            maxIdle = SessionTimeouts.getUserSessionMaxIdleMs(realm, null, getValue());
            lifespan = SessionTimeouts.getUserSessionLifespanMs(realm, null, getValue());
        }
        return new Expiration(maxIdle, lifespan);
    }

    @Override
    public String getId() {
        return getValue().getId();
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public String getBrokerSessionId() {
        return getValue().getBrokerSessionId();
    }

    @Override
    public String getBrokerUserId() {
        return getValue().getBrokerUserId();
    }

    @Override
    public UserModel getUser() {
        return user;
    }

    @Override
    public String getLoginUsername() {
        return getValue().getLoginUsername();
    }

    @Override
    public String getIpAddress() {
        return getValue().getIpAddress();
    }

    @Override
    public String getAuthMethod() {
        return getValue().getAuthMethod();
    }

    @Override
    public boolean isRememberMe() {
        return getValue().isRememberMe();
    }

    @Override
    public int getStarted() {
        return getValue().getStarted();
    }

    @Override
    public int getLastSessionRefresh() {
        return getValue().getLastSessionRefresh();
    }

    @Override
    public void setLastSessionRefresh(int seconds) {
        addAndApplyChange(userSessionEntity -> userSessionEntity.setLastSessionRefresh(seconds));
    }

    @Override
    public boolean isOffline() {
        return offline;
    }

    @Override
    public Map<String, AuthenticatedClientSessionModel> getAuthenticatedClientSessions() {
        return clientSessionMappingAdapter;
    }

    @Override
    public void removeAuthenticatedClientSessions(Collection<String> removedClientUUIDS) {
        clientSessionMappingAdapter.removeAll(removedClientUUIDS);
    }

    @Override
    public AuthenticatedClientSessionModel getAuthenticatedClientSessionByClient(String clientUUID) {
        return clientSessionMappingAdapter.get(clientUUID);
    }

    @Override
    public String getNote(String name) {
        return notesUpdater.get(name);
    }

    @Override
    public void setNote(String name, String value) {
        if (value == null) {
            removeNote(name);
        } else {
            notesUpdater.put(name, value);
        }
    }

    @Override
    public void removeNote(String name) {
        notesUpdater.remove(name);
    }

    @Override
    public Map<String, String> getNotes() {
        return notesUpdater;
    }

    @Override
    public State getState() {
        return getValue().getState();
    }

    @Override
    public void setState(State state) {
        addAndApplyChange(userSessionEntity -> userSessionEntity.setState(state));
    }

    @Override
    public void restartSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
        this.realm = realm;
        this.user = user;
        changes.clear();
        notesUpdater.clear();
        clientSessionMappingAdapter.clear();
        addAndApplyChange(userSessionEntity -> UserSessionEntity.updateSessionEntity(userSessionEntity, realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId));
    }

    @Override
    public SessionPersistenceState getPersistenceState() {
        return persistenceState;
    }

    @Override
    public boolean isTransient() {
        return !isDeleted() && persistenceState == SessionPersistenceState.TRANSIENT;
    }

    @Override
    protected boolean isUnchanged() {
        return changes.isEmpty() && notesUpdater.isUnchanged() && clientSessionMappingAdapter.isUnchanged();
    }

    /**
     * Initializes this class with references to other models classes.
     *
     * @param persistenceState The {@link SessionPersistenceState}.
     * @param realm            The {@link RealmModel} to where this user session belongs.
     * @param user             The {@link UserModel} associated to this user session.
     * @param factory          The {@link ClientSessionAdapterFactory} to create the {@link ClientSessionMappingAdapter}
     *                         to track modifications into the client sessions.
     */
    public synchronized void initialize(SessionPersistenceState persistenceState, RealmModel realm, UserModel user, ClientSessionAdapterFactory factory) {
        initStore(getValue());
        this.realm = Objects.requireNonNull(realm);
        this.user = Objects.requireNonNull(user);
        this.persistenceState = Objects.requireNonNull(persistenceState);
        clientSessionMappingAdapter = factory.create(getValue().getAuthenticatedClientSessions());
    }

    /**
     * @return {@code true} if it is already initialized.
     */
    public synchronized boolean isInitialized() {
        return realm != null;
    }

    private void addAndApplyChange(Consumer<UserSessionEntity> change) {
        change.accept(getValue());
        changes.add(change);
    }

    private static void initNotes(UserSessionEntity entity) {
        var notes = entity.getNotes();
        if (notes == null) {
            entity.setNotes(new HashMap<>());
        }
    }

    private static void initStore(UserSessionEntity entity) {
        var store = entity.getAuthenticatedClientSessions();
        if (store == null) {
            entity.setAuthenticatedClientSessions(new AuthenticatedClientSessionStore());
        }
    }

    /**
     * Factory SPI to create {@link ClientSessionMappingAdapter} for the {@link AuthenticatedClientSessionStore} used by
     * this instance.
     */
    public interface ClientSessionAdapterFactory {
        ClientSessionMappingAdapter create(AuthenticatedClientSessionStore clientSessionStore);
    }

    private record Factory(boolean offline) implements UpdaterFactory<String, UserSessionEntity, UserSessionUpdater> {

        @Override
        public UserSessionUpdater create(String key, UserSessionEntity entity) {
            return new UserSessionUpdater(key, Objects.requireNonNull(entity), -1, offline, UpdaterState.CREATED);
        }

        @Override
        public UserSessionUpdater wrapFromCache(String key, MetadataValue<UserSessionEntity> entity) {
            assert entity != null;
            return new UserSessionUpdater(key, Objects.requireNonNull(entity.getValue()), entity.getVersion(), offline, UpdaterState.READ);
        }

        @Override
        public UserSessionUpdater deleted(String key) {
            return new UserSessionUpdater(key, null, -1, offline, UpdaterState.DELETED);
        }
    }
}
