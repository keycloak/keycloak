package org.keycloak.testsuite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.common.util.Time;

/**
 * HOWTO USE THIS:
 *
 * 1) Run KeycloakServer with system properties (assuming mongo up and running on localhost):
 *      -Dkeycloak.realm.provider=mongo -Dkeycloak.user.provider=mongo -Dkeycloak.userSessionPersister.provider=mongo -Dkeycloak.connectionsMongo.db=keycloak -Dkeycloak.connectionsInfinispan.clustered=true -Dresources -DstartInfinispanCLI
 *
 * 2) Write command on STDIN to persist 50000 userSessions to mongo: persistSessions 50000
 *
 * 3) Run command "clear" to ensure infinispan cache is cleared. Doublecheck with command "size" is 0
 *
 * 4) Write command to load sessions from persistent storage - 100 sessions per worker transaction: loadPersistentSessions 100
 *
 * See the progress in log. Finally run command "size" to ensure size is 100001 (50000 userSessions + 50000 clientSessions + 1 initializationState item)
 *
 * 5) Alternative to step 3+4 - Kill the server after step 2 and start two KeycloakServer in parallel on ports 8081 and 8082 . See the progress in logs of loading persistent sessions to infinispan.
 * Kill the coordinator (usually 8081 node) during startup and see the node 8082 became coordinator and took ownership of loading persistent sessions. After node 8082 fully started, the size of infinispan is again 100001
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanCLI {

    private static final Logger log = Logger.getLogger(InfinispanCLI.class);

    private final KeycloakSessionFactory sessionFactory;

    public InfinispanCLI(KeycloakServer server) {
        this.sessionFactory = server.getSessionFactory();
    }

    // WARNING: Stdin blocking operation
    public void start() throws IOException {
        log.info("Starting infinispan CLI. Exit with 'exit'");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                log.info("Command: " + line);

                if (line.equals("exit")) {
                    return;
                }

                final String finalLine = line;

                KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                    @Override
                    public void run(KeycloakSession session) {
                        InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
                        Cache<String, SessionEntity> ispnCache = provider.getCache(InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME);
                        runTask(finalLine, ispnCache);
                    }

                });
            }
        } finally {
            log.info("Exit infinispan CLI");
            reader.close();
        }
    }

    private void runTask(String line, Cache<String, SessionEntity> cache) {
        try {
            String[] splits = line.split(" ");
            if (splits[0].equals("put")) {
                UserSessionEntity userSession = new UserSessionEntity();
                String id = splits[1];

                userSession.setId(id);
                userSession.setRealm(splits[2]);
                userSession.setLastSessionRefresh(Time.currentTime());
                cache.put(id, userSession);

            } else if (splits[0].equals("get")) {
                String id = splits[1];
                UserSessionEntity userSession = (UserSessionEntity) cache.get(id);
                printSession(id, userSession);
            } else if (splits[0].equals("remove")) {
                String id = splits[1];
                cache.remove(id);
            } else if (splits[0].equals("clear")) {
                cache.clear();
                log.info("Cache cleared");
            } else if (splits[0].equals("size")) {
                log.info("Size: " + cache.size());
            } else if (splits[0].equals("list")) {
                for (String id : cache.keySet()) {
                    SessionEntity entity = cache.get(id);
                    if (!(entity instanceof UserSessionEntity)) {
                        continue;
                    }
                    UserSessionEntity userSession = (UserSessionEntity) cache.get(id);
                    log.info("list: key=" + id + ", value=" + toString(userSession));
                }

            } else if (splits[0].equals("getLocal")) {
                String id = splits[1];
                cache = ((AdvancedCache) cache).withFlags(Flag.CACHE_MODE_LOCAL);
                UserSessionEntity userSession = (UserSessionEntity) cache.get(id);
                printSession(id, userSession);

            } else if (splits[0].equals("persistSessions")) {

                final int count = Integer.parseInt(splits[1]);
                final List<String> userSessionIds = new LinkedList<>();
                final List<String> clientSessionIds = new LinkedList<>();

                // Create sessions in separate transaction first
                KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                    @Override
                    public void run(KeycloakSession session) {
                        RealmModel realm = session.realms().getRealmByName("master");
                        UserModel john = session.users().getUserByUsername("admin", realm);
                        ClientModel testApp = realm.getClientByClientId("security-admin-console");
                        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);

                        for (int i=0 ; i<count ; i++) {
                            UserSessionModel userSession = session.sessions().createUserSession(realm, john, "john-doh@localhost", "127.0.0.2", "form", true, null, null);
                            ClientSessionModel clientSession = session.sessions().createClientSession(realm, testApp);
                            clientSession.setUserSession(userSession);
                            clientSession.setRedirectUri("http://redirect");
                            clientSession.setNote("foo", "bar-" + i);
                            userSessionIds.add(userSession.getId());
                            clientSessionIds.add(clientSession.getId());
                        }
                    }

                });

                log.info("Sessions created in infinispan storage");

                // Persist them now
                KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                    @Override
                    public void run(KeycloakSession session) {
                        RealmModel realm = session.realms().getRealmByName("master");
                        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);

                        for (String userSessionId : userSessionIds) {
                            UserSessionModel userSession = session.sessions().getUserSession(realm, userSessionId);
                            persister.createUserSession(userSession, true);
                        }

                        log.info("userSessions persisted");

                        for (String clientSessionId : clientSessionIds) {
                            ClientSessionModel clientSession = session.sessions().getClientSession(realm, clientSessionId);
                            persister.createClientSession(clientSession, true);
                        }

                        log.info("clientSessions persisted");
                    }

                });

                // Persist them now
                KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                    @Override
                    public void run(KeycloakSession session) {
                        RealmModel realm = session.realms().getRealmByName("master");
                        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);

                        log.info(count + " sessions persisted. Total number of sessions: " + persister.getUserSessionsCount(true));
                    }

                });

            } else if (splits[0].equals("loadPersistentSessions")) {

                int sessionsPerSegment = Integer.parseInt(splits[1]);
                UserSessionProviderFactory sessionProviderFactory = (UserSessionProviderFactory) sessionFactory.getProviderFactory(UserSessionProvider.class);
                sessionProviderFactory.loadPersistentSessions(sessionFactory, 10, sessionsPerSegment);

                log.info("All persistent sessions loaded successfully");
            }
        } catch (RuntimeException e) {
            log.error("Error occured during command. ", e);
        }
    }

    private void printSession(String id, UserSessionEntity userSession) {
        if (userSession == null) {
            log.info("Not found session with Id: " + id);
        } else {
            log.info("Found session. ID: " + toString(userSession));
        }
    }

    private String toString(UserSessionEntity userSession) {
        return "ID: " + userSession.getId() + ", realm: " + userSession.getRealm() + ", lastAccessTime: " + Time.toDate(userSession.getLastSessionRefresh()) +
                ", clientSessions: " + userSession.getClientSessions().size();
    }
}
