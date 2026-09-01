package org.keycloak.ssf.transmitter.subject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.common.profile.PropertiesProfileConfigResolver;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.ssf.subject.ComplexSubjectId;
import org.keycloak.ssf.subject.EmailSubjectId;
import org.keycloak.ssf.subject.IssuerSubjectId;
import org.keycloak.ssf.subject.OpaqueSubjectId;
import org.keycloak.storage.UserStorageProvider;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for resolving a captured snapshot back from a subject identifier.
 *
 * <p>Focused on the issuer gate: an {@code iss_sub} subject only names a Keycloak
 * user id when this transmitter issued it, because {@code SubjectUserLookup} reads
 * {@code sub} as an external IdP subject for any other issuer. The filter consults
 * the snapshot before the live lookup for purge events, so the two have to agree on
 * what {@code sub} means.
 *
 * <p>The realm here carries a {@code frontendUrl} attribute, which makes the
 * transmitter's issuer differ from the realm issuer — the configuration that would
 * break if the gate were written against {@code SubjectUserLookup.isRealmIssuer}
 * instead of the transmitter's own issuer.
 */
class PurgedUserSnapshotTest {

    static final String REALM_ID = "realm-1";
    static final String USER_ID = "1c9a1a0e-0000-4000-8000-000000000001";
    static final String EMAIL = "purged@local.test";

    /** What SsfUtil.getIssuerUrl returns for this realm: the frontendUrl attribute, verbatim. */
    static final String TRANSMITTER_ISSUER = "https://ssf.example/auth/realms/test";

    static final String FOREIGN_ISSUER = "https://idp.partner.example";

    @BeforeAll
    static void initProfile() {
        Profile.configure(new PropertiesProfileConfigResolver(new Properties()));
    }

    KeycloakSession session;
    RealmModel realm;
    UserModel user;

