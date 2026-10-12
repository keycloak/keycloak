package org.keycloak.tests.oauth;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.protocol.oidc.grants.device.DeviceGrantType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.oauth.device.DeviceAuthorizationResponse;

import org.apache.http.Header;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@KeycloakIntegrationTest(
        config = DeviceUserCodeRateLimitTest.ProxyHeadersServerConfig.class)
public class DeviceUserCodeRateLimitTest {

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int WINDOW_SECONDS = 20;
    private static final int SHORT_CODE_LIFESPAN_SECONDS = 5;
    private static final int CLIENT_LIFESPAN_OVERRIDE_SECONDS = 300;
    private static final int CODE_RETENTION_PADDING_SECONDS = 120;

    // Comfortably more requests than the budget, so that an implementation charging the failure only after the
    // lookup lets the surplus through instead of blocking it.
    private static final int CONCURRENT_ATTEMPTS = MAX_FAILED_ATTEMPTS + 5;
    private static final int CONCURRENT_TIMEOUT_SECONDS = 30;
    private static final long EVENT_COLLECTION_TIMEOUT_MILLIS = 10_000L;
    private static final long EVENT_COLLECTION_POLL_MILLIS = 100L;

    // A blocked attempt is deliberately indistinguishable from an invalid one in the
    // response and the event error code, so the two are told apart by the event reason.
    private static final String REASON_RATE_LIMITED = "rate limit exceeded for source IP";
    private static final String REASON_CODE_NOT_FOUND = "device code not found (it may already have been used)";
    private static final String REASON_CODE_EXPIRED = "device code expired";

    // One source address per test, so a budget exhausted by one cannot leak into another.
    private static final String SOURCE_ACCEPTED_BEFORE_LIMIT = "10.1.0.1";
    private static final String SOURCE_SHARED_BUDGET = "10.2.0.1";
    private static final String SOURCE_SHARED_BUDGET_OTHER = "10.2.0.2";
    private static final String SOURCE_EMPTY_FORM = "10.3.0.1";
    private static final String SOURCE_NO_RESET_ON_SUCCESS = "10.4.0.1";
    private static final String SOURCE_RECOVERS_AFTER_EXPIRY = "10.5.0.1";
    private static final String SOURCE_EXPIRED_CODE = "10.6.0.1";
    private static final String SOURCE_ADMIN_API_ROUND_TRIP = "10.7.0.1";
    private static final String SOURCE_CONCURRENT = "10.8.0.1";
    private static final String SOURCE_ZERO_REALM_LIFESPAN = "10.9.0.1";
    private static final String SOURCE_ATTRIBUTES_ONLY_UPDATE = "10.10.0.1";

    private static final String UNRELATED_ATTRIBUTE = "unrelatedRealmAttribute";

    // The default user-code provider only generates uppercase letters.
    private static final String INVALID_CODE = "WXYZ-0000";

    private static final String VERIFY_FORM_ID = "kc-user-verify-device-user-code-form";
    private static final String LOGIN_FORM_ID = "kc-form-login";
    private static final String INVALID_CODE_MESSAGE = "Invalid code, please try again.";
    private static final String EXPIRED_CODE_MESSAGE = "The code has expired.";

