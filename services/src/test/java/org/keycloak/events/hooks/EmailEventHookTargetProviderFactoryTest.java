package org.keycloak.events.hooks;

import java.util.List;
import java.util.Map;

import org.keycloak.provider.ProviderConfigProperty;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EmailEventHookTargetProviderFactoryTest {

    private final EmailEventHookTargetProviderFactory factory = new EmailEventHookTargetProviderFactory();

    @Test
    public void shouldExposeAllEmailSettings() {
        assertEquals(
                List.of(
                        "recipientTemplate",
                        "localeTemplate",
                        "subjectTemplate",
                        "textBodyTemplate",
                        "htmlBodyTemplate"),
                factory.getConfigMetadata().stream().map(ProviderConfigProperty::getName).toList());
    }

    @Test
    public void shouldSupportBatchAndAggregation() {
        assertTrue(factory.supportsBatch());
        assertTrue(factory.supportsAggregation());
    }

    @Test
    public void shouldAcceptValidEmailSettings() {
        factory.validateConfig(null, Map.of(
                "recipientTemplate", "ops@example.org",
                "subjectTemplate", "${msg(\"emailTestSubject\")}",
                "textBodyTemplate", "Event: ${eventId}"
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectMissingRecipientTemplate() {
        factory.validateConfig(null, Map.of(
                "subjectTemplate", "Test",
                "textBodyTemplate", "Body"
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectMissingBodyTemplates() {
        factory.validateConfig(null, Map.of(
                "recipientTemplate", "ops@example.org",
                "subjectTemplate", "Subject"
        ));
    }
}
