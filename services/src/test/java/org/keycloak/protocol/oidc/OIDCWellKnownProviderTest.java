package org.keycloak.protocol.oidc;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsProcessor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OIDCWellKnownProviderTest {

    @Test
    public void advertisesEveryTypeOfMultiTypeProcessorOnce() {
        List<String> types = typesSupported(
                processor(true, "type_a", "type_b"),
                processor(true, "type_c"));

        assertEquals(List.of("type_a", "type_b", "type_c"), sorted(types));
    }

    @Test
    public void advertisesCollidingTypeOnce() {
        List<String> types = typesSupported(
                processor(true, "shared", "low_only"),
                processor(true, "shared", "high_only"));

        assertEquals(List.of("high_only", "low_only", "shared"), sorted(types));
    }

    @Test
    public void omitsTypesOfUnsupportedProcessors() {
        List<String> types = typesSupported(
                processor(false, "shared", "disabled_only"),
                processor(true, "shared", "enabled_only"));

        assertEquals(List.of("enabled_only", "shared"), sorted(types));
    }

    // Helpers ---------------------------------------------------------------------------------------------------------

    private static List<String> typesSupported(AuthorizationDetailsProcessor<?>... processors) {
        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getAllProviders(AuthorizationDetailsProcessor.class))
                .thenReturn(new LinkedHashSet<>(Arrays.asList(processors)));
        return new OIDCWellKnownProvider(session, null, false).getAuthorizationDetailsTypesSupported();
    }

    private static AuthorizationDetailsProcessor<?> processor(boolean supported, String... types) {
        AuthorizationDetailsProcessor<?> processor = mock(AuthorizationDetailsProcessor.class);
        when(processor.isSupported()).thenReturn(supported);
        when(processor.getSupportedTypes()).thenReturn(Set.of(types));
        return processor;
    }

    /**
     * Sorted copy of the given list. The order in which processors are visited is not significant, but duplicates are,
     * so the list is sorted rather than converted to a set.
     */
    private static List<String> sorted(List<String> types) {
        return types.stream().sorted().collect(Collectors.toList());
    }
}