    @InjectRealm(config = DeviceRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient(config = DeviceClientConfig.class)
    OAuthClient oauth;

    @InjectOAuthClient(ref = "shortcode", config = ShortLivedCodeClientConfig.class)
    OAuthClient shortCodeOauth;

    @InjectRealm(ref = "zerolifespan", config = ZeroRealmLifespanConfig.class)
    ManagedRealm zeroRealmLifespanRealm;

    @InjectOAuthClient(
            ref = "zerolifespan",
            realmRef = "zerolifespan",
            config = ClientLifespanOverrideConfig.class)
    OAuthClient zeroRealmLifespanOauth;

    @InjectHttpClient(followRedirects = false)
    CloseableHttpClient httpClient;

    @InjectEvents
    Events events;

    @InjectEvents(ref = "zerolifespan", realmRef = "zerolifespan")
    Events zeroRealmLifespanEvents;

    @Test
    public void validCodeIsAcceptedBeforeLimit() {
        DeviceAuthorizationResponse device = requestDeviceAuthorization();
        String source = SOURCE_ACCEPTED_BEFORE_LIMIT;

        assertInvalid(postUserCode(device.getVerificationUri(), source, INVALID_CODE), source);

        assertAccepted(postUserCode(device.getVerificationUri(), source, device.getUserCode()), source);
    }

    @Test
    public void getAndPostShareFailureBudget() {
        DeviceAuthorizationResponse device = requestDeviceAuthorization();
        String source = SOURCE_SHARED_BUDGET;

        assertInvalid(postUserCode(device.getVerificationUri(), source, INVALID_CODE), source);
        assertInvalid(getUserCode(device.getVerificationUri(), source, INVALID_CODE), source);
        assertInvalid(postUserCode(device.getVerificationUri(), source, INVALID_CODE), source);

        // Both methods must reject a valid code after the shared budget is exhausted.
        assertRateLimited(postUserCode(device.getVerificationUri(), source, device.getUserCode()), source);
        assertRateLimited(getUserCode(device.getVerificationUri(), source, device.getUserCode()), source);

        // Blocking affects the source, not the device code.
        assertAccepted(
                postUserCode(
                        device.getVerificationUri(),
                        SOURCE_SHARED_BUDGET_OTHER,
                        device.getUserCode()),
                SOURCE_SHARED_BUDGET_OTHER);
    }

    @Test
    public void emptyFormDoesNotConsumeBudget() {
        DeviceAuthorizationResponse device = requestDeviceAuthorization();
        String source = SOURCE_EMPTY_FORM;

        for (int request = 0; request < MAX_FAILED_ATTEMPTS + 1; request++) {
            assertEmptyForm(getUserCode(device.getVerificationUri(), source, null));
            assertEmptyForm(postUserCode(device.getVerificationUri(), source, null));
        }

        assertAccepted(postUserCode(device.getVerificationUri(), source, device.getUserCode()), source);
    }

    @Test
    public void successfulVerificationDoesNotResetFailures() {
        DeviceAuthorizationResponse firstDevice = requestDeviceAuthorization();
        String source = SOURCE_NO_RESET_ON_SUCCESS;

        assertInvalid(postUserCode(firstDevice.getVerificationUri(), source, INVALID_CODE), source);
        assertAccepted(postUserCode(firstDevice.getVerificationUri(), source, firstDevice.getUserCode()), source);

        // A fresh flow avoids depending on reuse of an already verified code.
        DeviceAuthorizationResponse secondDevice = requestDeviceAuthorization();

        for (int attempt = 1; attempt < MAX_FAILED_ATTEMPTS; attempt++) {
            assertInvalid(
                    postUserCode(
                            secondDevice.getVerificationUri(), source, INVALID_CODE),
                    source);
        }

        assertRateLimited(
                postUserCode(
                        secondDevice.getVerificationUri(),
                        source,
                        secondDevice.getUserCode()),
                source);
    }

    @Test
    public void verificationRecoversAfterExpiry() throws InterruptedException {
        DeviceAuthorizationResponse device = requestDeviceAuthorization();
        String source = SOURCE_RECOVERS_AFTER_EXPIRY;

        long firstFailureAt = System.currentTimeMillis();
        exhaustFailureBudget(device.getVerificationUri(), source);

        assertRateLimited(
                postUserCode(
                        device.getVerificationUri(), source, device.getUserCode()),
                source);

        // The counter is created on the first attempt, here a failure, and expires a fixed window later.
        long remaining = (WINDOW_SECONDS + 2) * 1000L
                - (System.currentTimeMillis() - firstFailureAt);
        if (remaining > 0) {
            Thread.sleep(remaining);
        }

        // Issue a fresh code so this assertion only depends on limiter expiry.
        DeviceAuthorizationResponse freshDevice = requestDeviceAuthorization();

        assertAccepted(
                postUserCode(
                        freshDevice.getVerificationUri(),
                        source,
                        freshDevice.getUserCode()),
                source);
    }

    /**
     * Submitting a code that is real but expired is an ordinary mistake, not a guess, so it must not spend the
     * budget that everyone behind the same source address shares.
     */
    @Test
    public void expiredCodeDoesNotConsumeBudget() throws InterruptedException {
        DeviceAuthorizationResponse device = requestDeviceAuthorization(shortCodeOauth);
        String source = SOURCE_EXPIRED_CODE;

        Thread.sleep((SHORT_CODE_LIFESPAN_SECONDS + 2) * 1000L);

        for (int attempt = 0; attempt < MAX_FAILED_ATTEMPTS + 1; attempt++) {
            assertExpired(
                    postUserCode(
                            device.getVerificationUri(), source, device.getUserCode()),
                    source);
        }

        DeviceAuthorizationResponse freshDevice = requestDeviceAuthorization(shortCodeOauth);

        assertAccepted(
                postUserCode(
                        freshDevice.getVerificationUri(),
                        source,
                        freshDevice.getUserCode()),
                source);
    }

    /**
     * The budget has to hold when attempts overlap. Reading the count before the lookup and charging the failure
     * only afterwards lets every request that arrives while one attempt remains pass the check and guess, so a
     * caller with request concurrency C turns a budget of N into roughly N + C. The attempt is therefore charged
     * before the lookup and given back for a recognized code, which makes the split below exact rather than
     * timing dependent: the attempts are serialized by one atomic counter, so the first {@code MAX_FAILED_ATTEMPTS}
     * of them reach the lookup and the rest are blocked, whatever order they arrive in.
     */
    @Test
    public void concurrentAttemptsCannotExceedBudget() throws Exception {
        DeviceAuthorizationResponse device = requestDeviceAuthorization();
        String source = SOURCE_CONCURRENT;

        List<VerificationResponse> responses;

        // The injected client pools only two connections per route, which would serialize the requests and hide
        // the very overlap under test.
        try (CloseableHttpClient concurrentClient = HttpClientBuilder.create()
                .disableRedirectHandling()
                .setMaxConnPerRoute(CONCURRENT_ATTEMPTS)
                .setMaxConnTotal(CONCURRENT_ATTEMPTS)
                .build()) {
            responses = postUserCodeConcurrently(
                    concurrentClient, device.getVerificationUri(), source, INVALID_CODE);
        }

        // The overlapping attempts spent the budget exactly once, so a valid code submitted afterwards is still
        // blocked. Collected together with the batch because the counter expires with the device code lifespan.
        VerificationResponse afterBatch = postUserCode(device.getVerificationUri(), source, device.getUserCode());

        responses.forEach(this::assertRejectedPage);
        assertRejectedPage(afterBatch);

        // Every one of these requests failing as an error event is itself part of the assertion: had the valid
        // code been accepted, it would have produced a success event and the count would not add up.
        Map<String, Long> reasons = countVerificationErrorReasons(CONCURRENT_ATTEMPTS + 1, source);

        assertThat(reasons.get(REASON_CODE_NOT_FOUND), is((long) MAX_FAILED_ATTEMPTS));
        assertThat(reasons.get(REASON_RATE_LIMITED), is((long) (CONCURRENT_ATTEMPTS + 1 - MAX_FAILED_ATTEMPTS)));
    }

    /**
     * A realm device code lifespan of zero does not mean no code is alive: {@code getLifespan(client)} lets a
     * client override it, and such a client goes on issuing codes that are valid for its own lifespan. The realm
     * lifespan is only the window failures are counted over, and at verification time there is no client to ask
     * for a better one, so a zero there must fall back to the default window rather than switch the limit off
     * for those flows.
     */
    @Test
    public void clientLifespanOverrideKeepsLimitActive() {
        DeviceAuthorizationResponse device = requestDeviceAuthorization(zeroRealmLifespanOauth);
        String source = SOURCE_ZERO_REALM_LIFESPAN;
        Events realmEvents = zeroRealmLifespanEvents;

        for (int attempt = 0; attempt < MAX_FAILED_ATTEMPTS; attempt++) {
            assertInvalid(
                    realmEvents,
                    postUserCode(device.getVerificationUri(), source, INVALID_CODE),
                    source);
        }

        // The code is live, thanks to the client override, so without the limit this would be accepted and
        // redirect into the login flow.
        assertRateLimited(
                realmEvents,
                postUserCode(device.getVerificationUri(), source, device.getUserCode()),
                source);
    }

    /**
     * The limit is a first-class field of the realm representation, like the device code lifespan and polling
     * interval beside it, so it has to survive the Admin REST API in both directions. The realm is restored
     * explicitly because {@code ManagedRealmCleanup#resetToOriginalRepresentation} is not visible from here and
     * {@code updateWithCleanup} can only reach attributes, not the typed field under test.
     */
    @Test
    public void maxFailedAttemptsRoundTripsThroughAdminApi() {
        RealmRepresentation original = realm.admin().toRepresentation();
        assertThat(original.getOAuth2DeviceMaxFailedVerificationAttempts(), is(MAX_FAILED_ATTEMPTS));

        try {
            RealmRepresentation update = realm.admin().toRepresentation();
            update.setOAuth2DeviceMaxFailedVerificationAttempts(1);
            realm.admin().update(update);

            assertThat(realm.admin().toRepresentation().getOAuth2DeviceMaxFailedVerificationAttempts(), is(1));

            // The endpoint reads the realm on every request, so the lowered budget applies without a restart.
            DeviceAuthorizationResponse device = requestDeviceAuthorization();
            String source = SOURCE_ADMIN_API_ROUND_TRIP;

            assertInvalid(postUserCode(device.getVerificationUri(), source, INVALID_CODE), source);
            assertRateLimited(postUserCode(device.getVerificationUri(), source, device.getUserCode()), source);
        } finally {
            realm.admin().update(original);
        }
    }

    /**
     * A partial update carrying an attributes map that says nothing about the device flow must not take the limit
     * with it. The attribute replacement in {@code DefaultExportImportManager#updateRealm} deletes every realm
     * attribute missing from the incoming map unless it is excluded as a typed field, and a partial update has no
     * typed field to put it back, so a configured limit would silently relax to the default.
     */
    @Test
    public void attributesOnlyUpdatePreservesMaxFailedAttempts() {
        RealmRepresentation original = realm.admin().toRepresentation();
        assertThat(original.getOAuth2DeviceMaxFailedVerificationAttempts(), is(MAX_FAILED_ATTEMPTS));

        try {
            RealmRepresentation partial = new RealmRepresentation();
            partial.setAttributes(Map.of(UNRELATED_ATTRIBUTE, "value"));
            realm.admin().update(partial);

            assertThat(
                    realm.admin().toRepresentation().getOAuth2DeviceMaxFailedVerificationAttempts(),
                    is(MAX_FAILED_ATTEMPTS));

            // Still enforced, not merely still reported.
            DeviceAuthorizationResponse device = requestDeviceAuthorization();
            String source = SOURCE_ATTRIBUTES_ONLY_UPDATE;

            exhaustFailureBudget(device.getVerificationUri(), source);
            assertRateLimited(postUserCode(device.getVerificationUri(), source, device.getUserCode()), source);

            // And what kept it out of that sweep: it is carried by the typed field alone, so it was never in the
            // attributes bag for the replacement to drop. Asserted last so that a regression reports the damage
            // rather than the cause.
            assertThat(
                    original.getAttributes(),
                    not(hasKey(OAuth2DeviceConfig.OAUTH2_DEVICE_MAX_FAILED_VERIFICATION_ATTEMPTS)));
        } finally {
            // The sweep also cleared the realm device code lifespan, which the rest of this class depends on.
            realm.admin().update(original);
        }
    }

    private DeviceAuthorizationResponse requestDeviceAuthorization() {
        return requestDeviceAuthorization(oauth);
    }

    private DeviceAuthorizationResponse requestDeviceAuthorization(
            OAuthClient client) {
        DeviceAuthorizationResponse device =
                client.device().doDeviceAuthorizationRequest();

        assertThat(device.getStatusCode(), is(200));
        // The device authorization endpoint sets the OAUTH2_DEVICE_AUTH event type but never completes the event
        // on the success path, so there is nothing to poll here; only verification produces events.

        return device;
    }

    private void exhaustFailureBudget(String verificationUri, String sourceAddress) {
        for (int attempt = 0; attempt < MAX_FAILED_ATTEMPTS; attempt++) {
            assertInvalid(
                    postUserCode(verificationUri, sourceAddress, INVALID_CODE),
                    sourceAddress);
        }
    }

    /**
     * Fires {@link #CONCURRENT_ATTEMPTS} verification requests from one source address and releases them together,
     * so that they are genuinely in flight at the same time rather than merely issued in a loop.
     */
    private List<VerificationResponse> postUserCodeConcurrently(
            CloseableHttpClient client, String verificationUri, String sourceAddress, String userCode)
            throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_ATTEMPTS);
        CyclicBarrier allReady = new CyclicBarrier(CONCURRENT_ATTEMPTS);

