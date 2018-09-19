package org.keycloak.performance;

import java.text.SimpleDateFormat;
import org.keycloak.performance.iteration.FilteredIterator;
import org.keycloak.performance.iteration.LoopingIterator;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.configuration.CombinedConfiguration;
import org.jboss.logging.Logger;

import static org.keycloak.performance.RealmsConfigurationBuilder.computeAppUrl;
import static org.keycloak.performance.RealmsConfigurationBuilder.computeClientId;
import static org.keycloak.performance.RealmsConfigurationBuilder.computeEmail;
import static org.keycloak.performance.RealmsConfigurationBuilder.computeFirstName;
import static org.keycloak.performance.RealmsConfigurationBuilder.computeLastName;
import static org.keycloak.performance.RealmsConfigurationBuilder.computePassword;
import static org.keycloak.performance.RealmsConfigurationBuilder.computeSecret;
import static org.keycloak.performance.RealmsConfigurationBuilder.computeUsername;
import org.keycloak.performance.util.CombinedConfigurationNoInterpolation;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 * @author <a href="mailto:tkyjovsk@redhat.com">Tomas Kyjovsky</a>
 */
public class TestConfig {

    private static final Logger LOGGER = Logger.getLogger(TestConfig.class);

    public static final CombinedConfiguration CONFIG;

    static {
        CONFIG = new CombinedConfigurationNoInterpolation();
    }

    //
    // Settings used by RealmsConfigurationBuilder only - when generating the DATASET
    //
    public static final int hashIterations = Integer.getInteger("hashIterations", 27500);

    //
    // Settings used by RealmsConfigurationLoader only - when loading data into Keycloak
    //
    public static final int numOfWorkers = Integer.getInteger("numOfWorkers", 1);
    public static final int startAtRealmIdx = Integer.getInteger("startAtRealmIdx", 0);
    public static final int startAtUserIdx = 0; // doesn't work properly, will be removed later //Integer.getInteger("startAtUserIdx", 0);
    public static final boolean ignoreConflicts = "true".equals(System.getProperty("ignoreConflicts", "false"));
    public static final boolean skipRealmRoles = "true".equals(System.getProperty("skipRealmRoles", "false"));
    public static final boolean skipClientRoles = "true".equals(System.getProperty("skipClientRoles", "false"));

    //
    // Settings used by RealmConfigurationLoader to connect to Admin REST API
    //
    public static final String authRealm = System.getProperty("authRealm", "master");
    public static final String authUser = System.getProperty("authUser", "admin");
    public static final String authPassword = System.getProperty("authPassword", "admin");
    public static final String authClient = System.getProperty("authClient", "admin-cli");

    //
    // Settings used by RealmsConfigurationBuilder to generate the DATASET and by tests to work within constraints of the DATASET
    //
    public static final int numOfRealms = Integer.getInteger("numOfRealms", 1);
    public static final int usersPerRealm = Integer.getInteger("usersPerRealm", 2);
    public static final int clientsPerRealm = Integer.getInteger("clientsPerRealm", 2);
    public static final int realmRoles = Integer.getInteger("realmRoles", 2);
    public static final int realmRolesPerUser = Integer.getInteger("realmRolesPerUser", 2);
    public static final int clientRolesPerUser = Integer.getInteger("clientRolesPerUser", 2);
    public static final int clientRolesPerClient = Integer.getInteger("clientRolesPerClient", 2);

    // sequential vs random DATASET iteration
    public static final int sequentialRealmsFrom = Integer.getInteger("sequentialRealmsFrom", -1); // -1 means random iteration
    public static final int sequentialUsersFrom = Integer.getInteger("sequentialUsersFrom", -1); // -1 means random iteration
    public static final boolean sequentialRealms = sequentialRealmsFrom >= 0;
    public static final boolean sequentialUsers = sequentialUsersFrom >= 0;

    //
    // Settings used by tests to control common test parameters
    //
    public static final double usersPerSec = Double.valueOf(System.getProperty("usersPerSec", "1"));
    public static final int rampUpPeriod = Integer.getInteger("rampUpPeriod", 0);
    public static final int warmUpPeriod = Integer.getInteger("warmUpPeriod", 0);
    public static final int measurementPeriod = Integer.getInteger("measurementPeriod", 30);
    public static final boolean filterResults = Boolean.getBoolean("filterResults"); // filter out results outside of measurementPeriod
    public static final int userThinkTime = Integer.getInteger("userThinkTime", 0);
    public static final int refreshTokenPeriod = Integer.getInteger("refreshTokenPeriod", 0);
    public static final double logoutPct = Double.valueOf(System.getProperty("logoutPct", "100"));

    // Computed timestamps
    public static final long simulationStartTime = System.currentTimeMillis();
    public static final long warmUpStartTime = simulationStartTime + rampUpPeriod * 1000;
    public static final long measurementStartTime = warmUpStartTime + warmUpPeriod * 1000;
    public static final long measurementEndTime = measurementStartTime + measurementPeriod * 1000;

    //
    // Settings used by OIDCLoginAndLogoutSimulation to control behavior specific to OIDCLoginAndLogoutSimulation
    //
    public static final int badLoginAttempts = Integer.getInteger("badLoginAttempts", 0);
    public static final int refreshTokenCount = Integer.getInteger("refreshTokenCount", 0);

