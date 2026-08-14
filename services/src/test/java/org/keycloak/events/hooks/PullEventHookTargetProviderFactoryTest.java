package org.keycloak.events.hooks;

import java.util.List;
import java.util.Map;

import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PullEventHookTargetProviderFactoryTest {

    private final PullEventHookTargetProviderFactory factory = new PullEventHookTargetProviderFactory();

    @Test
    public void shouldExposePullSecretAndCustomBodyMappingSettings() {
        assertEquals(
                List.of("pullSecret", "customBodyMappingTemplate"),
                factory.getConfigMetadata().stream().map(ProviderConfigProperty::getName).toList());
    }

    @Test
    public void shouldSupportPullButNotPush() {
        assertTrue(factory.supportsPull());
        assertFalse(factory.supportsPush());
        assertFalse(factory.supportsRetry());
        assertTrue(factory.supportsBatch());
    }

    @Test
    public void shouldAllowMissingPullSecret() {
        factory.validateConfig(null, Map.of());
        assertFalse(factory.getConfigMetadata().get(0).isRequired());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectInvalidCustomBodyTemplateSyntax() {
        factory.validateConfig(null, Map.of(
                "customBodyMappingTemplate", "<#if event>"
        ));
    }

    @Test
    public void shouldRedactPullSecret() {
        Map<String, Object> redacted = factory.redactConfig(Map.of("pullSecret", "top-secret"));
        assertEquals(EventHookTargetProviderFactory.REDACTED_SECRET_VALUE, redacted.get("pullSecret"));
    }

    @Test
    public void shouldPreserveExistingPullSecretWhenSubmittedValueIsRedacted() {
        Map<String, Object> normalized = factory.normalizeConfig(
                Map.of("pullSecret", "top-secret"),
                Map.of("pullSecret", EventHookTargetProviderFactory.REDACTED_SECRET_VALUE)
        );

        assertEquals("top-secret", normalized.get("pullSecret"));
    }

    @Test
    public void shouldUsePlaceholdersInDisplayInfoWhenRealmOrTargetMissing() {
        assertEquals("PULL: /realms/{realm}/event-hooks/{targetId}/consume", factory.getDisplayInfo(new EventHookTargetModel()));
    }

    @Test
    public void shouldReturnReadyTestResult() {
        EventHookTargetModel target = new EventHookTargetModel();
        target.setRealmId("realm-1");
        target.setRealmName("demo");
        target.setId("target-1");

        EventHookDeliveryResult result = factory.test(null, realm("realm-1", "demo"), target);

        assertTrue(result.isSuccess());
        assertEquals("PULL_TEST_READY", result.getStatusCode());
        assertEquals("/realms/demo/event-hooks/target-1/test", result.getDetails());
    }

    @Test
    public void shouldExposeConsumeEndpointResource() {
        EventHookTargetModel target = new EventHookTargetModel();

        Object resource = factory.getTargetEndpointResource(null, null, target, "consume");
        Object testResource = factory.getTargetEndpointResource(null, null, target, "test");

        assertTrue(resource instanceof PullEventHookTargetEndpointResource);
        assertTrue(testResource instanceof PullEventHookTargetEndpointResource);
        assertEquals(null, factory.getTargetEndpointResource(null, null, target, "other"));
    }

    @Test
    public void shouldReturnWaitingDeliveryResult() throws Exception {
        EventHookTargetModel target = new EventHookTargetModel();
        target.setRealmId("realm-1");
        target.setRealmName("demo");
        target.setId("target-1");

        EventHookMessageModel message = new EventHookMessageModel();
        EventHookDeliveryResult result = factory.create(null).deliver(target, message);

        assertFalse(result.isSuccess());
        assertTrue(result.isWaiting());
        assertEquals("PULL_WAITING", result.getStatusCode());
        assertEquals("Waiting for consumption through /realms/demo/event-hooks/target-1/consume", result.getDetails());
    }

    @Test
    public void shouldReturnWaitingBatchDeliveryResult() throws Exception {
        EventHookTargetModel target = new EventHookTargetModel();
        target.setRealmId("realm-1");
        target.setRealmName("demo");
        target.setId("target-1");

        EventHookDeliveryResult result = factory.create(null).deliverBatch(target, List.of(new EventHookMessageModel(), new EventHookMessageModel()));

        assertFalse(result.isSuccess());
        assertTrue(result.isWaiting());
        assertEquals("PULL_WAITING", result.getStatusCode());
        assertEquals("Waiting for consumption through /realms/demo/event-hooks/target-1/consume", result.getDetails());
    }

    @Test
    public void shouldUsePlaceholdersInWaitingDeliveryResultWhenRealmOrTargetMissing() throws Exception {
        EventHookDeliveryResult result = factory.create(null).deliver(new EventHookTargetModel(), new EventHookMessageModel());

        assertFalse(result.isSuccess());
        assertTrue(result.isWaiting());
        assertEquals("PULL_WAITING", result.getStatusCode());
        assertEquals("Waiting for consumption through /realms/{realm}/event-hooks/{targetId}/consume", result.getDetails());
    }

    private RealmModel realm(String id, String name) {
        return (RealmModel) java.lang.reflect.Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(),
                new Class<?>[] { RealmModel.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getId" -> id;
                    case "getName" -> name;
                    default -> null;
                });
    }
}
