package org.keycloak.protocol.oid4vc.issuance.credentialoffer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DefaultCredentialOfferStorageTest {

    private static final String TEST_REALM_ID = "test-realm-uuid";
    private static final String EXPECTED_CACHE_KEY_PREFIX = "oid4vc_offer:";

    private Map<String, Map<String, String>> cacheStore;
    private DefaultCredentialOfferStorage storage;
    private CredentialOfferState testOfferState;
    private String testOfferId;

    @Before
    public void setUp() {
        cacheStore = new HashMap<>();

        SingleUseObjectProvider singleUseObjects = new SingleUseObjectProvider() {
            @Override public void put(String key, long lifespanSeconds, Map<String, String> notes) { cacheStore.put(key, notes); }
            @Override public Map<String, String> get(String key) { return cacheStore.get(key); }
            @Override public Map<String, String> remove(String key) { return cacheStore.remove(key); }
            @Override public boolean replace(String key, Map<String, String> notes) { return false; }
            @Override public boolean putIfAbsent(String key, long lifespanInSeconds) { return false; }
            @Override public boolean contains(String key) { return cacheStore.containsKey(key); }
            @Override public void close() {}
        };

        RealmModel realm = proxy(RealmModel.class, (proxy, method, args) -> {
            if ("getId".equals(method.getName()) && args == null) return TEST_REALM_ID;
            if ("getName".equals(method.getName()) && args == null) return "test-realm";
            return defaultReturn(method);
        });

        KeycloakContext context = proxy(KeycloakContext.class, (proxy, method, args) -> {
            if ("getRealm".equals(method.getName()) && args == null) return realm;
            return defaultReturn(method);
        });

        KeycloakSession session = proxy(KeycloakSession.class, (proxy, method, args) -> {
            if ("getContext".equals(method.getName()) && args == null) return context;
            if ("singleUseObjects".equals(method.getName()) && args == null) return singleUseObjects;
            return defaultReturn(method);
        });

        storage = new DefaultCredentialOfferStorage(session);

        // Create a real CredentialOfferState for testing
        CredentialsOffer credOffer = new CredentialsOffer()
                .setCredentialIssuer("test-issuer")
                .setCredentialConfigurationIds(List.of("test-credential"));

        testOfferState = new CredentialOfferState(
                credOffer, "test-client", null,
                Time.currentTimeSeconds() + 100, null
        );
        testOfferId = testOfferState.getCredentialsOfferId();
    }

    @Test
    public void shouldStoreOfferWithPrefixedCacheKey() {
        storage.putOfferState(testOfferState);

        // Verify entry stored under prefixed key
        String expectedCacheKey = EXPECTED_CACHE_KEY_PREFIX + TEST_REALM_ID + ":" + testOfferId;
        Map<String, String> storedEntry = cacheStore.get(expectedCacheKey);
        assertNotNull("Offer state should be stored with a prefixed cache key", storedEntry);

        // Verify bare offerId key does NOT exist
        assertNull("Offer state should NOT be stored under bare offer ID", cacheStore.get(testOfferId));

        // Verify retrieval works through public API
        CredentialOfferState retrieved = storage.getOfferStateById(testOfferId);
        assertNotNull("Should retrieve offer state by ID", retrieved);
        assertEquals(testOfferId, retrieved.getCredentialsOfferId());
    }

    @Test
    public void shouldRetrieveOfferStateByNonce() {
        storage.putOfferState(testOfferState);

        CredentialOfferState retrieved = storage.getOfferStateByNonce(testOfferState.getNonce());
        assertNotNull("Should retrieve offer state by nonce", retrieved);
        assertEquals(testOfferId, retrieved.getCredentialsOfferId());
        assertEquals(testOfferState.getNonce(), retrieved.getNonce());
    }

    @Test
    public void shouldRemoveOfferStateUnderPrefixedKey() {
        storage.putOfferState(testOfferState);

        String expectedCacheKey = EXPECTED_CACHE_KEY_PREFIX + TEST_REALM_ID + ":" + testOfferId;
        assertNotNull("Offer should be stored before removal", cacheStore.get(expectedCacheKey));

        storage.removeOfferState(testOfferState);

        assertNull("Offer should be removed from prefixed cache key", cacheStore.get(expectedCacheKey));
    }

    @Test
    public void shouldReturnNullForUnknownId() {
        assertNull(storage.getOfferStateById("non-existent-id"));
    }

    @Test
    public void shouldReturnNullForUnknownNonce() {
        assertNull(storage.getOfferStateByNonce("non-existent-nonce"));
    }

    @Test
    public void shouldNotStoreExpiredOffer() {
        CredentialsOffer credOffer = new CredentialsOffer()
                .setCredentialIssuer("test-issuer")
                .setCredentialConfigurationIds(List.of("test-credential"));

        CredentialOfferState expiredOffer = new CredentialOfferState(
                credOffer, "test-client", null,
                Time.currentTimeSeconds() - 100, null  // already expired
        );

        storage.putOfferState(expiredOffer);

        String expectedCacheKey = EXPECTED_CACHE_KEY_PREFIX + TEST_REALM_ID + ":" + expiredOffer.getCredentialsOfferId();
        assertNull("Expired offer should not be stored", cacheStore.get(expectedCacheKey));
    }

    // Proxy helper for implementing interfaces without multiple abstract methods
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> interfaceClass, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                handler);
    }

    private static Object defaultReturn(Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType == boolean.class || returnType == Boolean.class) return false;
        if (returnType == int.class || returnType == Integer.class) return 0;
        if (returnType == long.class || returnType == Long.class) return 0L;
        if (returnType == void.class) return null;
        if (returnType == String.class) return "";
        return null;
    }
}
