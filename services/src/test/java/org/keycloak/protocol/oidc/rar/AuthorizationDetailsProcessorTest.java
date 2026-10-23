package org.keycloak.protocol.oidc.rar;

import java.util.List;
import java.util.Set;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.util.AuthorizationDetailsParser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests the default methods of {@link AuthorizationDetailsProcessor}, in particular the backwards compatible
 * defaults between {@link AuthorizationDetailsProcessor#getSupportedType()} and {@link AuthorizationDetailsProcessor#getSupportedTypes()}.
 */
public class AuthorizationDetailsProcessorTest {

    /**
     * Only implements the abstract methods that are unrelated to type support, so subclasses can exercise the defaults.
     */
    private abstract static class BaseProcessor implements AuthorizationDetailsProcessor<AuthorizationDetailsJSONRepresentation> {

        @Override
        public boolean isSupported() {
            return true;
        }

        @Override
        public Class<AuthorizationDetailsJSONRepresentation> getSupportedResponseJavaType() {
            return AuthorizationDetailsJSONRepresentation.class;
        }

        @Override
        public AuthorizationDetailsJSONRepresentation validateAuthorizationDetail(AuthorizationDetailsJSONRepresentation authzDetail) {
            return authzDetail;
        }

        @Override
        public AuthorizationDetailsJSONRepresentation process(UserSessionModel userSession, ClientSessionContext clientSessionCtx, AuthorizationDetailsJSONRepresentation authzDetail) {
            return authzDetail;
        }

        @Override
        public List<AuthorizationDetailsJSONRepresentation> handleMissingAuthorizationDetails(UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
            return null;
        }

        @Override
        public AuthorizationDetailsJSONRepresentation processStoredAuthorizationDetails(UserSessionModel userSession, ClientSessionContext clientSessionCtx, AuthorizationDetailsJSONRepresentation storedAuthDetailsMember) {
            return storedAuthDetailsMember;
        }

        @Override
        public void afterAuthorizationDetailsProcessed(UserSessionModel userSession, ClientSessionContext clientSessionCtx, AuthorizationDetailsJSONRepresentation authorizationDetailsResponse) {
        }

        @Override
        public void close() {
        }
    }

    /**
     * Processor as implemented before {@link AuthorizationDetailsProcessor#getSupportedTypes()} was introduced.
     */
    private static class LegacyProcessor extends BaseProcessor {

        @Override
        @SuppressWarnings("deprecation")
        public String getSupportedType() {
            return "legacy_type";
        }
    }

    /**
     * Processor supporting multiple types and doing its own narrowing.
     */
    private static class MultiTypeProcessor extends BaseProcessor {

        @Override
        public Set<String> getSupportedTypes() {
            return Set.of("type_a", "type_b");
        }

        @Override
        public AuthorizationDetailsJSONRepresentation narrowRepresentation(AuthorizationDetailsJSONRepresentation authzDetail) {
            return authzDetail;
        }
    }

    /**
     * Processor relying on the default {@link AuthorizationDetailsProcessor#narrowRepresentation} and hence on the global parser registry.
     */
    private static class RegistryProcessor extends BaseProcessor {

        @Override
        public Set<String> getSupportedTypes() {
            return Set.of("registered_type");
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void legacyProcessorSupportedTypesDerivedFromSupportedType() {
        LegacyProcessor processor = new LegacyProcessor();

        assertEquals("legacy_type", processor.getSupportedType());
        assertEquals(Set.of("legacy_type"), processor.getSupportedTypes());
        assertTrue(processor.isSupportedType("legacy_type"));
        assertFalse(processor.isSupportedType("other"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void multiTypeProcessorSupportedTypeDerivedFromSupportedTypes() {
        MultiTypeProcessor processor = new MultiTypeProcessor();

        assertTrue(processor.getSupportedTypes().contains(processor.getSupportedType()));
        assertTrue(processor.isSupportedType("type_a"));
        assertTrue(processor.isSupportedType("type_b"));
        assertFalse(processor.isSupportedType("type_c"));
    }

    @Test
    public void getSupportedAuthorizationDetailsFiltersByAllSupportedTypes() {
        List<AuthorizationDetailsJSONRepresentation> details = List.of(detail("type_a"), detail("type_c"), detail("type_b"));

        List<AuthorizationDetailsJSONRepresentation> supported = new MultiTypeProcessor().getSupportedAuthorizationDetails(details);

        assertEquals(List.of(details.get(0), details.get(2)), supported);
    }

    @Test
    public void getSupportedAuthorizationDetailsReturnsNullForNullInput() {
        assertNull(new MultiTypeProcessor().getSupportedAuthorizationDetails(null));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void defaultNarrowRepresentationDelegatesToRegisteredParser() {
        AuthorizationDetailsJSONRepresentation parsed = new AuthorizationDetailsJSONRepresentation();
        AuthorizationDetailsParser.registerParser("registered_type", new AuthorizationDetailsParser() {
            @Override
            public <T extends AuthorizationDetailsJSONRepresentation> T asSubtype(AuthorizationDetailsJSONRepresentation authzDetail, Class<T> clazz) {
                return clazz.cast(parsed);
            }
        });

        assertSame(parsed, new RegistryProcessor().narrowRepresentation(detail("registered_type")));
    }

    private static AuthorizationDetailsJSONRepresentation detail(String type) {
        AuthorizationDetailsJSONRepresentation detail = new AuthorizationDetailsJSONRepresentation();
        detail.setType(type);
        return detail;
    }
}
