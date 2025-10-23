package org.keycloak.models.sessions.infinispan.changes.remote.updater.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.BaseUpdater;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.Expiration;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.Updater;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.UpdaterFactory;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.helper.MapUpdater;
import org.keycloak.models.sessions.infinispan.entities.RemoteUserSessionEntity;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;

/**
 * The {@link Updater} implementation to keep track of modifications for {@link UserSessionModel}.
 */
public class UserSessionUpdater extends BaseUpdater<String, RemoteUserSessionEntity> implements UserSessionModel {

    private static final Factory ONLINE = new Factory(false);
    private static final Factory OFFLINE = new Factory(true);

    private final MapUpdater<String, String> notesUpdater;
    private final List<Consumer<RemoteUserSessionEntity>> changes;
    private final boolean offline;
    private RealmModel realm;
    private UserModel user;
    private AuthenticatedClientSessionMapping clientSessions;
    private SessionPersistenceState persistenceState = SessionPersistenceState.PERSISTENT;

    private UserSessionUpdater(String cacheKey, RemoteUserSessionEntity cacheValue, long version, boolean offline, UpdaterState initialState) {
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
     * @return The {@link UpdaterFactory} implementation to create online sessions instances of
     * {@link UserSessionModel}.
     */
    public static UpdaterFactory<String, RemoteUserSessionEntity, UserSessionUpdater> onlineFactory() {
        return ONLINE;
    }

    /**
     * @return The {@link UpdaterFactory} implementation to create offline sessions instances of
     * {@link UserSessionModel}.
     */
    public static UpdaterFactory<String, RemoteUserSessionEntity, UserSessionUpdater> offlineFactory() {
        return OFFLINE;
    }

    @Override
    public RemoteUserSessionEntity apply(String ignored, RemoteUserSessionEntity userSessionEntity) {
        initNotes(userSessionEntity);
        changes.forEach(change -> change.accept(userSessionEntity));
        notesUpdater.applyChanges(userSessionEntity.getNotes());
        return userSessionEntity;
    }

    @Override
    public Expiration computeExpiration() {
        long maxIdle = SessionTimeouts.getUserSessionMaxIdleMs(realm, isOffline(), getValue().isRememberMe(), getValue().getLastSessionRefresh());
        long lifespan = SessionTimeouts.getUserSessionLifespanMs(realm, isOffline(), getValue().isRememberMe(), getValue().getStarted());
        return new Expiration(maxIdle, lifespan);
    }

    @Override
    public String getId() {
        return getKey();
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
        return clientSessions;
    }

    @Override
    public void removeAuthenticatedClientSessions(Collection<String> removedClientUUIDS) {
        if (removedClientUUIDS == null || removedClientUUIDS.isEmpty()) {
            return;
        }
        removedClientUUIDS.forEach(clientSessions::remove);
    }

    @Override
    public AuthenticatedClientSessionModel getAuthenticatedClientSessionByClient(String clientUUID) {
        return clientSessions.get(clientUUID);
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
        clientSessions.onUserSessionRestart();
        resetState();
        addAndApplyChange(userSessionEntity -> userSessionEntity.restart(realm.getId(), user.getId(), loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId));
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
        return changes.isEmpty() && notesUpdater.isUnchanged();
    }

    /**
     * Initializes this class with references to other models classes.
     *
     * @param persistenceState The {@link SessionPersistenceState}.
     * @param realm            The {@link RealmModel} to where this user session belongs.
     * @param user             The {@link UserModel} associated to this user session.
     * @param clientSessions   The {@link Map} associated to this use session.
     */
    public synchronized void initialize(SessionPersistenceState persistenceState, RealmModel realm, UserModel user, AuthenticatedClientSessionMapping clientSessions) {
        this.realm = Objects.requireNonNull(realm);
        this.user = Objects.requireNonNull(user);
        this.persistenceState = Objects.requireNonNull(persistenceState);
        this.clientSessions = Objects.requireNonNull(clientSessions);
    }

    /**
     * @return {@code true} if it is already initialized.
     */
    public synchronized boolean isInitialized() {
        return realm != null;
    }

    private void addAndApplyChange(Consumer<RemoteUserSessionEntity> change) {
        change.accept(getValue());
        changes.add(change);
    }

    private static void initNotes(RemoteUserSessionEntity entity) {
        var notes = entity.getNotes();
        if (notes == null) {
            entity.setNotes(new HashMap<>());
        }
    }

    private record Factory(
            boolean offline) implements UpdaterFactory<String, RemoteUserSessionEntity, UserSessionUpdater> {

        @Override
        public UserSessionUpdater create(String key, RemoteUserSessionEntity entity) {
            return new UserSessionUpdater(key, Objects.requireNonNull(entity), NO_VERSION, offline, UpdaterState.CREATED);
        }

        @Override
        public UserSessionUpdater wrapFromCache(String key, RemoteUserSessionEntity value, long version) {
            return new UserSessionUpdater(key, value, version, offline, UpdaterState.READ);
        }

        @Override
        public UserSessionUpdater deleted(String key) {
            return new UserSessionUpdater(key, null, NO_VERSION, offline, UpdaterState.DELETED);
        }
    }
}