        try {
            List<Future<VerificationResponse>> pending = new ArrayList<>(CONCURRENT_ATTEMPTS);

            for (int attempt = 0; attempt < CONCURRENT_ATTEMPTS; attempt++) {
                pending.add(executor.submit(() -> {
                    allReady.await(CONCURRENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    return postUserCode(client, verificationUri, sourceAddress, userCode);
                }));
            }

            List<VerificationResponse> responses = new ArrayList<>(CONCURRENT_ATTEMPTS);

            for (Future<VerificationResponse> response : pending) {
                responses.add(response.get(CONCURRENT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }

            return responses;
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Counts the error events of a batch of concurrent requests by reason. They cannot be matched to individual
     * responses the way {@link #assertVerificationError} does, because a blocked attempt is deliberately
     * indistinguishable from a rejected one in the response and nothing orders the events against the requests.
     */
    private Map<String, Long> countVerificationErrorReasons(int expected, String sourceAddress)
            throws InterruptedException {
        List<EventRepresentation> collected = new ArrayList<>(expected);
        long deadline = System.currentTimeMillis() + EVENT_COLLECTION_TIMEOUT_MILLIS;

        // poll() only sees events already stored when it runs, so one whose write lands a moment after the
        // response needs a further round.
        while (collected.size() < expected && System.currentTimeMillis() < deadline) {
            EventRepresentation event = events.poll();

            if (event == null) {
                Thread.sleep(EVENT_COLLECTION_POLL_MILLIS);
            } else {
                collected.add(event);
            }
        }

        assertThat(collected, hasSize(expected));

        Map<String, Long> reasons = new HashMap<>();

        for (EventRepresentation event : collected) {
            EventAssertion.assertError(event)
                    .type(EventType.OAUTH2_DEVICE_VERIFY_USER_CODE_ERROR)
                    .error(Errors.INVALID_OAUTH2_USER_CODE);
            assertThat(event.getIpAddress(), is(sourceAddress));
            reasons.merge(event.getDetails().get(Details.REASON), 1L, Long::sum);
        }

        return reasons;
    }

    private void assertAccepted(
            VerificationResponse response, String sourceAddress) {
        // Successful verification starts the browser authentication flow, which redirects rather than rendering
        // the verification form again.
        assertThat(response.status(), is(302));
        assertThat(response.location(), containsString("login-actions/authenticate"));
        assertThat(response.body(), not(containsString(VERIFY_FORM_ID)));

        EventRepresentation event = events.poll();
        EventAssertion.assertSuccess(event)
                .type(EventType.OAUTH2_DEVICE_VERIFY_USER_CODE);
        assertThat(event.getIpAddress(), is(sourceAddress));
    }

    private void assertInvalid(VerificationResponse response, String sourceAddress) {
        assertInvalid(events, response, sourceAddress);
    }

    private void assertInvalid(Events realmEvents, VerificationResponse response, String sourceAddress) {
        assertRejectedPage(response);
        assertVerificationError(realmEvents, REASON_CODE_NOT_FOUND, sourceAddress);
    }

    private void assertRateLimited(VerificationResponse response, String sourceAddress) {
        assertRateLimited(events, response, sourceAddress);
    }

    private void assertRateLimited(Events realmEvents, VerificationResponse response, String sourceAddress) {
        assertRejectedPage(response);
        assertVerificationError(realmEvents, REASON_RATE_LIMITED, sourceAddress);
    }

    private void assertExpired(VerificationResponse response, String sourceAddress) {
        assertVerificationError(REASON_CODE_EXPIRED, sourceAddress);
        assertThat(response.status(), is(200));
        assertThat(response.body(), containsString(VERIFY_FORM_ID));
        assertThat(response.body(), containsString(EXPIRED_CODE_MESSAGE));
    }

    private void assertRejectedPage(VerificationResponse response) {
        assertThat(response.status(), is(200));
        assertThat(response.body(), containsString(VERIFY_FORM_ID));
        assertThat(response.body(), containsString(INVALID_CODE_MESSAGE));
        assertThat(response.body(), not(containsString(LOGIN_FORM_ID)));
        assertThat(response.cacheControl(), containsString("no-store"));
        assertThat(response.cacheControl(), containsString("must-revalidate"));
        assertThat(response.cacheControl(), containsString("max-age=0"));
    }

    private void assertVerificationError(String reason, String sourceAddress) {
        assertVerificationError(events, reason, sourceAddress);
    }

    private void assertVerificationError(Events realmEvents, String reason, String sourceAddress) {
        EventRepresentation event = realmEvents.poll();
        EventAssertion.assertError(event)
                .type(EventType.OAUTH2_DEVICE_VERIFY_USER_CODE_ERROR)
                .error(Errors.INVALID_OAUTH2_USER_CODE)
                .details(Details.REASON, reason);
        assertThat(event.getIpAddress(), is(sourceAddress));
    }

    private void assertEmptyForm(VerificationResponse response) {
        assertThat(response.status(), is(200));
        assertThat(response.body(), containsString(VERIFY_FORM_ID));
        assertThat(response.body(), not(containsString(INVALID_CODE_MESSAGE)));
        assertThat(events.poll(), nullValue());
    }

    private VerificationResponse getUserCode(
            String verificationUri, String sourceAddress, String userCode) {
        try {
            URIBuilder uri = new URIBuilder(verificationUri);
            if (userCode != null) {
                // The GET endpoint binds "user_code"; only the POST form field is
                // named "device_user_code".
                uri.addParameter(OAuth2Constants.USER_CODE, userCode);
            }
            return execute(new HttpGet(uri.build()), sourceAddress);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid verification URI", e);
        }
    }

    private VerificationResponse postUserCode(
            String verificationUri, String sourceAddress, String userCode) {
        return postUserCode(httpClient, verificationUri, sourceAddress, userCode);
    }

    private VerificationResponse postUserCode(
            CloseableHttpClient client, String verificationUri, String sourceAddress, String userCode) {
        HttpPost post = new HttpPost(URI.create(verificationUri));

        List<BasicNameValuePair> form = userCode == null
                ? List.of()
                : List.of(new BasicNameValuePair(
                        DeviceGrantType.OAUTH2_DEVICE_USER_CODE, userCode));

        post.setEntity(new UrlEncodedFormEntity(form, StandardCharsets.UTF_8));
        return execute(client, post, sourceAddress);
    }

    private VerificationResponse execute(HttpRequestBase request, String sourceAddress) {
        return execute(httpClient, request, sourceAddress);
    }

    private VerificationResponse execute(
            CloseableHttpClient client, HttpRequestBase request, String sourceAddress) {
        request.setHeader("Accept", "text/html");
        request.setHeader("Accept-Language", "en");
        request.setHeader("X-Forwarded-For", sourceAddress);

        try (CloseableHttpResponse response = client.execute(request)) {
            Header cacheControl = response.getFirstHeader("Cache-Control");
            Header location = response.getFirstHeader("Location");
            String body = response.getEntity() == null
                    ? ""
                    : EntityUtils.toString(
                            response.getEntity(), StandardCharsets.UTF_8);

            return new VerificationResponse(
                    response.getStatusLine().getStatusCode(),
                    cacheControl == null ? null : cacheControl.getValue(),
                    location == null ? "" : location.getValue(),
                    body);
        } catch (IOException e) {
            throw new RuntimeException("Device verification request failed", e);
        }
    }

    private record VerificationResponse(int status, String cacheControl, String location, String body) {}

    public static class ProxyHeadersServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(
                KeycloakServerConfigBuilder config) {
            return config.option("proxy-headers", "xforwarded");
        }
    }

    public static class DeviceRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .bruteForceProtected(false)
                    .attribute(
                            OAuth2DeviceConfig.OAUTH2_DEVICE_MAX_FAILED_VERIFICATION_ATTEMPTS,
                            String.valueOf(MAX_FAILED_ATTEMPTS))
                    .attribute(
                            OAuth2DeviceConfig.OAUTH2_DEVICE_CODE_LIFESPAN,
                            String.valueOf(WINDOW_SECONDS));
        }
    }

    /**
     * A realm whose device code lifespan is zero, which the admin console allows, while the failure limit is on.
     */
    public static class ZeroRealmLifespanConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .bruteForceProtected(false)
                    .attribute(
                            OAuth2DeviceConfig.OAUTH2_DEVICE_MAX_FAILED_VERIFICATION_ATTEMPTS,
                            String.valueOf(MAX_FAILED_ATTEMPTS))
                    .attribute(OAuth2DeviceConfig.OAUTH2_DEVICE_CODE_LIFESPAN, "0");
        }
    }

