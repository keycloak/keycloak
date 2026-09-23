package org.keycloak.tests.admin;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.email.TokenAuthEmailAuthenticator;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakUrls;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class SMTPTokenAuthConcurrencyTest {

    private static final String SMTP_USER = "admin@localhost";
    private static final String TOKEN_PATH = "/smtp-token-endpoint";
    private static final long TIMEOUT_SECONDS = 30;

    @InjectRealm(ref = "realmA", config = SmtpTokenAuthRealm.class)
    ManagedRealm realmA;

    @InjectRealm(ref = "realmB", config = SmtpTokenAuthRealm.class)
    ManagedRealm realmB;

    @InjectAdminClient(ref = "adminA", realmRef = "realmA", mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak adminA;

    @InjectAdminClient(ref = "adminB", realmRef = "realmB", mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak adminB;

    @InjectAdminClient(ref = "adminB2", realmRef = "realmB", mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak adminB2;

    @InjectRunOnServer(realmRef = "realmB")
    RunOnServerClient runOnServer;

    @InjectMailServer
    MailServer mailServer;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    // A dedicated server: the shared test HTTP server has a single thread, which the held endpoint would occupy.
    private HttpServer tokenServer;
    private ExecutorService tokenServerExecutor;

    @BeforeEach
    public void startTokenServer() throws IOException {
        tokenServerExecutor = Executors.newCachedThreadPool();
        tokenServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        tokenServer.setExecutor(tokenServerExecutor);
        tokenServer.start();
    }

    @AfterEach
    public void stopTokenServer() {
        tokenServer.stop(0);
        tokenServerExecutor.shutdownNow();
    }

    @Test
    public void slowTokenEndpointInOneRealmDoesNotBlockOtherRealms() throws Exception {
        mailServer.credentials(SMTP_USER, token -> true);
        HeldTokenEndpoint slowEndpoint = new HeldTokenEndpoint("{\"error\":\"temporarily_unavailable\"}");
        tokenServer.createContext(TOKEN_PATH, slowEndpoint);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Response> realmAResponse = executor.submit(() -> testSmtpConnection(adminA, realmA, heldTokenUrl()));
            assertTrue(slowEndpoint.awaitRequest(), "The token request of realm A did not reach the token endpoint");

            Future<Response> realmBResponse = executor.submit(() -> testSmtpConnection(adminB, realmB, keycloakUrls.getToken(realmB.getName())));
            assertStatus(awaitResponse(realmBResponse, "Realm B was blocked by the pending token request of realm A"), 204);
            assertFalse(realmAResponse.isDone(), "The token request of realm A should still be pending");

            slowEndpoint.release();
            assertStatus(awaitResponse(realmAResponse, "Realm A did not complete after its token endpoint responded"), 500);
        } finally {
            slowEndpoint.release();
            executor.shutdownNow();
        }
    }

    @Test
    public void concurrentRequestsInOneRealmFetchASingleToken() throws Exception {
        mailServer.credentials(SMTP_USER, token -> true);
        HeldTokenEndpoint slowEndpoint = new HeldTokenEndpoint("{\"access_token\":\"test-token\",\"expires_in\":300}");
        tokenServer.createContext(TOKEN_PATH, slowEndpoint);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Response> first = executor.submit(() -> testSmtpConnection(adminB, realmB, heldTokenUrl()));
            assertTrue(slowEndpoint.awaitRequest(), "The first token request did not reach the token endpoint");

            Future<Response> second = executor.submit(() -> testSmtpConnection(adminB2, realmB, heldTokenUrl()));
            awaitRequestBlockedInTokenRefresh();
            slowEndpoint.release();

            assertStatus(awaitResponse(first, "The first request did not complete"), 204);
            assertStatus(awaitResponse(second, "The second request did not complete"), 204);
            assertEquals(1, slowEndpoint.requestCount(), "Concurrent requests for the same realm should reuse one token");
        } finally {
            slowEndpoint.release();
            executor.shutdownNow();
        }
    }

    @Test
    public void lateRejectionOfOldTokenKeepsTheRefreshedToken() throws Exception {
        IssuingTokenEndpoint endpoint = new IssuingTokenEndpoint();
        tokenServer.createContext(TOKEN_PATH, endpoint);
        mailServer.credentials(SMTP_USER, token -> true);
        assertStatus(testSmtpConnection(adminB, realmB, heldTokenUrl()), 204);

        // Both requests present the cached token-1. The first rejection waits until the second request has presented
        // it too, and the second rejection waits until the first request has logged in with the refreshed token.
        CountDownLatch bothPresentedOldToken = new CountDownLatch(1);
        CountDownLatch refreshedTokenUsed = new CountDownLatch(1);
        AtomicInteger rejections = new AtomicInteger();
        mailServer.credentials(SMTP_USER, token -> {
            if (!token.equals("token-1")) {
                refreshedTokenUsed.countDown();
                return true;
            }
            try {
                if (rejections.incrementAndGet() == 1) {
                    bothPresentedOldToken.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } else {
                    bothPresentedOldToken.countDown();
                    refreshedTokenUsed.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return false;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Response> first = executor.submit(() -> testSmtpConnection(adminB, realmB, heldTokenUrl()));
            Future<Response> second = executor.submit(() -> testSmtpConnection(adminB2, realmB, heldTokenUrl()));

            assertStatus(awaitResponse(first, "The first request did not complete"), 204);
            assertStatus(awaitResponse(second, "The second request did not complete"), 204);
            assertEquals(2, endpoint.requestCount(), "A late rejection of the old token should not evict the refreshed one");
        } finally {
            executor.shutdownNow();
        }
    }

    private void awaitRequestBlockedInTokenRefresh() throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
        while (!isRequestBlockedInTokenRefresh()) {
            if (System.currentTimeMillis() > deadline) {
                fail("The second request never started waiting for the token refresh of the first one");
            }
            Thread.sleep(100);
        }
    }

    private boolean isRequestBlockedInTokenRefresh() {
        return runOnServer.fetch(session -> Arrays.stream(ManagementFactory.getThreadMXBean().dumpAllThreads(false, false))
                .anyMatch(thread -> thread.getThreadState() == Thread.State.BLOCKED
                        && thread.getStackTrace().length > 0
                        && thread.getStackTrace()[0].getClassName().equals(TokenAuthEmailAuthenticator.class.getName())
                        && thread.getStackTrace()[0].getMethodName().equals("gatherValidToken")), Boolean.class);
    }

    private String heldTokenUrl() {
        return "http://127.0.0.1:" + tokenServer.getAddress().getPort() + TOKEN_PATH;
    }

    private Response testSmtpConnection(Keycloak adminClient, ManagedRealm realm, String tokenUrl) {
        Map<String, String> config = new HashMap<>();
        config.put("host", "127.0.0.1");
        config.put("port", "3025");
        config.put("from", "auto@keycloak.org");
        config.put("auth", "true");
        config.put("authType", "token");
        config.put("user", SMTP_USER);
        config.put("authTokenUrl", tokenUrl);
        config.put("authTokenClientId", "smtp-client");
        config.put("authTokenClientSecret", "secret");
        config.put("authTokenScope", "basic");
        return adminClient.realms().realm(realm.getName()).testSMTPConnection(config);
    }

    private static Response awaitResponse(Future<Response> response, String timeoutMessage) throws Exception {
        try {
            return response.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            return fail(timeoutMessage);
        }
    }

    private static void assertStatus(Response response, int status) {
        try (response) {
            assertEquals(status, response.getStatus());
        }
    }

    /**
     * Token endpoint that holds every request until released. It streams whitespace while waiting, so the
     * connection stays active and the HTTP client's socket timeout does not end the request early.
     */
    private static final class HeldTokenEndpoint implements HttpHandler {

        private final byte[] responseBody;
        private final CountDownLatch released = new CountDownLatch(1);
        private final Semaphore arrived = new Semaphore(0);
        private final AtomicInteger requests = new AtomicInteger();

        HeldTokenEndpoint(String responseBody) {
            this.responseBody = responseBody.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            requests.incrementAndGet();
            arrived.release();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream body = exchange.getResponseBody()) {
                while (!released.await(1, TimeUnit.SECONDS)) {
                    body.write(' ');
                    body.flush();
                }
                body.write(responseBody);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        }

        boolean awaitRequest() throws InterruptedException {
            return arrived.tryAcquire(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        void release() {
            released.countDown();
        }

        int requestCount() {
            return requests.get();
        }
    }

    /**
     * Token endpoint that answers right away with a new token (token-1, token-2, ...) for every request.
     */
    private static final class IssuingTokenEndpoint implements HttpHandler {

        private final AtomicInteger requests = new AtomicInteger();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] body = ("{\"access_token\":\"token-" + requests.incrementAndGet() + "\",\"expires_in\":300}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            } finally {
                exchange.close();
            }
        }

        int requestCount() {
            return requests.get();
        }
    }

    public static class SmtpTokenAuthRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.clients(ClientBuilder.create("myclient")
                    .secret("mysecret")
                    .directAccessGrantsEnabled(true));
            realm.clients(ClientBuilder.create("smtp-client")
                    .secret("secret")
                    .serviceAccountsEnabled(true));
            realm.users(UserBuilder.create("myadmin")
                    .name("My", "Admin")
                    .email(SMTP_USER)
                    .emailVerified(true)
                    .password("myadmin")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN));
            return realm;
        }
    }
}
