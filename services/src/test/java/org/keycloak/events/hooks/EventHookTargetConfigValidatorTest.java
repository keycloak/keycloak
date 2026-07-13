package org.keycloak.events.hooks;

import java.util.Map;

import org.junit.Test;

public class EventHookTargetConfigValidatorTest {

    @Test
    public void shouldAllowAggregationTimeoutForBulkHttpTargets() {
        EventHookTargetConfigValidator.validate(
                null,
                new HttpEventHookTargetProviderFactory(),
                Map.of(
                        "url", "https://example.org/hooks/keycloak",
                        "deliveryMode", "BULK",
                        "maxEventsPerBatch", 10,
                        "aggregationTimeoutMs", 3_000));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectAggregationTimeoutOutsideBulkMode() {
        EventHookTargetConfigValidator.validate(
                null,
                new HttpEventHookTargetProviderFactory(),
                Map.of(
                        "url", "https://example.org/hooks/keycloak",
                        "deliveryMode", "SINGLE",
                        "aggregationTimeoutMs", 3_000));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectAggregationTimeoutForProvidersWithoutSupport() {
        EventHookTargetConfigValidator.validate(
                null,
                new PullEventHookTargetProviderFactory(),
                Map.of(
                        "deliveryMode", "BULK",
                        "aggregationTimeoutMs", 3_000));
    }
}
