package org.keycloak.ssf.transmitter.subject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.OrganizationModel;
import org.keycloak.models.UserModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SsfNotifyAttributes} attribute key derivation,
 * value semantics, and the write guards that keep the
 * {@code ssf.notify.<clientId>} / tombstone attributes from being
 * rewritten when their value would not change. The guards matter for
 * read-only user stores (e.g. LDAP edit mode {@code READ_ONLY}, or with
 * import disabled), where a redundant write would throw a
 * {@code ReadOnlyException} instead of being a harmless no-op.
 */
class SsfNotifyAttributesTest {

    private static final String CLIENT_ID = "rcv";
    private static final String NOTIFY_KEY = "ssf.notify.rcv";
    private static final String REMOVED_AT_KEY = "ssf.notifyRemovedAt.rcv";

    @Test
    void attributeKey_prefixesClientId() {
        assertEquals("ssf.notify.abc-123", SsfNotifyAttributes.attributeKey("abc-123"));
    }

    @Test
    void attributeKey_handlesUuid() {
        String clientId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
        assertEquals("ssf.notify." + clientId, SsfNotifyAttributes.attributeKey(clientId));
    }

    @Test
    void attributePrefix_isConsistent() {
        assertEquals("ssf.notify.", SsfNotifyAttributes.ATTRIBUTE_PREFIX);
    }

    @Test
    void attributeKey_handlesUrlClientId() {
        // Receiver OAuth client_ids may be URLs.
        String clientId = "https://rp.example.com";
        assertEquals("ssf.notify.https://rp.example.com",
                SsfNotifyAttributes.attributeKey(clientId));
        assertEquals("ssf.notifyRemovedAt.https://rp.example.com",
                SsfNotifyAttributes.removedAtKey(clientId));
    }

    // ----- user: setForUser -----

    @Test
    void setForUser_writesTrue_whenUnset() {
        UserModel user = mock(UserModel.class);
        when(user.getFirstAttribute(NOTIFY_KEY)).thenReturn(null);

        SsfNotifyAttributes.setForUser(user, CLIENT_ID);

        verify(user).setSingleAttribute(NOTIFY_KEY, "true");
    }

    @Test
    void setForUser_writesTrue_whenCurrentlyFalse() {
        UserModel user = mock(UserModel.class);
        when(user.getFirstAttribute(NOTIFY_KEY)).thenReturn("false");

        SsfNotifyAttributes.setForUser(user, CLIENT_ID);

        verify(user).setSingleAttribute(NOTIFY_KEY, "true");
    }

    @Test
    void setForUser_skips_whenAlreadyTrue() {
        UserModel user = mock(UserModel.class);
        when(user.getFirstAttribute(NOTIFY_KEY)).thenReturn("true");

        SsfNotifyAttributes.setForUser(user, CLIENT_ID);

        verify(user, never()).setSingleAttribute(anyString(), anyString());
    }

    // ----- user: excludeForUser -----

    @Test
    void excludeForUser_writesFalse_whenCurrentlyTrue() {
        UserModel user = mock(UserModel.class);
        when(user.getFirstAttribute(NOTIFY_KEY)).thenReturn("true");

        SsfNotifyAttributes.excludeForUser(user, CLIENT_ID);

        verify(user).setSingleAttribute(NOTIFY_KEY, "false");
    }

    @Test
    void excludeForUser_writesFalse_whenUnset() {
        // Ignoring an unset subject must write an explicit "false" so the
        // exclusion actually takes effect — e.g. to override a
        // default_subjects=ALL stream or an org-level notify=true.
        UserModel user = mock(UserModel.class);
        when(user.getFirstAttribute(NOTIFY_KEY)).thenReturn(null);

        SsfNotifyAttributes.excludeForUser(user, CLIENT_ID);

        verify(user).setSingleAttribute(NOTIFY_KEY, "false");
    }

    @Test
    void excludeForUser_skips_whenAlreadyFalse() {
        UserModel user = mock(UserModel.class);
        when(user.getFirstAttribute(NOTIFY_KEY)).thenReturn("false");

        SsfNotifyAttributes.excludeForUser(user, CLIENT_ID);

        verify(user, never()).setSingleAttribute(anyString(), anyString());
    }

    // ----- user: clearForUser / clearRemovedAtForUser -----

