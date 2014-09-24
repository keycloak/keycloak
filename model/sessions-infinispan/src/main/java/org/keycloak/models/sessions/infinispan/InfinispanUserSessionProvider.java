package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.infinispan.query.dsl.*;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.util.Time;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanUserSessionProvider implements UserSessionProvider {

    private KeycloakSession session;
    private Cache<String, UserSessionEntity> userSessions;
    private Cache<String, ClientSessionEntity> clientSessions;

    public InfinispanUserSessionProvider(KeycloakSession session, Cache<String, UserSessionEntity> userSessions, Cache<String, ClientSessionEntity> clientSessions) {
        this.session = session;
        this.userSessions = userSessions;
        this.clientSessions = clientSessions;
    }

    @Override
    public ClientSessionModel createClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession, String redirectUri, String state, Set<String> roles) {
        ClientSessionEntity entity = new ClientSessionEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setRealm(realm.getId());
        entity.setTimestamp(Time.currentTime());
        entity.setClient(client.getId());
        entity.setUserSession(userSession.getId());
        entity.setRedirectUri(redirectUri);
        entity.setState(state);
        entity.setRoles(roles);

        add(entity);

        return wrap(realm, entity);
    }

    @Override
    public UserSessionModel createUserSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe) {
        String id = KeycloakModelUtils.generateId();

        UserSessionEntity entity = new UserSessionEntity();
        entity.setId(id);
        entity.setRealm(realm.getId());
        entity.setUser(user.getId());
        entity.setLoginUsername(loginUsername);
        entity.setIpAddress(ipAddress);
        entity.setAuthMethod(authMethod);
        entity.setRememberMe(rememberMe);

        int currentTime = Time.currentTime();

        entity.setStarted(currentTime);
        entity.setLastSessionRefresh(currentTime);

        add(entity);

        return wrap(realm, entity);
    }

    @Override
    public ClientSessionModel getClientSession(RealmModel realm, String id) {
        ClientSessionEntity entity = clientSessions.get(id);
        return wrap(realm, entity);
    }

    @Override
    public UserSessionModel getUserSession(RealmModel realm, String id) {
        UserSessionEntity entity = userSessions.get(id);
        return wrap(realm, entity);
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, UserModel user) {
        List<UserSessionEntity> entities = userSessions().eq("user", user.getId()).orderBy("started").list();
        return wrapUserSessions(realm, entities);
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client) {
        return getUserSessions(realm, client, -1, -1);
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client, int firstResult, int maxResults) {
        Set<String> userSessionIds = new LinkedHashSet<String>();
        List<ClientSessionEntity> clientSessions = clientSessions().eq("client", client.getId()).first(firstResult).max(maxResults).orderBy("timestamp").list();
        for (ClientSessionEntity e : clientSessions) {
            userSessionIds.add(e.getUserSession());
        }

        List<UserSessionModel> userSessions = new LinkedList<UserSessionModel>();
        for (String userSessionId : userSessionIds) {
            userSessions.add(getUserSession(realm, userSessionId));
        }
        return userSessions;
    }

    @Override
    public int getActiveUserSessions(RealmModel realm, ClientModel client) {
        Set<String> userSessionIds = new HashSet<String>();
        List<ClientSessionEntity> clientSessions = clientSessions().eq("client", client.getId()).list();
        for (ClientSessionEntity e : clientSessions) {
            userSessionIds.add(e.getUserSession());
        }
        return userSessionIds.size();
    }

    @Override
    public void removeUserSession(RealmModel realm, UserSessionModel session) {
        removeUserSession(session.getId());
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        List<UserSessionEntity> entities = userSessions().eq("user", user.getId()).list();
        for (UserSessionEntity e : entities) {
            if (realm.getId().equals(e.getRealm())) {
                removeUserSession(e.getId());
            }
        }
    }

    @Override
    public void removeExpiredUserSessions(RealmModel realm) {
        List<UserSessionEntity> entities = userSessions().eq("realm", realm.getId()).and().lt("started", Time.currentTime() - realm.getSsoSessionMaxLifespan()).list();
        for (UserSessionEntity e : entities) {
            removeUserSession(e.getId());
        }
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        List<UserSessionEntity> entities = userSessions().eq("realm", realm.getId()).list();
        for (UserSessionEntity e : entities) {
            removeUserSession(e.getId());
        }
    }

    @Override
    public UsernameLoginFailureModel getUserLoginFailure(RealmModel realm, String username) {
        // TODO
        return null;
    }

    @Override
    public UsernameLoginFailureModel addUserLoginFailure(RealmModel realm, String username) {
        // TODO
        return null;
    }

    @Override
    public List<UsernameLoginFailureModel> getAllUserLoginFailures(RealmModel realm) {
        // TODO
        return null;
    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        removeUserSessions(realm);
    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {
        List<ClientSessionEntity> entities = clientSessions().eq("realm", realm.getId()).and().eq("client", client.getId()).list();
        for (ClientSessionEntity c : entities) {
            clientSessions.remove(c.getId());
        }
    }

    @Override
    public void onUserRemoved(RealmModel realm, UserModel user) {
        removeUserSessions(realm, user);
    }

    @Override
    public void close() {
    }

    void add(UserSessionEntity entity) {
        userSessions.put(entity.getId(), entity);
    }

    void update(UserSessionEntity entity) {
        userSessions.replace(entity.getId(), entity);
    }

    void removeUserSession(String userSessionId) {
        userSessions.remove(userSessionId);

        List<ClientSessionEntity> clientSessions = clientSessions().eq("userSession", userSessionId).list();
        for (ClientSessionEntity c : clientSessions) {
            removeClientSession(c.getId());
        }
    }

    void add(ClientSessionEntity entity) {
        clientSessions.put(entity.getId(), entity);
    }

    void update(ClientSessionEntity entity) {
        clientSessions.replace(entity.getId(), entity);
    }

    void removeClientSession(String clientSessionId) {
        clientSessions.remove(clientSessionId);
    }

    UserSessionModel wrap(RealmModel realm, UserSessionEntity entity) {
        if (entity != null && realm.getId().equals(entity.getRealm())) {
            return new UserSessionAdapter(session, this, realm, entity);
        } else {
            return null;
        }
    }

    List<UserSessionModel> wrapUserSessions(RealmModel realm, List<UserSessionEntity> entities) {
        List<UserSessionModel> models = new LinkedList<UserSessionModel>();
        for (UserSessionEntity e : entities) {
            UserSessionModel m = wrap(realm, e);
            if (m != null) {
                models.add(m);
            }
        }
        return models;
    }

    ClientSessionModel wrap(RealmModel realm, ClientSessionEntity entity) {
        if (entity != null && realm.getId().equals(entity.getRealm())) {
            return new ClientSessionAdapter(session, this, realm, entity);
        } else {
            return null;
        }
    }

    List<ClientSessionModel> wrapClientSessions(RealmModel realm, List<ClientSessionEntity> entities) {
        List<ClientSessionModel> models = new LinkedList<ClientSessionModel>();
        for (ClientSessionEntity e : entities) {
            ClientSessionModel m = wrap(realm, e);
            if (m != null) {
                models.add(m);
            }
        }
        return models;
    }


    class Query<T> {

        private boolean indexed;
        private final QueryBuilder qb;
        private FilterConditionContext ctx;
        private FilterConditionBeginContext chainCtx;
        private long first = -1;
        private int max = -1;
        private String orderBy;

        public Query(SearchManager searchManager, Class<T> clazz) {
            indexed = searchManager.getSearchFactory().getIndexedTypes().contains(clazz);
            qb = searchManager.getQueryFactory().from(clazz);
        }

        Query eq(String field, Object value) {
            if (chainCtx != null) {
                ctx = chainCtx.having(field).eq(value);
                chainCtx = null;
            } else if (ctx == null) {
                ctx = qb.having(field).eq(value);
            } else {
                throw new IllegalStateException();
            }
            return this;
        }

        Query and() {
            if (chainCtx != null) {
                throw new IllegalStateException();
            }
            chainCtx = ctx.and();
            return this;
        }

        Query lt(String field, Object value) {
            if (chainCtx != null) {
                ctx = chainCtx.having(field).lt(value);
                chainCtx = null;
            } else if (ctx == null) {
                ctx = qb.having(field).lt(value);
            } else {
                throw new IllegalStateException();
            }
            return this;
        }

        Query first(long first) {
            this.first = first;
            return this;
        }

        Query max(int max) {
            this.max = max;
            return this;
        }

        Query orderBy(String orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        <T> List<T> list() {
            if (!indexed) {
                return Collections.emptyList();
            }

            QueryBuilder<org.infinispan.query.dsl.Query> qb = ctx.toBuilder();
            if (first != -1) {
                qb.startOffset(first);
            }
            if (max != -1) {
                qb.maxResults(max);
            }
            if (orderBy != null) {
                qb.orderBy(orderBy, SortOrder.ASC);
            }
            return qb.build().list();
        }

    }

    Query userSessions() {
        return new Query<UserSessionEntity>(Search.getSearchManager(userSessions), UserSessionEntity.class);
    }


    Query clientSessions() {
        return new Query<ClientSessionEntity>(Search.getSearchManager(clientSessions), ClientSessionEntity.class);
    }

}
