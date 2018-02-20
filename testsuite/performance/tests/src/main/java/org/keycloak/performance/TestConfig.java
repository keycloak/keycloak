package org.keycloak.performance;

import org.keycloak.performance.util.FilteredIterator;
import org.keycloak.performance.util.LoopingIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

import static org.keycloak.performance.RealmsConfigurationBuilder.computeAppUrl;
import static org.keycloak.performance.RealmsConfigurationBuilder.computeClientId;
import static org.keycloak.performance.RealmsConfigurationBuilder.computePassword;
import static org.keycloak.performance.RealmsConfigurationBuilder.computeSecret;
import static org.keycloak.performance.RealmsConfigurationBuilder.computeUsername;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class TestConfig {

    //
    // Settings used by RealmsConfigurationBuilder only - when generating the dataset
    //
    public static final int hashIterations = Integer.getInteger("hashIterations", 27500);

    //
    // Settings used by RealmsConfigurationLoader only - when loading data into Keycloak
    //
    public static final int numOfWorkers = Integer.getInteger("numOfWorkers", 1);

    //
    // Settings used by RealmConfigurationLoader to connect to Admin REST API
    //
    public static final String authRealm = System.getProperty("authRealm", "master");
    public static final String authUser = System.getProperty("authUser", "admin");
    public static final String authPassword = System.getProperty("authPassword", "admin");
    public static final String authClient = System.getProperty("authClient", "admin-cli");

    //
    // Settings used by RealmsConfigurationBuilder to generate the dataset and by tests to work within constraints of the dataset
    //
    public static final int numOfRealms = Integer.getInteger("numOfRealms", 1);
    public static final int usersPerRealm = Integer.getInteger("usersPerRealm", 2);
    public static final int clientsPerRealm = Integer.getInteger("clientsPerRealm", 2);
    public static final int realmRoles = Integer.getInteger("realmRoles", 2);
    public static final int realmRolesPerUser = Integer.getInteger("realmRolesPerUser", 2);
    public static final int clientRolesPerUser = Integer.getInteger("clientRolesPerUser", 2);
    public static final int clientRolesPerClient = Integer.getInteger("clientRolesPerClient", 2);

    //
    // Settings used by tests to control common test parameters
    //
    public static final int runUsers = Integer.getInteger("runUsers", 1);
    public static final int rampUpPeriod = Integer.getInteger("rampUpPeriod", 0);
    public static final int steadyLoadPeriod = Integer.getInteger("steadyLoadPeriod", 30);
    public static final boolean rampDownASAP = Boolean.getBoolean("rampDownASAP"); // check for rampdown condition after each scenario step
    public static final int pace = Integer.getInteger("pace", 0); // additional dynamic "pause buffer" between scenario loops
    public static final int userThinkTime = Integer.getInteger("userThinkTime", 5);
    public static final int refreshTokenPeriod = Integer.getInteger("refreshTokenPeriod", 10);

    // Computed timestamps
    public static final long simulationStartTime = System.currentTimeMillis();//new Date().getTime();
    public static final long rampDownPeriodStartTime = simulationStartTime + (rampUpPeriod + steadyLoadPeriod) * 1000;

    //
    // Settings used by BasicOIDCSimulation to control behavior specific to BasicOIDCSimulation
    //
    public static final int badLoginAttempts = Integer.getInteger("badLoginAttempts", 0);
    public static final int refreshTokenCount = Integer.getInteger("refreshTokenCount", 0);

    public static final String serverUris;
    public static final List<String> serverUrisList;

    // Round-robin infinite iterator that directs each next session to the next server
    public static final Iterator<String> serverUrisIterator;

    static {
        // if KEYCLOAK_SERVER_URIS env var is set, and system property serverUris is not set
        String serversProp = System.getProperty("keycloak.server.uris");
        if (serversProp == null) {
            String serversEnv = System.getenv("KEYCLOAK_SERVERS");
            serverUris = serversEnv != null ? serversEnv : "http://localhost:8080/auth";
        } else {
            serverUris = serversProp;
        }

        // initialize serverUrisList and serverUrisIterator
        serverUrisList = Arrays.asList(serverUris.split(" "));
        serverUrisIterator = new LoopingIterator<>(serverUrisList);
    }

    // Users iterators by realm
    private static final ConcurrentMap<String, Iterator<UserInfo>> usersIteratorMap = new ConcurrentHashMap<>();

    // Clients iterators by realm
    private static final ConcurrentMap<String, Iterator<ClientInfo>> clientsIteratorMap = new ConcurrentHashMap<>();

    public static Iterator<UserInfo> getUsersIterator(String realm) {
        return usersIteratorMap.computeIfAbsent(realm, (k) -> randomUsersIterator(realm));
    }

    public static Iterator<ClientInfo> getClientsIterator(String realm) {
        return clientsIteratorMap.computeIfAbsent(realm, (k) -> randomClientsIterator(realm));
    }

    public static Iterator<ClientInfo> getConfidentialClientsIterator(String realm) {
        Iterator<ClientInfo> clientsIt = getClientsIterator(realm);
        return new FilteredIterator<>(clientsIt, (v) -> RealmsConfigurationBuilder.isClientConfidential(v.index));
    }

    public static String toStringCommonTestParameters() {
        return String.format(
        "  runUsers: %s\n" + 
        "  rampUpPeriod: %s\n"+ 
        "  steadyLoadPeriod: %s\n"+
        "  rampDownASAP: %s\n"+ 
        "  pace: %s\n"+ 
        "  userThinkTime: %s\n"+ 
        "  refreshTokenPeriod: %s",
                runUsers, rampUpPeriod, steadyLoadPeriod, rampDownASAP, pace, userThinkTime, refreshTokenPeriod
        );
    }

    public static String toStringDatasetProperties() {
        return String.format("  numOfRealms: %s\n  usersPerRealm: %s\n  clientsPerRealm: %s\n  realmRoles: %s\n  realmRolesPerUser: %s\n  clientRolesPerUser: %s\n  clientRolesPerClient: %s\n  hashIterations: %s",
                numOfRealms, usersPerRealm, clientsPerRealm, realmRoles, realmRolesPerUser, clientRolesPerUser, clientRolesPerClient, hashIterations);
    }
    
    public static Iterator<UserInfo> sequentialUsersIterator(final String realm) {

        return new Iterator<UserInfo>() {

            int idx = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public synchronized UserInfo next() {
                if (idx >= usersPerRealm) {
                    idx = 0;
                }

                String user = computeUsername(realm, idx);
                idx += 1;
                return new UserInfo(user, computePassword(user));
            }
        };
    }

    public static Iterator<UserInfo> randomUsersIterator(final String realm) {

        return new Iterator<UserInfo>() {

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public UserInfo next() {
                String user = computeUsername(realm, ThreadLocalRandom.current().nextInt(usersPerRealm));
                return new UserInfo(user, computePassword(user));
            }
        };
    }

    public static Iterator<ClientInfo> randomClientsIterator(final String realm) {

        return new Iterator<ClientInfo>() {

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public ClientInfo next() {
                int idx = ThreadLocalRandom.current().nextInt(clientsPerRealm);
                String clientId = computeClientId(realm, idx);
                String appUrl = computeAppUrl(clientId);
                return new ClientInfo(idx, clientId, computeSecret(clientId), appUrl);
            }
        };
    }

    public static Iterator<String> randomRealmsIterator() {

        return new Iterator<String>() {

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public String next() {
                return "realm_" + ThreadLocalRandom.current().nextInt(numOfRealms);
            }
        };
    }

    static void validateConfiguration() {
        if (realmRolesPerUser > realmRoles) {
            throw new RuntimeException("Can't have more realmRolesPerUser than there are realmRoles");
        }
        if (clientRolesPerUser > clientsPerRealm * clientRolesPerClient) {
            throw new RuntimeException("Can't have more clientRolesPerUser than there are all client roles (clientsPerRealm * clientRolesPerClient)");
        }
    }
    
}
