package org.keycloak.tests.ssf.transmitter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.partialimport.PartialImportResults;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.risc.RiscAccountPurged;
import org.keycloak.ssf.subject.EmailSubjectId;
import org.keycloak.ssf.subject.IssuerSubjectId;
import org.keycloak.ssf.transmitter.DefaultSsfTransmitterProviderFactory;
import org.keycloak.ssf.transmitter.SsfScopes;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.event.SsfUserSubjectFormats;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.StreamDeliveryConfig;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.support.SsfTransmitterUrls;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.server.DefaultKeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testframework.util.HttpServerUtil;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the RISC {@code account-purged} event.
 *
 * <p>These cover ground the mapper unit tests structurally cannot. Every purge
 * happens <em>after</em> Keycloak has already deleted the user row, so the SET's
 * subject can only come from the {@code PurgedUserSnapshot} captured on
 * {@code UserModel.UserPreRemovedEvent}. The unit tests construct the mapper with a
 * {@code null} session and so never exercise the capture hook, the listener's
 * snapshot fallback, or the dispatch-time subject gate — the three places where a
 * regression would silently stop purge events reaching receivers.
 *
 * <p>The negative cases matter as much as the positive ones: Keycloak has several
 * paths that delete a user row without an account ceasing to exist, and they are kept
 * out of the purge branch by the admin event's operation type and resource type rather
 * than by anything deliberate. Those are pinned here so a future widening of the path
 * matching cannot start emitting spurious GDPR-relevant signals unnoticed.
 */
@KeycloakIntegrationTest(config = SsfTransmitterAccountPurgedEventTests.AccountPurgedServerConfig.class)
public class SsfTransmitterAccountPurgedEventTests {

    static final String RECEIVER = "ssf-receiver-account-purged";
    static final String RECEIVER_SECRET = "receiver-account-purged-secret";

    static final String PUSH_CONTEXT_PATH = "/ssf/push-account-purged";
    static final String MOCK_PUSH_ENDPOINT = "http://127.0.0.1:8500" + PUSH_CONTEXT_PATH;
    static final String PUSH_AUTH_HEADER = "Bearer dummy-account-purged-receiver";

    static final long PUSH_WAIT_SECONDS = 5;
    static final long NO_PUSH_WAIT_SECONDS = 3;

    @InjectRealm(config = AccountPurgedRealm.class)
    ManagedRealm realm;

    @InjectSimpleHttp
    SimpleHttp http;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectHttpServer
    HttpServer mockReceiverServer;

    private final BlockingQueue<String> pushes = new LinkedBlockingQueue<>();