    /**
     * A client overriding the realm lifespan, so that its codes stay valid however the realm is configured.
     */
    public static class ClientLifespanOverrideConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client
                    .clientId("device-client-lifespan-override")
                    .publicClient()
                    .attribute(
                            OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED,
                            "true")
                    .attribute(
                            OAuth2DeviceConfig.OAUTH2_DEVICE_CODE_LIFESPAN_PER_CLIENT,
                            String.valueOf(CLIENT_LIFESPAN_OVERRIDE_SECONDS));
        }
    }

    public static class ShortLivedCodeClientConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client
                    .clientId("device-client-short-code")
                    .publicClient()
                    .attribute(
                            OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED,
                            "true")
                    .attribute(
                            OAuth2DeviceConfig.OAUTH2_DEVICE_CODE_LIFESPAN_PER_CLIENT,
                            String.valueOf(SHORT_CODE_LIFESPAN_SECONDS))
                    .attribute(
                            OAuth2DeviceConfig.OAUTH2_DEVICE_POLLING_INTERVAL_PER_CLIENT,
                            String.valueOf(CODE_RETENTION_PADDING_SECONDS));
        }
    }

    public static class DeviceClientConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client
                    .clientId("device-client")
                    .publicClient()
                    .attribute(
                            OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED,
                            "true");
        }
    }
}
