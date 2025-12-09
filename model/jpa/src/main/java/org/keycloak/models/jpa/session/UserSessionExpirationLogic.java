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
import org.keycloak.models.KeycloakSessionTaskWithResult;
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
    private static final Consumer<TypedQuery<Object[]>> NO_PARAMETERS = typedQuery -> {
    };

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

        runInBatches(sessionFactory,
                s -> findAndRemoveSessions(s, realmId, batchSize, true, Details.USER_SESSION_EXPIRED_REASON, "findExpiredOfflineUserSessionsLastRefresh", setLastSessionRefresh, expiredSessions),
                expiredSessions::clear);

        runInBatches(sessionFactory,
                s -> findAndRemoveSessions(s, realmId, batchSize, true, Details.USER_SESSION_EXPIRED_REASON, "findExpiredOfflineUserSessionsCreatedOn", setCreatedOn, expiredSessions),
                expiredSessions::clear);


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
        Consumer<TypedQuery<Object[]>> setRememberMe = setRememberMe(rememberMe);
        Consumer<TypedQuery<Object[]>> setCreatedOn = UserSessionExpirationLogic.<Object[]>setCreatedOn(oldestCreatedOn)
                .andThen(setRememberMe);

        int oldestLastSessionRefresh = currentTime - expiration.getMaxIdle(rememberMe) - SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS;
        var setLastSessionRefresh = UserSessionExpirationLogic.<Object[]>setLastSessionRefresh(oldestLastSessionRefresh)
                .andThen(setRememberMe);

        String realmId = realm.getId();
        final List<UserSessionAndUser> expiredSessions = new ArrayList<>(batchSize);

        runInBatches(sessionFactory,
                s -> findAndRemoveSessions(s, realmId, batchSize, false, Details.USER_SESSION_EXPIRED_REASON, "findExpiredRegularUserSessionsLastRefresh", setLastSessionRefresh, expiredSessions),
                expiredSessions::clear);

        runInBatches(sessionFactory,
                s -> findAndRemoveSessions(s, realmId, batchSize, false, Details.USER_SESSION_EXPIRED_REASON, "findExpiredRegularUserSessionsCreatedOn", setCreatedOn, expiredSessions),
                expiredSessions::clear);

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

        final List<UserSessionAndUser> sessionsWithRememberMe = new ArrayList<>(batchSize);
        final List<String> sessionsWithoutRememberMe = new ArrayList<>(batchSize);
        final Runnable cleanup = () -> {
            sessionsWithRememberMe.clear();
            sessionsWithoutRememberMe.clear();
        };
        runInBatches(sessionFactory,
                s -> handleRememberMeColumnValue(s, realmId, realmName, batchSize, rememberMeEnabledInRealm, "findUserSessionAndDataWithNullRememberMeLastRefresh", setLastSessionRefresh, sessionsWithRememberMe, sessionsWithoutRememberMe),
                cleanup);
        runInBatches(sessionFactory,
                s -> handleRememberMeColumnValue(s, realmId, realmName, batchSize, rememberMeEnabledInRealm, "findUserSessionAndDataWithNullRememberMeCreatedOn", setCreatedOn, sessionsWithRememberMe, sessionsWithoutRememberMe),
                cleanup);

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
     * @param batchSize      Sets the maximum number of user sessions to remove in a single transaction.
     */
    public static void deleteInvalidSessions(KeycloakSessionFactory sessionFactory, RealmModel realm, int batchSize) {
        long start = System.nanoTime();
        final String realmId = realm.getId();
        final String realmName = realm.getName();

        logger.tracef("Removing invalid user sessions for realm '%s'", realmName);

        List<UserSessionAndUser> invalidSession = new ArrayList<>();
        runInBatches(sessionFactory,
                s -> findAndRemoveSessions(s, realmId, batchSize, false, Details.INVALID_USER_SESSION_REMEMBER_ME_REASON, "findInvalidRegularUserSessions", NO_PARAMETERS, invalidSession),
                invalidSession::clear);

        long duration = System.nanoTime() - start;
        logger.debugf("Invalid session removed for realm '%s'. Took %dms", realmName, TimeUnit.NANOSECONDS.toMillis(duration));
    }

    private static boolean handleRememberMeColumnValue(KeycloakSession session, String realmId, String realmName, int batchSize, boolean rememberMeEnabled, String queryName, Consumer<TypedQuery<Object[]>> setParameters, List<UserSessionAndUser> sessionsWithRememberMeCollector, List<String> sessionsWithoutRememberMeCollector) {
        final EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

        TypedQuery<Object[]> query = em.createNamedQuery(queryName, Object[].class);
        setParameters.accept(query);
        query.setParameter("realmId", realmId)
                .setHint(AvailableHints.HINT_READ_ONLY, true)
                .setMaxResults(batchSize)
                .getResultStream()
                .map(UserSessionIdAndRememberMe::fromQueryProjection)
                .forEach(userSession -> {
                    if (userSession.rememberMe()) {
                        sessionsWithRememberMeCollector.add(userSession.sessionAndUser());
                    } else {
                        sessionsWithoutRememberMeCollector.add(userSession.sessionAndUser().userSessionId());
                    }
                });

        int updateCount = updateRememberMeColumn(em, false, sessionsWithoutRememberMeCollector);
        if (rememberMeEnabled) {
            int rememberMeUpdateCount = updateRememberMeColumn(em, true, sessionsWithRememberMeCollector.stream().map(UserSessionAndUser::userSessionId).toList());
            logger.debugf("%d sessions with remember me, and %d sessions without remember updated, for realm '%s'", rememberMeUpdateCount, updateCount, realmName);
        } else {
            int deletedCount = handleResultsToRemove(session, em, realmId, false, Details.INVALID_USER_SESSION_REMEMBER_ME_REASON, sessionsWithRememberMeCollector);
            logger.debugf("%d sessions without remember me updated, and %d invalid sessions deleted, for realm '%s'", updateCount, deletedCount, realmName);
        }

        return sessionsWithRememberMeCollector.size() + sessionsWithoutRememberMeCollector.size() >= batchSize;
    }

    private static void createUserSessionDeletedEvent(KeycloakSession session, RealmModel realm, UserSessionAndUser data, String reason) {
        new EventBuilder(realm, session)
                .user(data.userId())
                .session(data.userSessionId())
                .event(EventType.USER_SESSION_DELETED)
                .detail(Details.REASON, reason)
                .success();
    }

    // returns true if it has more rows to check
    private static boolean findAndRemoveSessions(KeycloakSession session, String realmId, int batchSize, boolean offline, String eventReason, String queryName, Consumer<TypedQuery<Object[]>> queryParameters, List<UserSessionAndUser> expiredSessions) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

        TypedQuery<Object[]> query = em.createNamedQuery(queryName, Object[].class);
        queryParameters.accept(query);
        query.setParameter("realmId", realmId)
                .setHint(AvailableHints.HINT_READ_ONLY, true)
                .setMaxResults(batchSize)
                .getResultStream()
                .map(UserSessionAndUser::fromQueryProjection)
                .forEach(expiredSessions::add);

        handleResultsToRemove(session, em, realmId, offline, eventReason, expiredSessions);

        // This should be safe.
        // If the hits are less than the desired batch size, we should not have expired sessions.
        return expiredSessions.size() >= batchSize;
    }

    private static int handleResultsToRemove(KeycloakSession session, EntityManager em, String realmId, boolean offline, String eventReason, Collection<UserSessionAndUser> expiredSessions) {
        if (expiredSessions.isEmpty()) {
            return 0;
        }

        RealmModel realm = session.realms().getRealm(realmId);
        session.getContext().setRealm(realm);

        // creates the expiration events and extracts the user session IDs for the delete statement.
        var sessionIds = expiredSessions.stream()
                .peek(sessionAndUser -> createUserSessionDeletedEvent(session, realm, sessionAndUser, eventReason))
                .map(UserSessionAndUser::userSessionId)
                .toList();

        String offlineStr = offlineToString(offline);

        int cs = em.createNamedQuery("deleteClientSessionsByUserSessions")
                .setParameter("userSessionId", sessionIds)
                .setParameter("offline", offlineStr)
                .executeUpdate();

        int us = em.createNamedQuery("deleteUserSessions")
                .setParameter("offline", offlineStr)
                .setParameter("userSessionIds", sessionIds)
                .executeUpdate();
        logger.debugf("Removed %d user sessions and %d client sessions in realm '%s'", us, cs, realm.getName());
        return us;
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

    private static void runInBatches(KeycloakSessionFactory sessionFactory, KeycloakSessionTaskWithResult<Boolean> task, Runnable afterBatchAction) {
        boolean hasMore;
        do {
            hasMore = runJobInTransactionWithResult(sessionFactory, null, task, "session-expiration-task");
            afterBatchAction.run();
        } while (hasMore);
    }

    private static <T> Consumer<TypedQuery<T>> setLastSessionRefresh(int value) {
        return query -> query.setParameter("lastSessionRefresh", value);
    }

    private static <T> Consumer<TypedQuery<T>> setCreatedOn(int value) {
        return query -> query.setParameter("createdOn", value);
    }

    private static <T> Consumer<TypedQuery<T>> setRememberMe(boolean value) {
        return query -> query.setParameter("rememberMe", value);
    }
}