    @BeforeEach
    public void setup() throws IOException {
        pushes.clear();

        mockReceiverServer.createContext(PUSH_CONTEXT_PATH, new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try (InputStream is = exchange.getRequestBody()) {
                    pushes.add(new String(is.readAllBytes(), StandardCharsets.UTF_8));
                }
                HttpServerUtil.sendResponse(exchange, 202, Map.of());
            }
        });

        assignOptionalClientScopes(RECEIVER, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);

        // Tests in this class deliberately vary the receiver's subject format and
        // default_subjects policy, so reset both to the class defaults rather than
        // inheriting whatever the previous test left behind.
        setReceiverAttributes(Map.of(
                ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "ALL",
                ClientStreamStore.SSF_STREAM_USER_SUBJECT_FORMAT_KEY, IssuerSubjectId.TYPE));

        createPushStream(Set.of(RiscAccountPurged.TYPE));
    }

    @AfterEach
    public void cleanup() {
        bestEffortDeleteStream();
        // Organizations created by the +tenant / org-notify tests would otherwise
        // leak into later tests and change their subject-gate verdicts.
        bestEffortDeleteAllOrganizations();
        try {
            mockReceiverServer.removeContext(PUSH_CONTEXT_PATH);
        } catch (IllegalArgumentException ignored) {
        }
    }

    // --- the purge itself ---------------------------------------------------

    @Test
    public void adminDeletesUser_deliversAccountPurgedEvent() throws Exception {
        String userId = createUser("purge-basic", "purge-basic@local.test");

        deleteUser(userId);

        JsonNode set = decodeSet(awaitPush("an admin user deletion must auto-emit RiscAccountPurged"));

        Assertions.assertTrue(set.path("events").has(RiscAccountPurged.TYPE),
                "delivered SET should carry the account-purged event type");
        Assertions.assertEquals("admin",
                set.path("events").path(RiscAccountPurged.TYPE).path("initiating_entity").asText(),
                "a deletion through the admin REST API must report initiating_entity=admin");

        // The decisive assertion: the user row was gone before the admin event fired,
        // so a correct subject here can only have come from the pre-removal snapshot.
        Assertions.assertEquals(IssuerSubjectId.TYPE, set.path("sub_id").path("format").asText(),
                "default user subject format is iss_sub");
        Assertions.assertEquals(userId, set.path("sub_id").path("sub").asText(),
                "sub_id.sub must identify the user that was purged");
    }

    @Test
    public void emailSubjectFormat_purgedSubjectCarriesEmail() throws Exception {
        // Exercises the snapshot fallback in SecurityEventTokenMapper.lookupUserEmail:
        // for a purged user the live lookup always misses, so an email-format stream
        // can only be served from the attributes copied before deletion.
        setReceiverAttributes(Map.of(
                ClientStreamStore.SSF_STREAM_USER_SUBJECT_FORMAT_KEY, EmailSubjectId.TYPE));

        String email = "purge-email@local.test";
        String userId = createUser("purge-email", email);

        deleteUser(userId);

        JsonNode set = decodeSet(awaitPush(
                "an email-format stream must still receive a purge event for a deleted user"));

        Assertions.assertEquals(EmailSubjectId.TYPE, set.path("sub_id").path("format").asText(),
                "stream is configured for the email subject format");
        Assertions.assertEquals(email, set.path("sub_id").path("email").asText(),
                "the purged user's email must survive in the snapshot");
    }

    // --- subject gate -------------------------------------------------------

    @Test
    public void defaultSubjectsNone_notifiedUser_purgeDelivered() throws Exception {
        // The highest-value case in this class. default_subjects=NONE is the
        // transmitter default, and under it the dispatcher delivers only to subjects
        // explicitly carrying ssf.notify.<clientId>=true. That attribute lives on the
        // user, which no longer exists by the time the gate runs — so this passes only
        // if the snapshot faithfully carried the user's attributes across the deletion.
        setReceiverAttributes(Map.of(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "NONE"));

        String userId = createUser("purge-notified", "purge-notified@local.test");
        // Subscribe through the SSF admin endpoint rather than by setting the
        // attribute on the user representation: ssf.notify.<clientId> is an
        // unmanaged attribute, which User Profile silently drops on the admin
        // user API by default. This is also the path an operator actually uses.
        addSubject(userId);

        deleteUser(userId);

        JsonNode set = decodeSet(awaitPush(
                "a subscribed user's purge must be delivered even under default_subjects=NONE"));
        Assertions.assertEquals(userId, set.path("sub_id").path("sub").asText(),
                "delivered purge should be for the subscribed user");
    }

    @Test
    public void defaultSubjectsNone_unnotifiedUser_purgeSuppressed() throws Exception {
        // Negative control for the test above: without it, that test would still pass
        // if the subject gate were bypassed altogether for purge events.
        setReceiverAttributes(Map.of(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "NONE"));

        String userId = createUser("purge-unnotified", "purge-unnotified@local.test");

        deleteUser(userId);

        Assertions.assertNull(pushes.poll(NO_PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "under default_subjects=NONE an unsubscribed user's purge must be suppressed");
    }

    // --- organizations ------------------------------------------------------

    @Test
    public void tenantSubjectFormat_purgedSubjectCarriesOrgAlias() throws Exception {
        // Organization membership is the one thing the snapshot genuinely cannot
        // answer on demand: OrganizationProvider.getByMember is an id-keyed query,
        // not an attribute read, so a detached user resolves to no organizations.
        // The alias therefore has to be resolved eagerly at capture time and served
        // back through buildTenantSubjectFromSnapshot.
        String orgAlias = createOrg(false);
        setReceiverAttributes(Map.of(
                ClientStreamStore.SSF_STREAM_USER_SUBJECT_FORMAT_KEY,
                SsfUserSubjectFormats.COMPLEX_ISS_SUB_PLUS_TENANT));

        String userId = createUser("purge-tenant", "purge-tenant@" + orgAlias + ".local.test");
        addUserToOrg(orgAlias, userId);

        deleteUser(userId);

        JsonNode set = decodeSet(awaitPush(
                "a +tenant stream must still receive a purge event for a deleted org member"));

        JsonNode subId = set.path("sub_id");
        Assertions.assertEquals("complex", subId.path("format").asText(),
                "the +tenant composition must produce a complex subject");
        Assertions.assertEquals(userId, subId.path("user").path("sub").asText(),
                "complex subject's user member identifies the purged user");
        Assertions.assertEquals(orgAlias, subId.path("tenant").path("id").asText(),
                "the organization alias must survive the deletion via the snapshot");
    }

    @Test
    public void orgNotifiedUser_purgeDelivered() throws Exception {
        // The org leg of the subject gate. The user carries no ssf.notify attribute
        // of its own -- it qualifies only through its organization, a verdict the
        // detached snapshot has to answer from attributes captured before deletion.
        String orgAlias = createOrg(true);
        setReceiverAttributes(Map.of(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "NONE"));

        String userId = createUser("purge-org-notified", "purge-org-notified@" + orgAlias + ".local.test");
        addUserToOrg(orgAlias, userId);

        deleteUser(userId);

        JsonNode set = decodeSet(awaitPush(
                "a user notified via their organization must have their purge delivered "
                        + "even under default_subjects=NONE"));
        Assertions.assertEquals(userId, set.path("sub_id").path("sub").asText(),
                "delivered purge should be for the org-notified user");
    }

    @Test
    public void orgExcludedUser_purgeSuppressed() throws Exception {
        // The exclusion leg of the org branch, under default_subjects=ALL where the
        // user would otherwise be delivered. Proves the snapshot's captured aliases
        // resolve back to live organizations and are evaluated by the same
        // SsfSubjectInclusionResolver the live path uses -- an extension overriding
        // that resolver must get the same verdict for purges as for every other event.
        String orgAlias = createOrgExcluded();
        setReceiverAttributes(Map.of(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "ALL"));

        String userId = createUser("purge-org-excluded", "purge-org-excluded@" + orgAlias + ".local.test");
        addUserToOrg(orgAlias, userId);

        deleteUser(userId);

        Assertions.assertNull(pushes.poll(NO_PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "a user excluded via their organization must not have their purge delivered, "
                        + "even under default_subjects=ALL");
    }

    // --- deletions that are not purges --------------------------------------

    @Test
    public void partialImportOverwrite_noPurgeEvent() throws Exception {
        // A partial import under the OVERWRITE policy really does delete the user row
        // -- then recreates it in the same request. The account never ceased to exist,
        // so emitting a purge would tell receivers to run data-retention cleanup for a
        // live account. It is kept out only by the admin event's operation type being
        // UPDATE rather than DELETE, which is one enum value away from the purge gate.
        String username = "purge-overwrite";
        createUser(username, "purge-overwrite@local.test");

        UserRepresentation replacement = new UserRepresentation();
        replacement.setUsername(username);
        replacement.setEmail("purge-overwrite@local.test");
        replacement.setEnabled(true);

        PartialImportRepresentation partialImport = new PartialImportRepresentation();
        // setIfResourceExists is the setter for the policy -- there is no setPolicy.
        partialImport.setIfResourceExists(PartialImportRepresentation.Policy.OVERWRITE.name());
        partialImport.setUsers(List.of(replacement));

        try (Response response = realm.admin().partialImport(partialImport)) {
            Assertions.assertEquals(200, response.getStatus(),
                    "partial import should succeed in test setup");
            PartialImportResults results = response.readEntity(PartialImportResults.class);
            Assertions.assertEquals(1, results.getOverwritten(),
                    "the existing user should have been overwritten, not added");
        }

        Assertions.assertNull(pushes.poll(NO_PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "an OVERWRITE partial import deletes and recreates the user -- "
                        + "the account was never purged, so no SET may be emitted");
    }

    @Test
    public void clientDeleteWithServiceAccount_noPurgeEvent() throws Exception {
        // Deleting a client removes its service-account user, but reports the deletion
        // as ResourceType.CLIENT at clients/{id}. Service accounts are not human
        // subjects and must never produce a purge SET.
        ClientRepresentation throwaway = ClientBuilder.create("purge-service-account-client")
                .secret("throwaway-secret")
                .serviceAccountsEnabled(true)
                .publicClient(false)
                .build();

        String clientUuid;
        try (Response response = realm.admin().clients().create(throwaway)) {
            Assertions.assertEquals(201, response.getStatus(),
                    "throwaway client creation should succeed in test setup");
            clientUuid = ApiUtil.getCreatedId(response);
        }

        realm.admin().clients().get(clientUuid).remove();

        Assertions.assertNull(pushes.poll(NO_PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "removing a client deletes its service-account user, but that is not an "
                        + "account purge and must not be emitted");
    }

    // --- triggers -----------------------------------------------------------

    protected String createUser(String username, String email) {
        return createUser(username, email, Map.of());
    }

    protected String createUser(String username, String email, Map<String, List<String>> attributes) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName("Purge");
        user.setLastName("Tester");
        user.setEnabled(true);
        if (!attributes.isEmpty()) {
            user.setAttributes(attributes);
        }

        try (Response response = realm.admin().users().create(user)) {
            Assertions.assertEquals(201, response.getStatus(),
                    () -> "user creation should succeed in test setup for " + username);
            return ApiUtil.getCreatedId(response);
        }
    }

    /**
     * Creates an organization, optionally carrying
     * {@code ssf.notify.<RECEIVER>=true}, and returns its alias.
     */
    protected String createOrg(boolean notify) {
        return createOrg(notify ? "true" : null);
    }

    /** Creates an organization carrying {@code ssf.notify.<RECEIVER>=false}. */
    protected String createOrgExcluded() {
        return createOrg("false");
    }

    protected String createOrg(String notifyValue) {
        String orgAlias = "purge-org-" + System.nanoTime();
        OrganizationRepresentation rep = new OrganizationRepresentation();
        rep.setName(orgAlias);
        rep.setAlias(orgAlias);
        rep.addDomain(new OrganizationDomainRepresentation(orgAlias + ".local.test"));
        if (notifyValue != null) {
            rep.singleAttribute("ssf.notify." + RECEIVER, notifyValue);
        }
        try (Response response = realm.admin().organizations().create(rep)) {
            Assertions.assertEquals(201, response.getStatus(),
                    "test organization creation should succeed");
        }
        return orgAlias;
    }

    protected void addUserToOrg(String orgAlias, String userId) {
        String orgId = realm.admin().organizations().getAll().stream()
                .filter(o -> orgAlias.equals(o.getAlias()))
                .map(OrganizationRepresentation::getId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected org with alias '" + orgAlias + "' to exist"));
        realm.admin().organizations().get(orgId).members().addMember(userId).close();
    }

    protected void bestEffortDeleteAllOrganizations() {
        try {
            realm.admin().organizations().getAll().forEach(o ->
                    realm.admin().organizations().get(o.getId()).delete().close());
        } catch (Exception ignored) {
        }
    }

    /**
     * Subscribes a user to the receiver's stream via
     * {@code POST /admin/realms/{realm}/ssf/clients/{clientId}/subjects/add},
     * which writes {@code ssf.notify.<clientId>=true} server-side.
     */
    protected void addSubject(String userId) throws IOException {
        String url = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + RECEIVER + "/subjects/add";
        try (SimpleHttpResponse response = http.doPost(url)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .json(Map.of("type", "user-id", "value", userId))
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus(),
                    () -> "subjects/add should succeed in test setup; body=" + safeBody(response));
        }
    }

    protected void deleteUser(String userId) {
        try (Response response = realm.admin().users().delete(userId)) {
            Assertions.assertEquals(204, response.getStatus(),
                    "user deletion should succeed");
        }
    }

    // --- setup --------------------------------------------------------------

    protected void setReceiverAttributes(Map<String, String> attributes) {
        ClientResource clientResource = realm.admin().clients().get(findClientByClientId(RECEIVER).getId());
        ClientRepresentation rep = clientResource.toRepresentation();
        rep.getAttributes().putAll(attributes);
        clientResource.update(rep);
    }

    protected void createPushStream(Set<String> eventsRequested) throws IOException {
        StreamDeliveryConfig delivery = new StreamDeliveryConfig();
        delivery.setMethod(Ssf.DELIVERY_METHOD_PUSH_URI);
        delivery.setEndpointUrl(MOCK_PUSH_ENDPOINT);
        delivery.setAuthorizationHeader(PUSH_AUTH_HEADER);

        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setDelivery(delivery);
        streamConfig.setEventsRequested(eventsRequested);
        streamConfig.setDescription("Account purged event integration test");

        String token = obtainReceiverManageToken();
        try (SimpleHttpResponse response = http.doPost(SsfTransmitterUrls.getStreamsEndpointUrl(realm.getBaseUrl()))
                .json(streamConfig)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(201, response.getStatus(),
                    () -> "stream creation should succeed in test setup; body=" + safeBody(response));
        }
    }

    protected String obtainReceiverManageToken() throws IOException {
        String tokenUrl = realm.getBaseUrl() + "/protocol/openid-connect/token";
        try (SimpleHttpResponse response = http.doPost(tokenUrl)
                .authBasic(RECEIVER, RECEIVER_SECRET)
                .param("grant_type", "client_credentials")
                .param("scope", SsfScopes.SCOPE_SSF_MANAGE + " " + SsfScopes.SCOPE_SSF_READ)
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus(),
                    "client_credentials grant should succeed for the receiver");
            return response.asJson().get("access_token").asText();
        }
    }

    // --- helpers ------------------------------------------------------------

    protected String awaitPush(String message) throws InterruptedException {
        String push = pushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNotNull(push, message);
        return push;
    }

    protected JsonNode decodeSet(String encoded) throws Exception {
        JWSInput jws = new JWSInput(encoded);
        return JsonSerialization.readValue(jws.getContent(), JsonNode.class);
    }

    protected void assignOptionalClientScopes(String clientId, String... scopeNames) {
        ClientRepresentation client = findClientByClientId(clientId);
        Assertions.assertNotNull(client, () -> "expected client '" + clientId + "' to exist");
        ClientResource clientResource = realm.admin().clients().get(client.getId());

        Set<String> alreadyAssigned = clientResource.getOptionalClientScopes().stream()
                .map(ClientScopeRepresentation::getName)
                .collect(Collectors.toSet());

        List<ClientScopeRepresentation> allScopes = realm.admin().clientScopes().findAll();
        for (String scopeName : scopeNames) {
            if (alreadyAssigned.contains(scopeName)) {
                continue;
            }
            ClientScopeRepresentation scope = allScopes.stream()
                    .filter(s -> scopeName.equals(s.getName()))
                    .findFirst()
                    .orElse(null);
            Assertions.assertNotNull(scope,
                    () -> "expected realm scope '" + scopeName + "' to exist");
            clientResource.addOptionalClientScope(scope.getId());
        }
    }

    protected ClientRepresentation findClientByClientId(String clientId) {
        List<ClientRepresentation> clients = realm.admin().clients().findByClientId(clientId);
        if (clients.isEmpty()) {
            return null;
        }
        return clients.get(0);
    }

    protected void bestEffortDeleteStream() {
        ClientRepresentation client = findClientByClientId(RECEIVER);
        if (client == null) {
            return;
        }
        String adminStreamUrl = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + client.getClientId() + "/stream";
        try (SimpleHttpResponse ignored = http.doDelete(adminStreamUrl)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .asResponse()) {
            // 204 / 404 both fine
        } catch (IOException e) {
            // best-effort
        }
    }

    private String safeBody(SimpleHttpResponse response) {
        try {
            return response.asString();
        } catch (Exception e) {
            return "<no body: " + e.getMessage() + ">";
        }
    }

    // --- config -------------------------------------------------------------

    public static class AccountPurgedServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            // ORGANIZATION is Type.DEFAULT and cannot be passed as an explicit
            // --features toggle, so the +tenant and org-notify tests rely on it
            // being enabled by default; only the realm-level switch is needed.
            config.features(Profile.Feature.SSF);
            config.log().categoryLevel("org.keycloak.ssf", "DEBUG");
            config.spiOption("ssf-transmitter", "default",
                    DefaultSsfTransmitterProviderFactory.CONFIG_OUTBOX_DRAINER_INTERVAL, "500ms");
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_MIN_VERIFICATION_INTERVAL_SECONDS, "0");
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_ALLOW_INSECURE_PUSH_TARGETS, "true");
            return configured;
        }
    }

    public static class AccountPurgedRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-transmitter-account-purged");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");
            realm.organizationsEnabled(true);

            realm.eventsEnabled(true);
            realm.adminEventsEnabled(true);
            realm.eventsListeners("jboss-logging", "ssf-events");

            realm.clients(
                    ClientBuilder.create(RECEIVER)
                            .secret(RECEIVER_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "http://127.0.0.1:8500/*")
                            .attribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "ALL")
                            .build()
            );

            return realm;
        }
    }
}