    public static final String serverUris;
    public static final List<String> serverUrisList;

    // Round-robin infinite ENTITY_ITERATOR that directs each next session to the next server
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

    // assertion properties
    public static final int maxFailedRequests = Integer.getInteger("maxFailedRequests", 0);
    public static final int maxMeanReponseTime = Integer.getInteger("maxMeanReponseTime", 300);

    // Users iterators by realm
    private static final ConcurrentMap<String, Iterator<UserInfo>> usersIteratorMap = new ConcurrentHashMap<>();

    // Clients iterators by realm
    private static final ConcurrentMap<String, Iterator<ClientInfo>> clientsIteratorMap = new ConcurrentHashMap<>();

    public static Iterator<String> getRealmsIterator() {
        return sequentialRealms ? sequentialRealmsIterator() : randomRealmsIterator();
    }

    public static Iterator<UserInfo> getUsersIterator(String realm) {
        return usersIteratorMap.computeIfAbsent(realm, (k) -> sequentialUsers ? sequentialUsersIterator(realm) : randomUsersIterator(realm));
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
                "  usersPerSec: %s\n"
                + "  rampUpPeriod: %s\n"
                + "  warmUpPeriod: %s\n"
                + "  measurementPeriod: %s\n"
                + "  filterResults: %s\n"
                + "  userThinkTime: %s\n"
                + "  refreshTokenPeriod: %s\n"
                + "  logoutPct: %s",
                usersPerSec, rampUpPeriod, warmUpPeriod, measurementPeriod, filterResults, userThinkTime, refreshTokenPeriod, logoutPct);
    }

    public static SimpleDateFormat SIMPLE_TIME = new SimpleDateFormat("HH:mm:ss");

    public static String toStringTimestamps() {
        return String.format("  simulationStartTime: %s\n"
                + "  warmUpStartTime: %s\n"
                + "  measurementStartTime: %s\n"
                + "  measurementEndTime: %s",
                SIMPLE_TIME.format(simulationStartTime),
                SIMPLE_TIME.format(warmUpStartTime),
                SIMPLE_TIME.format(measurementStartTime),
                SIMPLE_TIME.format(measurementEndTime));
    }

    public static String toStringDatasetProperties() {
        return String.format(
                "  numOfRealms: %s%s\n"
                + "  usersPerRealm: %s%s\n"
                + "  clientsPerRealm: %s\n"
                + "  realmRoles: %s\n"
                + "  realmRolesPerUser: %s\n"
                + "  clientRolesPerUser: %s\n"
                + "  clientRolesPerClient: %s\n"
                + "  hashIterations: %s",
                numOfRealms, sequentialRealms ? ",   sequential iteration starting from " + sequentialRealmsFrom : "",
                usersPerRealm, sequentialUsers ? ",   sequential iteration starting from " + sequentialUsersFrom : "",
                clientsPerRealm,
                realmRoles,
                realmRolesPerUser,
                clientRolesPerUser,
                clientRolesPerClient,
                hashIterations);
    }

    public static String toStringAssertionProperties() {
        return String.format("  maxFailedRequests: %s\n"
                + "  maxMeanReponseTime: %s",
                maxFailedRequests,
                maxMeanReponseTime);
    }

    public static Iterator<UserInfo> sequentialUsersIterator(final String realm) {

        return new Iterator<UserInfo>() {

            int idx = sequentialUsers ? sequentialUsersFrom : 0;

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
                String firstName = computeFirstName(idx);
                idx += 1;
                return new UserInfo(user,
                        computePassword(user),
                        firstName,
                        computeLastName(realm),
                        computeEmail(user)
                );
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
                int idx = ThreadLocalRandom.current().nextInt(usersPerRealm);
                String user = computeUsername(realm, idx);
                return new UserInfo(user,
                        computePassword(user),
                        computeFirstName(idx),
                        computeLastName(realm),
                        computeEmail(user)
                );
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

    public static Iterator<String> sequentialRealmsIterator() {

        return new Iterator<String>() {

            int idx = sequentialRealms ? sequentialRealmsFrom : 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public String next() {
                if (idx >= numOfRealms) {
                    idx = 0;
                }
                String realm = "realm_" + idx;
                idx += 1;
                return realm;
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

    public static void validateConfiguration() {
        if (realmRolesPerUser > realmRoles) {
            throw new RuntimeException("Can't have more realmRolesPerUser than there are realmRoles");
        }
        if (clientRolesPerUser > clientsPerRealm * clientRolesPerClient) {
            throw new RuntimeException("Can't have more clientRolesPerUser than there are all client roles (clientsPerRealm * clientRolesPerClient)");
        }
        if (sequentialRealmsFrom < -1 || sequentialRealmsFrom >= numOfRealms) {
            throw new RuntimeException("The folowing condition must be met: (-1 <= sequentialRealmsFrom < numOfRealms).");
        }
        if (sequentialUsersFrom < -1 || sequentialUsersFrom >= usersPerRealm) {
            throw new RuntimeException("The folowing condition must be met: (-1 <= sequentialUsersFrom < usersPerRealm).");
        }
        if (logoutPct < 0 || logoutPct > 100) {
            throw new RuntimeException("The `logoutPct` needs to be between 0 and 100.");
        }
    }

}
