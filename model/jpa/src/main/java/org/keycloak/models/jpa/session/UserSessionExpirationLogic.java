/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.jpa.session;


import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.RealmExpiration;
import org.keycloak.models.utils.SessionTimeoutHelper;

import org.hibernate.jpa.AvailableHints;
import org.jboss.logging.Logger;

import static org.keycloak.models.jpa.session.JpaSessionUtil.offlineToString;
import static org.keycloak.models.utils.KeycloakModelUtils.runJobInTransactionWithResult;


/**
 * Moved the user session expiration logic from {@link JpaUserSessionPersisterProvider} to here.
 */
final class UserSessionExpirationLogic {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private UserSessionExpirationLogic() {
    }

    /**
     * It expires the offline user sessions, using the {@code currentTime} and the realm's {@link RealmExpiration}.
     *
     * @param sessionFactory The {@link KeycloakSessionFactory}, used to start transactions.
     * @param realm          The {@link RealmModel} from the user session should be checked for expiration.
     * @param currentTime    The current timestamp, in seconds.
     * @param expiration     The realm's {@link RealmExpiration}. It contains the user session lifespan and max-idle
     *                       settings.
     * @param batchSize      Sets the maximum number of user sessions to delete in a single transaction.
     */
    public static void expireOfflineSessions(KeycloakSessionFactory sessionFactory, RealmModel realm, int currentTime, RealmExpiration expiration, int batchSize) {
        long start = System.nanoTime();
        logger.tracef("Removing expired offline user sessions for realm '%s'", realm.getName());

        final int oldestCreatedOn = realm.isOfflineSessionMaxLifespanEnabled() ?
                currentTime - expiration.offlineLifespan() - SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS :
                0;
        Consumer<TypedQuery<Object[]>> setCreatedOn = setCreatedOn(oldestCreatedOn);

        final int oldestLastSessionRefresh = currentTime - expiration.offlineMaxIdle() - SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS;
        Consumer<TypedQuery<Object[]>> setLastSessionRefresh = setLastSessionRefresh(oldestLastSessionRefresh);

        String realmId = realm.getId();
        final List<UserSessionAndUser> expiredSessions = new ArrayList<>(batchSize);

        boolean hasMore = true;
        while (hasMore) {
            hasMore = runJobInTransactionWithResult(sessionFactory,
                    s -> removeExpiredOfflineSessionsInTransaction(s, realmId, batchSize, "findExpiredOfflineUserSessionsLastRefresh", setLastSessionRefresh, expiredSessions));
            expiredSessions.clear();
        }

        hasMore = true;
        while (hasMore) {
            hasMore = runJobInTransactionWithResult(sessionFactory,
                    s -> removeExpiredOfflineSessionsInTransaction(s, realmId, batchSize, "findExpiredOfflineUserSessionsCreatedOn", setCreatedOn, expiredSessions));
            expiredSessions.clear();
        }

        long duration = System.nanoTime() - start;
        logger.debugf("Offline user session expiration task completed for realm '%s'. Took %dms", realm.getName(), TimeUnit.NANOSECONDS.toMillis(duration));
    }