    @BeforeEach
    void setUp() {
        realm = mock(RealmModel.class);
        lenient().when(realm.getId()).thenReturn(REALM_ID);
        lenient().when(realm.getName()).thenReturn("test");
        lenient().when(realm.getAttribute("frontendUrl")).thenReturn(TRANSMITTER_ISSUER);

        KeycloakContext context = mock(KeycloakContext.class);
        lenient().when(context.getRealm()).thenReturn(realm);
        lenient().when(context.getHttpRequest()).thenReturn(mock(HttpRequest.class));

        session = mock(KeycloakSession.class);
        lenient().when(session.getContext()).thenReturn(context);

        Map<String, Object> attributes = new HashMap<>();
        doAnswer(invocation -> {
            attributes.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(session).setAttribute(anyString(), any());
        lenient().when(session.getAttribute(anyString(), any(Class.class))).thenAnswer(invocation -> {
            Object value = attributes.get(invocation.<String>getArgument(0));
            return value == null ? null : invocation.<Class<?>>getArgument(1).cast(value);
        });

        // Organizations off, so capture skips the membership query entirely.
        OrganizationProvider orgProvider = mock(OrganizationProvider.class);
        lenient().when(orgProvider.isEnabled()).thenReturn(false);
        lenient().when(session.getProvider(OrganizationProvider.class)).thenReturn(orgProvider);

        user = mock(UserModel.class);
        lenient().when(user.getId()).thenReturn(USER_ID);
        lenient().when(user.getServiceAccountClientLink()).thenReturn(null);
        lenient().when(user.getUsername()).thenReturn("purged");
        lenient().when(user.getEmail()).thenReturn(EMAIL);
        lenient().when(user.isEnabled()).thenReturn(true);
        lenient().when(user.getAttributes()).thenReturn(Map.of(
                UserModel.USERNAME, List.of("purged"),
                UserModel.EMAIL, List.of(EMAIL),
                "ssf.notify.receiver-client-id", List.of("true")));
        // Answers, not fixed returns: a Stream is single-use, and tests that re-capture
        // would otherwise re-consume the one instance handed out here.
        lenient().when(user.getGroupsStream()).thenAnswer(invocation -> Stream.empty());
        lenient().when(user.getRoleMappingsStream()).thenAnswer(invocation -> Stream.empty());

        PurgedUserSnapshot.capture(session, realm, user);
    }

    @Test
    void capture_isFoundByUserId() {
        assertNotNull(PurgedUserSnapshot.lookup(session, realm, USER_ID),
                "the snapshot must be retrievable for the rest of the request");
    }

    @Test
    void issuerSubject_fromThisTransmitter_resolvesTheSnapshot() {
        PurgedUserSnapshot resolved =
                PurgedUserSnapshot.lookupBySubject(session, realm, issuerSubject(TRANSMITTER_ISSUER, USER_ID));

        assertNotNull(resolved);
        assertSame(PurgedUserSnapshot.lookup(session, realm, USER_ID), resolved);
    }

    @Test
    void issuerSubject_fromForeignIssuer_doesNotResolve() {
        // For a foreign issuer SubjectUserLookup treats sub as an external IdP
        // subject and resolves it through federated identity. The snapshot index is
        // keyed by internal id, so answering here would short-circuit to the wrong
        // subject whenever a foreign sub happened to equal a captured user id.
        assertNull(PurgedUserSnapshot.lookupBySubject(session, realm, issuerSubject(FOREIGN_ISSUER, USER_ID)));
    }

    @Test
    void issuerSubject_withoutIssuer_doesNotResolve() {
        assertNull(PurgedUserSnapshot.lookupBySubject(session, realm, issuerSubject(null, USER_ID)));
    }

    @Test
    void complexSubject_isGatedOnItsUserMemberIssuer() {
        ComplexSubjectId ours = new ComplexSubjectId();
        ours.setUser(issuerSubject(TRANSMITTER_ISSUER, USER_ID));
        assertNotNull(PurgedUserSnapshot.lookupBySubject(session, realm, ours));

        ComplexSubjectId foreign = new ComplexSubjectId();
        foreign.setUser(issuerSubject(FOREIGN_ISSUER, USER_ID));
        assertNull(PurgedUserSnapshot.lookupBySubject(session, realm, foreign));
    }

    @Test
    void opaqueAndEmailSubjects_areNotIssuerGated() {
        // Neither carries an issuer: an opaque id is transmitter-scoped by definition,
        // and the email index is populated from the captured user's own address.
        OpaqueSubjectId opaque = new OpaqueSubjectId();
        opaque.setId(USER_ID);
        assertNotNull(PurgedUserSnapshot.lookupBySubject(session, realm, opaque));

        EmailSubjectId email = new EmailSubjectId();
        email.setEmail(EMAIL);
        assertNotNull(PurgedUserSnapshot.lookupBySubject(session, realm, email));
    }

    // ----- federated users whose account outlives the local record -----

    @Test
    void localUser_isARealRemoval() {
        assertFalse(PurgedUserSnapshot.lookup(session, realm, USER_ID).isLocalRemovalOnly(),
                "a user with no federation link is genuinely gone");
    }

    @Test
    void readOnlyFederatedUser_isLocalRemovalOnly() {
        // LDAPStorageProvider.removeUser returns true without touching the directory in
        // READ_ONLY mode, so the deletion reports success while the account survives and
        // will be re-imported. Emitting account-purged there would drive irreversible
        // data-retention deletion downstream for a user who comes back.
        recapture(federationComponent(UserStorageProvider.EditMode.READ_ONLY.name()));

        assertTrue(PurgedUserSnapshot.lookup(session, realm, USER_ID).isLocalRemovalOnly());
    }

    @Test
    void unsyncedFederatedUser_isLocalRemovalOnly() {
        recapture(federationComponent(UserStorageProvider.EditMode.UNSYNCED.name()));

        assertTrue(PurgedUserSnapshot.lookup(session, realm, USER_ID).isLocalRemovalOnly());
    }

    @Test
    void writableFederatedUser_isARealRemoval() {
        // WRITABLE propagates the delete to the directory, so the account really is gone.
        recapture(federationComponent(UserStorageProvider.EditMode.WRITABLE.name()));

        assertFalse(PurgedUserSnapshot.lookup(session, realm, USER_ID).isLocalRemovalOnly());
    }

    @Test
    void federationLinkWithoutComponent_isARealRemoval() {
        // A dangling link tells us nothing; treat it as a real removal rather than
        // silently suppressing the event.
        lenient().when(user.getFederationLink()).thenReturn("gone-provider");
        lenient().when(realm.getComponent("gone-provider")).thenReturn(null);
        recaptureAsIs();

        assertFalse(PurgedUserSnapshot.lookup(session, realm, USER_ID).isLocalRemovalOnly());
    }

    @Test
    void federationProviderWithoutEditMode_isARealRemoval() {
        recapture(federationComponent(null));

        assertFalse(PurgedUserSnapshot.lookup(session, realm, USER_ID).isLocalRemovalOnly());
    }

    @Test
    void discard_removesBothIndexes() {
        PurgedUserSnapshot.discard(session, realm, USER_ID);

        assertNull(PurgedUserSnapshot.lookup(session, realm, USER_ID));

        EmailSubjectId email = new EmailSubjectId();
        email.setEmail(EMAIL);
        assertNull(PurgedUserSnapshot.lookupBySubject(session, realm, email));
    }

    /** Re-captures the user behind a federation provider configured with {@code editMode}. */
    private void recapture(ComponentModel component) {
        lenient().when(user.getFederationLink()).thenReturn("ldap-provider");
        lenient().when(realm.getComponent("ldap-provider")).thenReturn(component);
        recaptureAsIs();
    }

    private void recaptureAsIs() {
        PurgedUserSnapshot.discard(session, realm, USER_ID);
        PurgedUserSnapshot.capture(session, realm, user);
    }

    private ComponentModel federationComponent(String editMode) {
        ComponentModel component = new ComponentModel();
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        if (editMode != null) {
            config.putSingle(LDAPConstants.EDIT_MODE, editMode);
        }
        component.setConfig(config);
        return component;
    }

    private IssuerSubjectId issuerSubject(String iss, String sub) {
        IssuerSubjectId subject = new IssuerSubjectId();
        subject.setIss(iss);
        subject.setSub(sub);
        return subject;
    }
}
