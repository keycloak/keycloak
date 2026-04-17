package org.keycloak.events.hooks;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.EventHookTargetRepresentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EventHookTargetRepresentationUtilTest {

    @Test
    public void shouldRedactSecretsInRepresentation() {
        EventHookTargetModel target = new EventHookTargetModel();
        target.setId("target-1");
        target.setName("HTTP target");
        target.setType(HttpEventHookTargetProviderFactory.ID);
        target.setEnabled(true);
        target.setCreatedAt(11L);
        target.setUpdatedAt(22L);
        target.setSettings(Map.of(
                "url", "https://example.org/hooks/keycloak",
                "hmacSecret", "super-secret"
        ));

        EventHookTargetRepresentation representation = EventHookTargetRepresentationUtil.toRepresentation(
                session(new HttpEventHookTargetProviderFactory()), target, true);

        assertEquals(EventHookTargetProviderFactory.REDACTED_SECRET_VALUE, representation.getSettings().get("hmacSecret"));
        assertEquals("https://example.org/hooks/keycloak", representation.getSettings().get("url"));
        assertTrue(representation.getDisplayInfo().contains("https://example.org/hooks/keycloak"));
    }

    @Test
    public void shouldExposeAutoDisableStateInRepresentation() {
        EventHookTargetModel target = new EventHookTargetModel();
        target.setId("target-auto-1");
        target.setName("HTTP target");
        target.setType(HttpEventHookTargetProviderFactory.ID);
        target.setEnabled(false);
        target.setAutoDisabledUntil(System.currentTimeMillis() + 60_000L);
        target.setAutoDisabledReason("Automatically disabled after repeated 429 responses");
        target.setSettings(Map.of("url", "https://example.org/hooks/keycloak"));

        EventHookTargetRepresentation representation = EventHookTargetRepresentationUtil.toRepresentation(
                session(new HttpEventHookTargetProviderFactory()), target, true);

        assertTrue(Boolean.TRUE.equals(representation.getAutoDisabled()));
        assertEquals(target.getAutoDisabledUntil(), representation.getAutoDisabledUntil());
        assertEquals(target.getAutoDisabledReason(), representation.getAutoDisabledReason());
    }

    @Test
    public void shouldPreserveImportedMetadataWhenRequested() {
        EventHookTargetRepresentation representation = new EventHookTargetRepresentation();
        representation.setId("target-9");
        representation.setName("Imported target");
        representation.setType(HttpEventHookTargetProviderFactory.ID);
        representation.setEnabled(true);
        representation.setCreatedAt(123L);
        representation.setUpdatedAt(456L);
        representation.setSettings(Map.of("url", "https://example.org/imported"));

        EventHookTargetModel target = EventHookTargetRepresentationUtil.toModel(
                session(new HttpEventHookTargetProviderFactory()),
                realm("realm-1"),
                representation,
                null,
                999L,
                true);

        assertEquals("target-9", target.getId());
        assertEquals(123L, target.getCreatedAt());
        assertEquals(456L, target.getUpdatedAt());
        assertEquals("realm-1", target.getRealmId());
        assertEquals("https://example.org/imported", target.getSettings().get("url"));
    }

    @Test
    public void shouldPreserveSettingsWhenProviderIsMissing() {
        EventHookTargetModel target = new EventHookTargetModel();
        target.setId("target-missing");
        target.setName("Missing target");
        target.setType("custom-missing");
        target.setEnabled(true);
        target.setSettings(Map.of("token", "secret-value"));

        EventHookTargetRepresentation representation = EventHookTargetRepresentationUtil.toRepresentation(
                session(null), target, true);

        assertEquals("secret-value", representation.getSettings().get("token"));
        assertNull(representation.getDisplayInfo());
    }

    @Test
    public void shouldImportUnknownTypeWhenExplicitlyAllowed() {
        EventHookTargetRepresentation representation = new EventHookTargetRepresentation();
        representation.setId("target-10");
        representation.setName("Imported unknown target");
        representation.setType("custom-missing");
        representation.setEnabled(true);
        representation.setSettings(Map.of("apiKey", "preserved"));

        EventHookTargetModel target = EventHookTargetRepresentationUtil.toModel(
                session(null),
                realm("realm-2"),
                representation,
                null,
                321L,
                true,
                true);

        assertEquals("custom-missing", target.getType());
        assertEquals("preserved", target.getSettings().get("apiKey"));
    }

    @Test
    public void shouldClearAutoDisableStateWhenTargetIsEnabledManually() {
        EventHookTargetRepresentation representation = new EventHookTargetRepresentation();
        representation.setName("HTTP target");
        representation.setType(HttpEventHookTargetProviderFactory.ID);
        representation.setEnabled(true);
        representation.setSettings(Map.of("url", "https://example.org/enabled"));

        EventHookTargetModel existing = new EventHookTargetModel();
        existing.setId("target-11");
        existing.setRealmId("realm-3");
        existing.setName("HTTP target");
        existing.setType(HttpEventHookTargetProviderFactory.ID);
        existing.setEnabled(false);
        existing.setCreatedAt(1L);
        existing.setUpdatedAt(2L);
        existing.setAutoDisabledUntil(123_456L);
        existing.setAutoDisabledReason("Automatically disabled after repeated 429 responses");
        existing.setConsecutive429Count(3);
        existing.setSettings(Map.of("url", "https://example.org/disabled"));

        EventHookTargetModel target = EventHookTargetRepresentationUtil.toModel(
                session(new HttpEventHookTargetProviderFactory()),
                realm("realm-3"),
                representation,
                existing,
                999L,
                false);

        assertTrue(target.isEnabled());
        assertNull(target.getAutoDisabledUntil());
        assertNull(target.getAutoDisabledReason());
        assertNull(target.getConsecutive429Count());
    }

    private KeycloakSession session(EventHookTargetProviderFactory factory) {
        KeycloakSessionFactory sessionFactory = (KeycloakSessionFactory) Proxy.newProxyInstance(
                KeycloakSessionFactory.class.getClassLoader(),
                new Class<?>[] { KeycloakSessionFactory.class },
                (proxy, method, args) -> {
                    if ("getProviderFactory".equals(method.getName())
                            && args.length == 2
                            && args[0] == EventHookTargetProvider.class
                            && factory != null
                            && factory.getId().equals(args[1])) {
                        return factory;
                    }
                    return null;
                });

        return (KeycloakSession) Proxy.newProxyInstance(
                KeycloakSession.class.getClassLoader(),
                new Class<?>[] { KeycloakSession.class },
                (proxy, method, args) -> {
                    if ("getKeycloakSessionFactory".equals(method.getName())) {
                        return sessionFactory;
                    }
                    return null;
                });
    }

    private RealmModel realm(String id) {
        return (RealmModel) Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(),
                new Class<?>[] { RealmModel.class },
                (proxy, method, args) -> {
                    if ("getId".equals(method.getName())) {
                        return id;
                    }
                    return null;
                });
    }
}