    /**
     * It expires the regular user sessions, using the {@code currentTime} and the realm's {@link RealmExpiration}.
     *
     * @param sessionFactory The {@link KeycloakSessionFactory}, used to start transactions.
     * @param realm          The {@link RealmModel} from the user session should be checked for expiration.
     * @param currentTime    The current timestamp, in seconds.
     * @param expiration     The realm's {@link RealmExpiration}. It contains the user session lifespan and max-idle
     *                       settings.
     * @param rememberMe     If {@code true}, it only checks that have remember me enabled.
     * @param batchSize      Sets the maximum number of user sessions to delete in a single transaction.
     */
    public static void expireRegularSessions(KeycloakSessionFactory sessionFactory, RealmModel realm, int currentTime, RealmExpiration expiration, boolean rememberMe, int batchSize) {
        long start = System.nanoTime();
        logger.tracef("Removing expired regular user sessions for realm '%s'", realm.getName());

        int oldestCreatedOn = currentTime - expiration.getLifespan(rememberMe) - SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS;
        Consumer<TypedQuery<Object[]>> setCreatedOn = setCreatedOn(oldestCreatedOn);

        int oldestLastSessionRefresh = currentTime - expiration.getMaxIdle(rememberMe) - SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS;
        Consumer<TypedQuery<Object[]>> setLastSessionRefresh = setLastSessionRefresh(oldestLastSessionRefresh);

        String realmId = realm.getId();
        final List<UserSessionAndUser> expiredSessions = new ArrayList<>(batchSize);

        boolean hasMore = true;
        while (hasMore) {
            hasMore = runJobInTransactionWithResult(sessionFactory,
                    s -> removeExpiredRegularSessionInTransaction(s, realmId, rememberMe, batchSize, "findExpiredRegularUserSessionsLastRefresh", setLastSessionRefresh, expiredSessions));
            expiredSessions.clear();
        }

        hasMore = true;
        while (hasMore) {
            hasMore = runJobInTransactionWithResult(sessionFactory,
                    s -> removeExpiredRegularSessionInTransaction(s, realmId, rememberMe, batchSize, "findExpiredRegularUserSessionsCreatedOn", setCreatedOn, expiredSessions));
            expiredSessions.clear();
        }

        long duration = System.nanoTime() - start;
        logger.debugf("Regular user session expiration task completed for realm '%s'. Took %dms", realm.getName(), TimeUnit.NANOSECONDS.toMillis(duration));
    }

    /**
     * Migrates the remember me flag into to its own column, for an efficient query.
     * <p>
     * It only affects regular user sessions since offline sessions do not have remember me, and only migrates sessions
     * close to the expiration time to avoid concurrency issues on existing sessions.
     *
     * @param sessionFactory The {@link KeycloakSessionFactory}, used to start transactions.
     * @param realm          The {@link RealmModel} with the user session to be migrated.
     * @param currentTime    The current timestamp, in seconds.
     * @param expiration     The realm's {@link RealmExpiration}. It contains the user session lifespan and max-idle
     *                       settings.
     * @param batchSize      Sets the maximum number of user sessions to update in a single transaction.
     */
    public static void migrateRememberMe(KeycloakSessionFactory sessionFactory, RealmModel realm, RealmExpiration expiration, int currentTime, int batchSize) {
        long start = System.nanoTime();
        final boolean rememberMeEnabledInRealm = realm.isRememberMe();
        final String realmId = realm.getId();
        final String realmName = realm.getName();
        logger.tracef("Migrating remember me value for regular user sessions, for realm '%s'", realmName);

        // migrating session, they don't need to be accurate.
        final int expireMaxIdle = currentTime - Math.min(expiration.maxIdle(), expiration.rememberMeMaxIdle());
        Consumer<TypedQuery<Object[]>> setLastSessionRefresh = setLastSessionRefresh(expireMaxIdle);
        final int expireLifespan = currentTime - Math.min(expiration.lifespan(), expiration.rememberMeLifespan());
        Consumer<TypedQuery<Object[]>> setCreatedOn = setCreatedOn(expireLifespan);

        final List<String> sessionsWithRememberMe = new ArrayList<>(batchSize);
        final List<String> sessionsWithoutRememberMe = new ArrayList<>(batchSize);
        boolean hasMore = true;
        while (hasMore) {
            hasMore = runJobInTransactionWithResult(sessionFactory,
                    s -> handleRememberMeColumnValue(s, realmId, realmName, batchSize, rememberMeEnabledInRealm, "findUserSessionAndDataWithNullRememberMeLastRefresh", setLastSessionRefresh, sessionsWithRememberMe, sessionsWithoutRememberMe));
            sessionsWithRememberMe.clear();
            sessionsWithoutRememberMe.clear();
        }

        hasMore = true;
        while (hasMore) {
            hasMore = runJobInTransactionWithResult(sessionFactory,
                    s -> handleRememberMeColumnValue(s, realmId, realmName, batchSize, rememberMeEnabledInRealm, "findUserSessionAndDataWithNullRememberMeCreatedOn", setCreatedOn, sessionsWithRememberMe, sessionsWithoutRememberMe));
            sessionsWithRememberMe.clear();
            sessionsWithoutRememberMe.clear();
        }

        long duration = System.nanoTime() - start;
        logger.debugf("Migration task completed for realm '%s'. Took %dms", realmName, TimeUnit.NANOSECONDS.toMillis(duration));
    }

