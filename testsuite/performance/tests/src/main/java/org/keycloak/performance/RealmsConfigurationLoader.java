package org.keycloak.performance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import static org.keycloak.performance.RealmsConfigurationBuilder.EXPORT_FILENAME;

import static org.keycloak.performance.TestConfig.numOfWorkers;

/**
 * # build
 * mvn -f testsuite/integration-arquillian/tests/performance/gatling-perf clean install
 *
 * # generate benchmark-realms.json file with generated test data
 * mvn -f testsuite/integration-arquillian/tests/performance/gatling-perf exec:java -Dexec.mainClass=org.keycloak.performance.RealmsConfigurationBuilder -DnumOfRealms=2 -DusersPerRealm=2 -DclientsPerRealm=2 -DrealmRoles=2 -DrealmRolesPerUser=2 -DclientRolesPerUser=2 -DclientRolesPerClient=2
 *
 * # use benchmark-realms.json to load the data up to Keycloak Server listening on localhost:8080
 * mvn -f testsuite/integration-arquillian/tests/performance/gatling-perf exec:java -Dexec.mainClass=org.keycloak.performance.RealmsConfigurationLoader -DnumOfWorkers=5 -Dexec.args=benchmark-realms.json > perf-output.txt
 *
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class RealmsConfigurationLoader {

    static final int ERROR_CHECK_INTERVAL = 10;

    // multi-thread mechanics
    static final BlockingQueue<AdminJob> queue = new LinkedBlockingQueue<>(numOfWorkers);
    static final ArrayList<Worker> workers = new ArrayList<>();
    static final ConcurrentLinkedQueue<PendingResult> pendingResult = new ConcurrentLinkedQueue<>();

    // realm caches - we completely handle one realm before starting the next
    static ConcurrentHashMap<String, String> clientIdMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, String> realmRoleIdMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, Map<String, String>> clientRoleIdMap = new ConcurrentHashMap<>();
    static boolean realmCreated;

    public static void main(String [] args) throws IOException {
        System.out.println("Keycloak servers: "+TestConfig.serverUrisList);

        if (args.length == 0) {
            args = new String[] {EXPORT_FILENAME};
        }

        if (args.length != 1) {
            System.out.println("Usage: java " + RealmsConfigurationLoader.class.getName() + " <FILE>");
            return;
        }

        String file = args[0];
        System.out.println("Using file: " + new File(args[0]).getAbsolutePath());
        System.out.println("Number of workers (numOfWorkers): " + numOfWorkers);

        JsonParser p = initParser(file);

        initWorkers();

        try {

            // read json file using JSON stream API
            readRealms(p);

        } finally {

            completeWorkers();
        }
    }

    private static void completeWorkers() {

        try {
            // wait for all jobs to finish
            completePending();

        } finally {
            // stop workers
            for (Worker w : workers) {
                w.exit = true;
                try {
                    w.join(5000);
                    if (w.isAlive()) {
                        System.out.println("Worker thread failed to stop: ");
                        dumpThread(w);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted");
                }
            }
        }
    }

    private static void readRealms(JsonParser p) throws IOException {
        JsonToken t = p.nextToken();

        while (t != JsonToken.END_OBJECT && t != JsonToken.END_ARRAY) {
            if (t != JsonToken.START_ARRAY) {
                readRealm(p);
            }
            t = p.nextToken();
        }
    }

    private static void initWorkers() {
        // configure job queue and worker threads
        for (int i = 0; i < numOfWorkers; i++) {
            workers.add(new Worker());
        }
    }

    private static JsonParser initParser(String file) {
        JsonParser p;
        try {
            JsonFactory f = new JsonFactory();
            p = f.createParser(new File(file));

            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            p.setCodec(mapper);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse file " + new File(file).getAbsolutePath(), e);
        }
        return p;
    }

    private static void dumpThread(Worker w) {
        StringBuilder b = new StringBuilder();
        for (StackTraceElement e: w.getStackTrace()) {
            b.append(e.toString()).append("\n");
        }
        System.out.print(b);
    }

    private static void readRealm(JsonParser p) throws IOException {

        // as soon as we encounter users, roles, clients we create a CreateRealmJob
        // TODO: if after that point in a realm we encounter realm attribute, we report a warning but continue

        RealmRepresentation r = new RealmRepresentation();
        JsonToken t = p.nextToken();
        while (t != JsonToken.END_OBJECT) {

            //System.out.println(t + ", name: " + p.getCurrentName() + ", text: '" + p.getText() + "', value: " + p.getValueAsString());

            switch (p.getCurrentName()) {
                case "realm":
                    r.setRealm(getStringValue(p));
                    break;
                case "enabled":
                    r.setEnabled(getBooleanValue(p));
                    break;
                case "accessTokenLifespan":
                    r.setAccessCodeLifespan(getIntegerValue(p));
                    break;
                case "registrationAllowed":
                    r.setRegistrationAllowed(getBooleanValue(p));
                    break;
                case "passwordPolicy":
                    r.setPasswordPolicy(getStringValue(p));
                    break;
                case "users":
                    ensureRealm(r);
                    readUsers(r, p);
                    break;
                case "roles":
                    ensureRealm(r);
                    readRoles(r, p);
                    break;
                case "clients":
                    ensureRealm(r);
                    readClients(r, p);
                    break;
                default: {
                    // if we don't understand the field we ignore it - but report that
                    System.out.println("Realm attribute ignored: " + p.getCurrentName());
                    consumeAttribute(p);
                }
            }
            t = p.nextToken();
        }

        // we wait for realm to complete
        completePending();

        // reset realm specific cache
        realmCreated = false;
        clientIdMap.clear();
        realmRoleIdMap.clear();
        clientRoleIdMap.clear();
    }

    private static void ensureRealm(RealmRepresentation r) {
        if (!realmCreated) {
            createRealm(r);
            realmCreated = true;
        }
    }

    private static void createRealm(RealmRepresentation r) {
        try {
            queue.put(new CreateRealmJob(r));
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }

        // now wait for job to appear
        PendingResult next = pendingResult.poll();
        while (next == null) {
            waitForAwhile();
            next = pendingResult.poll();
        }

        // then wait for the job to complete
        while (!next.isDone()) {
            waitForAwhile();
        }

        try {
            next.get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Execution failed", e.getCause());
        }
    }

    private static void enqueueCreateUser(RealmRepresentation r, UserRepresentation u) {
        try {
            queue.put(new CreateUserJob(r, u));
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
    }

    private static void enqueueCreateRealmRole(RealmRepresentation r, RoleRepresentation role) {
        try {
            queue.put(new CreateRealmRoleJob(r, role));
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
    }

    private static void enqueueCreateClientRole(RealmRepresentation r, RoleRepresentation role, String client) {
        try {
            queue.put(new CreateClientRoleJob(r, role, client));
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
    }

    private static void enqueueCreateClient(RealmRepresentation r, ClientRepresentation client) {
        try {
            queue.put(new CreateClientJob(r, client));
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
    }

    private static void waitForAwhile() {
        waitForAwhile(100, "Interrupted");
    }

    private static void waitForAwhile(int millis) {
        waitForAwhile(millis, "Interrupted");
    }

    private static void waitForAwhile(int millis, String interruptMessage) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(interruptMessage);
        }
    }

    private static void readUsers(RealmRepresentation r, JsonParser p) throws IOException {
        JsonToken t = p.nextToken();
        if (t != JsonToken.START_ARRAY) {
            throw new RuntimeException("Error reading field 'users'. Expected array of users [" + t + "]");
        }
        int count = 0;
        t = p.nextToken();
        while (t == JsonToken.START_OBJECT) {
            UserRepresentation u = p.readValueAs(UserRepresentation.class);
            enqueueCreateUser(r, u);
            t = p.nextToken();
            count += 1;

            // every some users check to see pending errors
            // in order to short-circuit if any errors have occurred
            if (count % ERROR_CHECK_INTERVAL == 0) {
                checkPendingErrors();
            }
        }
    }

    private static void readRoles(RealmRepresentation r, JsonParser p) throws IOException {
        JsonToken t = p.nextToken();
        if (t != JsonToken.START_OBJECT) {
            throw new RuntimeException("Error reading field 'roles'. Expected start of object [" + t + "]");
        }

        t = p.nextToken();
        if (t != JsonToken.FIELD_NAME) {
            throw new RuntimeException("Error reading field 'roles'. Expected field 'realm' or 'client' [" + t + "]");
        }

        while (t != JsonToken.END_OBJECT) {
            switch (p.getCurrentName()) {
                case "realm":
                    readRealmRoles(r, p);
                    break;
                case "client":
                    waitForClientsCompleted();
                    readClientRoles(r, p);
                    break;
                default:
                    throw new RuntimeException("Unexpected field in roles: " + p.getCurrentName());
            }
            t = p.nextToken();
        }
    }

    private static void waitForClientsCompleted() {
        completePending();
    }

    private static void readClientRoles(RealmRepresentation r, JsonParser p) throws IOException {
        JsonToken t = p.nextToken();

        if (t != JsonToken.START_OBJECT) {
            throw new RuntimeException("Expected start_of_object on 'roles/client' [" + t + "]");
        }

        t = p.nextToken();

        int count = 0;
        while (t == JsonToken.FIELD_NAME) {
            String client = p.getCurrentName();

            t = p.nextToken();
            if (t != JsonToken.START_ARRAY) {
                throw new RuntimeException("Expected start_of_array on 'roles/client/" + client + " [" + t + "]");
            }

            t = p.nextToken();
            while (t != JsonToken.END_ARRAY) {
                RoleRepresentation u = p.readValueAs(RoleRepresentation.class);
                enqueueCreateClientRole(r, u, client);
                t = p.nextToken();
                count += 1;

                // every some roles check to see pending errors
                // in order to short-circuit if any errors have occurred
                if (count % ERROR_CHECK_INTERVAL == 0) {
                    checkPendingErrors();
                }
            }
            t = p.nextToken();
        }
    }

    private static void readRealmRoles(RealmRepresentation r, JsonParser p) throws IOException {
        JsonToken t = p.nextToken();

        if (t != JsonToken.START_ARRAY) {
            throw new RuntimeException("Expected start_of_array on 'roles/realm' [" + t + "]");
        }

        t = p.nextToken();

        int count = 0;
        while (t == JsonToken.START_OBJECT) {
            RoleRepresentation u = p.readValueAs(RoleRepresentation.class);
            enqueueCreateRealmRole(r, u);
            t = p.nextToken();
            count += 1;

            // every some roles check to see pending errors
            // in order to short-circuit if any errors have occurred
            if (count % ERROR_CHECK_INTERVAL == 0) {
                checkPendingErrors();
            }
        }
    }

    private static void readClients(RealmRepresentation r, JsonParser p) throws IOException {
        JsonToken t = p.nextToken();
        if (t != JsonToken.START_ARRAY) {
            throw new RuntimeException("Error reading field 'clients'. Expected array of clients [" + t + "]");
        }
        int count = 0;
        t = p.nextToken();
        while (t == JsonToken.START_OBJECT) {
            ClientRepresentation u = p.readValueAs(ClientRepresentation.class);
            enqueueCreateClient(r, u);
            t = p.nextToken();
            count += 1;

            // every some users check to see pending errors
            if (count % ERROR_CHECK_INTERVAL == 0) {
                checkPendingErrors();
            }
        }
    }

    private static void checkPendingErrors() {
        // now wait for job to appear
        PendingResult next = pendingResult.peek();
        while (next == null) {
            waitForAwhile();
            next = pendingResult.peek();
        }

        // now process then
        Iterator<PendingResult> it = pendingResult.iterator();
        while (it.hasNext()) {
            next = it.next();
            if (next.isDone() && !next.isCompletedExceptionally()) {
                it.remove();
            } else if (next.isCompletedExceptionally()) {
                try {
                    next.get();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted");
                } catch (ExecutionException e) {
                    throw new RuntimeException("Execution failed", e.getCause());
                }
            }
        }
    }

    private static void completePending() {

        // wait for queue to empty up
        while (queue.size() > 0) {
            waitForAwhile();
        }

        PendingResult next;
        while ((next = pendingResult.poll()) != null) {
            try {
                next.get();
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted");
            } catch (ExecutionException e) {
                throw new RuntimeException("Execution failed", e.getCause());
            }
        }
    }

    private static Integer getIntegerValue(JsonParser p) throws IOException {
        JsonToken t = p.nextToken();
        if (t != JsonToken.VALUE_NUMBER_INT) {
            throw new RuntimeException("Error while reading field '" + p.getCurrentName() + "'. Expected integer value [" + t + "]");
        }
        return p.getValueAsInt();
    }

    private static void consumeAttribute(JsonParser p) throws IOException {
        JsonToken t = p.nextToken();
        if (t == JsonToken.START_OBJECT || t == JsonToken.START_ARRAY) {
            p.skipChildren();
        }
    }

    private static Boolean getBooleanValue(JsonParser p) throws IOException {
        JsonToken t = p.nextToken();
        if (t !=  JsonToken.VALUE_TRUE && t != JsonToken.VALUE_FALSE) {
            throw new RuntimeException("Error while reading field '" + p.getCurrentName() + "'. Expected boolean value [" + t + "]");
        }
        return p.getValueAsBoolean();
    }

    private static String getStringValue(JsonParser p) throws IOException {
        JsonToken t = p.nextToken();
        if (t !=  JsonToken.VALUE_STRING) {
            throw new RuntimeException("Error while reading field '" + p.getCurrentName() + "'. Expected string value [" + t + "]");
        }
        return p.getText();
    }

    static class Worker extends Thread {

        volatile boolean exit = false;

        Worker() {
            start();
        }

        public void run() {
            while (!exit) {
                Job r = queue.poll();
                if (r == null) {
                    waitForAwhile(50, "Worker thread " + this.getName() + " interrupted");
                    continue;
                }
                PendingResult pending = new PendingResult(r);
                pendingResult.add(pending);
                try {
                    r.run();
                    pending.complete(true);
                } catch (Throwable t) {
                    pending.completeExceptionally(t);
                }
            }
        }
    }

    static class CreateRealmJob extends AdminJob {

        private RealmRepresentation realm;

        CreateRealmJob(RealmRepresentation r) {
            this.realm = r;
        }

        @Override
        public void run() {
            admin().realms().create(realm);
        }
    }

    static class CreateUserJob extends AdminJob {

        private RealmRepresentation realm;
        private UserRepresentation user;

        CreateUserJob(RealmRepresentation r, UserRepresentation u) {
            this.realm = r;
            this.user = u;
        }

        @Override
        public void run() {
            Response response = admin().realms().realm(realm.getRealm()).users().create(user);
            response.close();
            if (response.getStatus() != 201) {
                throw new RuntimeException("Failed to create user with status: " + response.getStatusInfo().getReasonPhrase());
            }

            String userId = extractIdFromResponse(response);

            List<CredentialRepresentation> creds = user.getCredentials();
            for (CredentialRepresentation cred: creds) {
                admin().realms().realm(realm.getRealm()).users().get(userId).resetPassword(cred);
            }

            List<String> realmRoles = user.getRealmRoles();
            if (realmRoles != null && !realmRoles.isEmpty()) {
                List<RoleRepresentation> roles = convertRealmRoleNamesToRepresentation(user.getRealmRoles());
                if (!roles.isEmpty()) {
                    admin().realms().realm(realm.getRealm()).users().get(userId).roles().realmLevel().add(roles);
                }
            }

            Map<String, List<String>> clientRoles = user.getClientRoles();
            if (clientRoles != null && !clientRoles.isEmpty()) {
                for (String clientId: clientRoles.keySet()) {
                    List<String> roleNames = clientRoles.get(clientId);
                    if (roleNames != null && !roleNames.isEmpty()) {
                        List<RoleRepresentation> reps = convertClientRoleNamesToRepresentation(clientId, roleNames);
                        if (!reps.isEmpty()) {
                            String idOfClient = clientIdMap.get(clientId);
                            if (idOfClient == null) {
                                throw new RuntimeException("No client created for clientId: " + clientId);
                            }
                            admin().realms().realm(realm.getRealm()).users().get(userId).roles().clientLevel(idOfClient).add(reps);
                        }
                    }
                }
            }
        }

        private List<RoleRepresentation> convertClientRoleNamesToRepresentation(String clientId, List<String> roles) {
            LinkedList<RoleRepresentation> result = new LinkedList<>();
            Map<String, String> roleIdMap = clientRoleIdMap.get(clientId);
            if (roleIdMap == null || roleIdMap.isEmpty()) {
                throw new RuntimeException("No client roles created for clientId: " + clientId);
            }

            for (String role: roles) {
                RoleRepresentation r = new RoleRepresentation();
                String id = roleIdMap.get(role);
                if (id == null) {
                    throw new RuntimeException("No client role created on client '" + clientId + "' for name: " + role);
                }
                r.setId(id);
                r.setName(role);
                result.add(r);
            }
            return result;
        }

        private List<RoleRepresentation> convertRealmRoleNamesToRepresentation(List<String> roles) {
            LinkedList<RoleRepresentation> result = new LinkedList<>();
            for (String role: roles) {
                RoleRepresentation r = new RoleRepresentation();
                String id = realmRoleIdMap.get(role);
                if (id == null) {
                    throw new RuntimeException("No realm role created for name: " + role);
                }
                r.setId(id);
                r.setName(role);
                result.add(r);
            }
            return result;
        }
    }

    static class CreateRealmRoleJob extends AdminJob {

        private RealmRepresentation realm;
        private RoleRepresentation role;

        CreateRealmRoleJob(RealmRepresentation r, RoleRepresentation role) {
            this.realm = r;
            this.role = role;
        }

        @Override
        public void run() {
            admin().realms().realm(realm.getRealm()).roles().create(role);

            // we need the id but it's not returned by REST API - we have to perform a get on the created role and save the returned id
            RoleRepresentation rr = admin().realms().realm(realm.getRealm()).roles().get(role.getName()).toRepresentation();
            realmRoleIdMap.put(rr.getName(), rr.getId());
        }
    }


    static class CreateClientRoleJob extends AdminJob {

        private RealmRepresentation realm;
        private RoleRepresentation role;
        private String clientId;

        CreateClientRoleJob(RealmRepresentation r, RoleRepresentation role, String clientId) {
            this.realm = r;
            this.role = role;
            this.clientId = clientId;
        }

        @Override
        public void run() {
            String id = clientIdMap.get(clientId);
            if (id == null) {
                throw new RuntimeException("No client created for clientId: " + clientId);
            }
            admin().realms().realm(realm.getRealm()).clients().get(id).roles().create(role);

            // we need the id but it's not returned by REST API - we have to perform a get on the created role and save the returned id
            RoleRepresentation rr = admin().realms().realm(realm.getRealm()).clients().get(id).roles().get(role.getName()).toRepresentation();

            Map<String, String> roleIdMap = clientRoleIdMap.get(clientId);
            if (roleIdMap == null) {
                roleIdMap = clientRoleIdMap.computeIfAbsent(clientId, (k) -> new ConcurrentHashMap<>());
            }

            roleIdMap.put(rr.getName(), rr.getId());
        }
    }

    static class CreateClientJob extends AdminJob {


        private ClientRepresentation client;
        private RealmRepresentation realm;

        public CreateClientJob(RealmRepresentation r, ClientRepresentation client) {
            this.realm = r;
            this.client = client;
        }

        @Override
        public void run() {
            Response response = admin().realms().realm(realm.getRealm()).clients().create(client);
            response.close();
            if (response.getStatus() != 201) {
                throw new RuntimeException("Failed to create client with status: " + response.getStatusInfo().getReasonPhrase());
            }
            String id = extractIdFromResponse(response);
            clientIdMap.put(client.getClientId(), id);
        }
    }


    static String extractIdFromResponse(Response response) {
        String location = response.getHeaderString("Location");
        if (location == null)
            return null;

        int last = location.lastIndexOf("/");
        if (last == -1) {
            return null;
        }
        String id = location.substring(last + 1);
        if (id == null || "".equals(id)) {
            throw new RuntimeException("Failed to extract 'id' of created resource");
        }

        return id;
    }

    static abstract class AdminJob extends Job {

        static Keycloak admin = Keycloak.getInstance(TestConfig.serverUrisList.get(0), TestConfig.authRealm, TestConfig.authUser, TestConfig.authPassword, TestConfig.authClient);

        static Keycloak admin() {
            return admin;
        }
    }

    static abstract class Job implements Runnable {

    }

    static class PendingResult extends CompletableFuture<Boolean> {

        Job job;

        PendingResult(Job job) {
            this.job = job;
        }
    }
}