    @Test
    void clearForUser_removes_whenSet() {
        UserModel user = mock(UserModel.class);
        when(user.getFirstAttribute(NOTIFY_KEY)).thenReturn("true");

        SsfNotifyAttributes.clearForUser(user, CLIENT_ID);

        verify(user).removeAttribute(NOTIFY_KEY);
    }

    @Test
    void clearForUser_skips_whenAbsent() {
        UserModel user = mock(UserModel.class);
        when(user.getFirstAttribute(NOTIFY_KEY)).thenReturn(null);

        SsfNotifyAttributes.clearForUser(user, CLIENT_ID);

        verify(user, never()).removeAttribute(anyString());
    }

    @Test
    void clearRemovedAtForUser_removes_whenSet() {
        UserModel user = mock(UserModel.class);
        when(user.getFirstAttribute(REMOVED_AT_KEY)).thenReturn("123");

        SsfNotifyAttributes.clearRemovedAtForUser(user, CLIENT_ID);

        verify(user).removeAttribute(REMOVED_AT_KEY);
    }

    @Test
    void clearRemovedAtForUser_skips_whenAbsent() {
        UserModel user = mock(UserModel.class);
        when(user.getFirstAttribute(REMOVED_AT_KEY)).thenReturn(null);

        SsfNotifyAttributes.clearRemovedAtForUser(user, CLIENT_ID);

        verify(user, never()).removeAttribute(anyString());
    }

    // ----- organization: setForOrganization -----

    @Test
    void setForOrganization_writesTrue_whenUnset() {
        OrganizationModel org = mock(OrganizationModel.class);
        when(org.getAttributes()).thenReturn(new HashMap<>());

        SsfNotifyAttributes.setForOrganization(org, CLIENT_ID);

        verify(org).setAttributes(Map.of(NOTIFY_KEY, List.of("true")));
    }

    @Test
    void setForOrganization_skips_whenAlreadyNotified() {
        OrganizationModel org = mock(OrganizationModel.class);
        when(org.getAttributes()).thenReturn(Map.of(NOTIFY_KEY, List.of("true")));

        SsfNotifyAttributes.setForOrganization(org, CLIENT_ID);

        verify(org, never()).setAttributes(anyMap());
    }

    // ----- organization: excludeForOrganization -----

    @Test
    void excludeForOrganization_writesFalse_whenUnset() {
        // Org exclude differs from user exclude: it writes an explicit
        // "false" even when unset, so a broadcast (default_subjects=ALL)
        // org can be opted out.
        OrganizationModel org = mock(OrganizationModel.class);
        when(org.getAttributes()).thenReturn(new HashMap<>());

        SsfNotifyAttributes.excludeForOrganization(org, CLIENT_ID);

        verify(org).setAttributes(Map.of(NOTIFY_KEY, List.of("false")));
    }

    @Test
    void excludeForOrganization_skips_whenAlreadyExcluded() {
        OrganizationModel org = mock(OrganizationModel.class);
        when(org.getAttributes()).thenReturn(Map.of(NOTIFY_KEY, List.of("false")));

        SsfNotifyAttributes.excludeForOrganization(org, CLIENT_ID);

        verify(org, never()).setAttributes(anyMap());
    }

    // ----- organization: clearForOrganization / clearRemovedAtForOrganization -----

    @Test
    void clearForOrganization_removes_whenPresent_andKeepsOtherAttributes() {
        OrganizationModel org = mock(OrganizationModel.class);
        Map<String, List<String>> attrs = new HashMap<>();
        attrs.put(NOTIFY_KEY, List.of("true"));
        attrs.put("unrelated", List.of("keep"));
        when(org.getAttributes()).thenReturn(attrs);

        SsfNotifyAttributes.clearForOrganization(org, CLIENT_ID);

        verify(org).setAttributes(Map.of("unrelated", List.of("keep")));
    }

    @Test
    void clearForOrganization_skips_whenAbsent() {
        OrganizationModel org = mock(OrganizationModel.class);
        when(org.getAttributes()).thenReturn(new HashMap<>());

        SsfNotifyAttributes.clearForOrganization(org, CLIENT_ID);

        verify(org, never()).setAttributes(anyMap());
    }

    @Test
    void clearRemovedAtForOrganization_skips_whenAbsent() {
        OrganizationModel org = mock(OrganizationModel.class);
        when(org.getAttributes()).thenReturn(new HashMap<>());

        SsfNotifyAttributes.clearRemovedAtForOrganization(org, CLIENT_ID);

        verify(org, never()).setAttributes(anyMap());
    }
}