    /**
     * Removes invalid regular user sessions from the database.
     * <p>
     * An invalid user session is a regular session with remember me column set to true, but with the remember me
     * disabled in the realm settings.
     *
     * @param sessionFactory The {@link KeycloakSessionFactory}, used to start transactions.
     * @param realm          The {@link RealmModel} to check and remove invalid user sessions.
     */
    public static void deleteInvalidSessions(KeycloakSessionFactory sessionFactory, RealmModel realm) {
        long start = System.nanoTime();
        final String realmId = realm.getId();
        final String realmName = realm.getName();

        logger.tracef("Removing invalid user sessions for realm '%s'", realmName);

        int count = runJobInTransactionWithResult(sessionFactory, session -> {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            return em.createNamedQuery("deleteInvalidSessions")
                    .setParameter("realmId", realmId)
                    .executeUpdate();
        });
        long duration = System.nanoTime() - start;
        logger.debugf("%d invalid session removed for realm '%s'. Took %dms", (Object) count, realmName, TimeUnit.NANOSECONDS.toMillis(duration));
    }

    private static boolean handleRememberMeColumnValue(KeycloakSession session, String realmId, String realmName, int batchSize, boolean rememberMeEnabled, String queryName, Consumer<TypedQuery<Object[]>> setParameters, List<String> sessionsWithRememberMeCollector, List<String> sessionsWithoutRememberMeCollector) {
        final EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

        TypedQuery<Object[]> query = em.createNamedQuery(queryName, Object[].class);
        setParameters.accept(query);
        query.setParameter("realmId", realmId)
                .setHint(AvailableHints.HINT_READ_ONLY, true)
                .setMaxResults(batchSize)
                .getResultStream()
                .map(UserSessionIdAndRememberMe::fromQueryProjection)
                .forEach(userSession -> (userSession.rememberMe() ? sessionsWithRememberMeCollector : sessionsWithoutRememberMeCollector).add(userSession.id()));

        int updateCount = updateRememberMeColumn(em, false, sessionsWithoutRememberMeCollector);
        if (rememberMeEnabled) {
            int rememberMeUpdateCount = updateRememberMeColumn(em, true, sessionsWithRememberMeCollector);
            logger.debugf("%d sessions with remember me, and %d sessions without remember updated, for realm '%s'", rememberMeUpdateCount, updateCount, realmName);
        } else {
            int deletedCount = deleteUserSessions(em, offlineToString(false), sessionsWithRememberMeCollector);
            logger.debugf("%d sessions without remember me updated, and %d invalid sessions deleted, for realm '%s'", updateCount, deletedCount, realmName);
        }

        return sessionsWithRememberMeCollector.size() + sessionsWithoutRememberMeCollector.size() >= batchSize;
    }

    private static void createExpirationEvent(KeycloakSession session, RealmModel realm, UserSessionAndUser userSessionAndUser) {
        new EventBuilder(realm, session)
                .user(userSessionAndUser.userId())
                .session(userSessionAndUser.userSessionId())
                .event(EventType.USER_SESSION_DELETED)
                .detail(Details.REASON, Details.EXPIRED_DETAIL)
                .success();
    }

