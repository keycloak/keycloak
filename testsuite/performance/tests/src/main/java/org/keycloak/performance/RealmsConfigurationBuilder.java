package org.keycloak.performance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.keycloak.common.util.ObjectUtil.capitalize;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class RealmsConfigurationBuilder {

    private Random RANDOM = new Random();

    private JsonGenerator g;

    private String file;

    public static final String EXPORT_FILENAME = "benchmark-realms.json";

    public RealmsConfigurationBuilder(String filename) {
        this.file = filename;

        try {
            JsonFactory f = new JsonFactory();
            g = f.createGenerator(new File(file), JsonEncoding.UTF8);

            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            g.setCodec(mapper);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create realms export file", e);
        }
    }

    public void build() throws IOException {
        // There are several realms
        startRealms();
        for (int i = 0; i < TestConfig.numOfRealms; i++) {
            RealmConfig realm = new RealmConfig();

            String realmName = "realm_" + i;
            startRealm(realmName, realm);

            // first create clients,
            // then roles, and client roles
            // create users at the end
            // reason: each next depends on availability of the previous

            // each realm has some clients
            startClients();
            for (int j = 0; j < TestConfig.clientsPerRealm; j++) {
                ClientRepresentation client = new ClientRepresentation();

                String clientId = computeClientId(realmName, j);
                client.setClientId(clientId);
                client.setEnabled(true);

                String baseDir = computeAppUrl(clientId);
                client.setBaseUrl(baseDir);

                List<String> uris = new ArrayList<>();
                uris.add(baseDir + "/*");

                if (isClientConfidential(j)) {
                    client.setRedirectUris(uris);
                    client.setSecret(computeSecret(clientId));
                } else if (isClientBearerOnly(j)) {
                    client.setBearerOnly(true);
                } else {
                    client.setPublicClient(true);
                    client.setRedirectUris(uris);
                }

                addClient(client);
            }

            completeClients();

            // each realm has some realm roles
            startRoles();
            startRealmRoles();
            for (int j = 0; j < TestConfig.realmRoles; j++) {
                addRole("role_" + j + "_ofRealm_" + i);
            }
            completeRealmRoles();

            // each client has some client roles
            startClientRoles();
            for (int j = 0; j < TestConfig.clientsPerRealm; j++) {
                addClientRoles("client_" + j + "_ofRealm_" + i);
            }
            completeClientRoles();

            completeRoles();

            // each realm so many users
            startUsers();
            for (int j = 0; j < TestConfig.usersPerRealm; j++) {
                UserRepresentation user = new UserRepresentation();
                user.setUsername(computeUsername(realmName, j));
                user.setEnabled(true);
                user.setEmail(computeEmail(user.getUsername()));
                user.setFirstName(computeFirstName(j));
                user.setLastName(computeLastName(realmName));

                CredentialRepresentation creds = new CredentialRepresentation();
                creds.setType("password");
                creds.setValue(computePassword(user.getUsername()));
                user.setCredentials(Arrays.asList(creds));

                // add realm roles
                // each user some random realm roles
                Set<String> realmRoles = new HashSet<String>();
                while (realmRoles.size() < TestConfig.realmRolesPerUser) {
                    realmRoles.add("role_" + random(TestConfig.realmRoles) + "_ofRealm_" + i);
                }
                user.setRealmRoles(new ArrayList<>(realmRoles));

                // add client roles
                // each user some random client roles (random client + random role on that client)
                Set<String> clientRoles = new HashSet<String>();
                while (clientRoles.size() < TestConfig.clientRolesPerUser) {
                    int client = random(TestConfig.clientsPerRealm);
                    int clientRole = random(TestConfig.clientRolesPerClient);
                    clientRoles.add("clientrole_" + clientRole + "_ofClient_" + client + "_ofRealm_" + i);
                }
                Map<String, List<String>> clientRoleMappings = new HashMap<>();
                for (String item : clientRoles) {
                    int s = item.indexOf("_ofClient_");
                    int b = s + "_ofClient_".length();
                    int e = item.indexOf("_", b);
                    String key = "client_" + item.substring(b, e) + "_ofRealm_" + i;
                    List<String> cliRoles = clientRoleMappings.get(key);
                    if (cliRoles == null) {
                        cliRoles = new ArrayList<>();
                        clientRoleMappings.put(key, cliRoles);
                    }
                    cliRoles.add(item);
                }
                user.setClientRoles(clientRoleMappings);
                addUser(user);
            }
            completeUsers();

            completeRealm();
        }
        completeRealms();
        g.close();
    }

    private void addClientRoles(String client) throws IOException {
        g.writeArrayFieldStart(client);
        for (int i = 0; i < TestConfig.clientRolesPerClient; i++) {
            g.writeStartObject();
            String name = "clientrole_" + i + "_of" + capitalize(client);
            g.writeStringField("name", name);
            g.writeStringField("description", "Test realm role - " + name);
            g.writeEndObject();
        }
        g.writeEndArray();
    }

    private void addClient(ClientRepresentation client) throws IOException {
        g.writeObject(client);
    }

    private void startClients() throws IOException {
        g.writeArrayFieldStart("clients");
    }

    private void completeClients() throws IOException {
        g.writeEndArray();
    }

    private void startRealms() throws IOException {
        g.writeStartArray();
    }

    private void completeRealms() throws IOException {
        g.writeEndArray();
    }

    private int random(int max) {
        return RANDOM.nextInt(max);
    }

    private void startRealm(String realmName, RealmConfig conf) throws IOException {
        g.writeStartObject();
        g.writeStringField("realm", realmName);
        g.writeBooleanField("enabled", true);
        g.writeNumberField("accessTokenLifespan", conf.accessTokenLifeSpan);
        g.writeStringField("sslRequired", "none");
        g.writeBooleanField("registrationAllowed", conf.registrationAllowed);
        g.writeStringField("passwordPolicy", "hashIterations(" + TestConfig.hashIterations + ")");

        /*
        if (conf.requiredCredentials != null) {
            g.writeArrayFieldStart("requiredCredentials");
            //g.writeStartArray();
            for (String item: conf.requiredCredentials) {
                g.writeString(item);
            }
            g.writeEndArray();
        }
         */
    }

    private void completeRealm() throws IOException {
        g.writeEndObject();
    }

    private void startUsers() throws IOException {
        g.writeArrayFieldStart("users");
    }

    private void completeUsers() throws IOException {
        g.writeEndArray();
    }

    private void addUser(UserRepresentation user) throws IOException {
        g.writeObject(user);
    }

    private void startRoles() throws IOException {
        g.writeObjectFieldStart("roles");
    }

    private void startRealmRoles() throws IOException {
        g.writeArrayFieldStart("realm");
    }

    private void startClientRoles() throws IOException {
        g.writeObjectFieldStart("client");
    }

    private void completeClientRoles() throws IOException {
        g.writeEndObject();
    }

    private void addRole(String role) throws IOException {
        g.writeStartObject();
        g.writeStringField("name", role);
        g.writeStringField("description", "Test realm role - " + role);
        g.writeEndObject();
    }

    private void completeRealmRoles() throws IOException {
        g.writeEndArray();
    }

    private void completeRoles() throws IOException {
        g.writeEndObject();
    }


    static boolean isClientConfidential(int index) {
        // every third client starting with 0
        return index % 3 == 0;
    }

    static boolean isClientBearerOnly(int index) {
        // every third client starting with 1
        return index % 3 == 1;
    }

    static boolean isClientPublic(int index) {
        // every third client starting with 2
        return index % 3 == 2;
    }

    static String computeClientId(String realm, int idx) {
        return "client_" + idx + "_of" + capitalize(realm);
    }

    static String computeAppUrl(String clientId) {
        return "http://keycloak-test-" + clientId.toLowerCase();
    }

    static String computeSecret(String clientId) {
        return "secretFor_" + clientId;
    }

    static String computeUsername(String realm, int idx) {
        return "user_" + idx + "_of" + capitalize(realm);
    }

    static String computePassword(String username) {
        return "passOfUser_" + username;
    }

    static String computeFirstName(int userIdx) {
        return "User" + userIdx;
    }

    static String computeLastName(String realm) {
        return "O'" + realm.replace("_", "");
    }

    static String computeEmail(String username) {
        return username + "@example.com";
    }

    


    public static void main(String[] args) throws IOException {

        File exportFile = new File(EXPORT_FILENAME);

        TestConfig.validateConfiguration();
        System.out.println("Generating test dataset with the following parameters: \n" + TestConfig.toStringDatasetProperties());

        new RealmsConfigurationBuilder(exportFile.getAbsolutePath()).build();

        System.out.println("Created " + exportFile.getAbsolutePath());
    }
}