    // returns true if it has more rows to check
    private static boolean removeExpiredOfflineSessionsInTransaction(KeycloakSession session, String realmId, int batchSize, String queryName, Consumer<TypedQuery<Object[]>> setParameters, List<UserSessionAndUser> expiredSessions) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

        TypedQuery<Object[]> query = em.createNamedQuery(queryName, Object[].class);
        setParameters.accept(query);
        query.setParameter("realmId", realmId)
                .setHint(AvailableHints.HINT_READ_ONLY, true)
                .setMaxResults(batchSize)
                .getResultStream()
                .map(UserSessionAndUser::fromQueryProjection)
                .forEach(expiredSessions::add);

        handleExpirationQueryResults(session, em, realmId, expiredSessions, true);

        // This should be safe.
        // If the hits are less than the desired batch size, we should not have expired sessions.
        return expiredSessions.size() >= batchSize;
    }

    // returns true if it has more rows to check
    private static boolean removeExpiredRegularSessionInTransaction(KeycloakSession session, String realmId, boolean rememberMe, int batchSize, String queryName, Consumer<TypedQuery<Object[]>> setParameters, List<UserSessionAndUser> expiredSessions) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

        TypedQuery<Object[]> query = em.createNamedQuery(queryName, Object[].class);
        setParameters.accept(query);
        query.setParameter("realmId", realmId)
                .setParameter("rememberMe", rememberMe)
                .setHint(AvailableHints.HINT_READ_ONLY, true)
                .setMaxResults(batchSize)
                .getResultStream()
                .map(UserSessionAndUser::fromQueryProjection)
                .forEach(expiredSessions::add);

        handleExpirationQueryResults(session, em, realmId, expiredSessions, false);

        // This should be safe.
        // If the hits are less than the desired batch size, we should not have expired sessions.
        return expiredSessions.size() >= batchSize;
    }

    private static void handleExpirationQueryResults(KeycloakSession session, EntityManager em, String realmId, Collection<UserSessionAndUser> expiredSessions, boolean offline) {
        if (expiredSessions.isEmpty()) {
            return;
        }

        RealmModel realm = session.realms().getRealm(realmId);
        session.getContext().setRealm(realm);

        // creates the expiration events and extracts the user session IDs for the delete statement.
        var sessionIds = expiredSessions.stream()
                .peek(sessionAndUser -> createExpirationEvent(session, realm, sessionAndUser))
                .map(UserSessionAndUser::userSessionId)
                .toList();

        String offlineStr = offlineToString(offline);

        int cs = em.createNamedQuery("deleteClientSessionsByUserSessions")
                .setParameter("userSessionId", sessionIds)
                .setParameter("offline", offlineStr)
                .executeUpdate();

        int us = deleteUserSessions(em, offlineStr, sessionIds);
        logger.debugf("Removed %d expired user sessions and %d expired client sessions in realm '%s'", us, cs, realm.getName());
    }

    private static int updateRememberMeColumn(EntityManager em, boolean rememberMe, Collection<String> sessionIds) {
        if (sessionIds.isEmpty()) {
            return 0;
        }
        return em.createNamedQuery("updateUserSessionRememberMeColumn")
                .setParameter("rememberMe", rememberMe)
                .setParameter("userSessionIds", sessionIds)
                .executeUpdate();
    }

    private static int deleteUserSessions(EntityManager em, String offlineStr, Collection<String> sessionIds) {
        if (sessionIds.isEmpty()) {
            return 0;
        }
        return em.createNamedQuery("deleteUserSessions")
                .setParameter("offline", offlineStr)
                .setParameter("userSessionIds", sessionIds)
                .executeUpdate();
    }

    private static <T> Consumer<TypedQuery<T>> setLastSessionRefresh(int value) {
        return query -> query.setParameter("lastSessionRefresh", value);
    }

    private static <T> Consumer<TypedQuery<T>> setCreatedOn(int value) {
        return query -> query.setParameter("createdOn", value);
    }
}
