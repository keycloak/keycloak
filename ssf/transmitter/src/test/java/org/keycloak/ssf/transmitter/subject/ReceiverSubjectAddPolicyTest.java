package org.keycloak.ssf.transmitter.subject;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.ssf.subject.SubjectResolution;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the receiver-driven add gate in
 * {@link SubjectManagementService#checkReceiverMayAdd}: the
 * {@link ReceiverSubjectAddPolicy} matrix, the admin-exclusion
 * override, and the already-notified idempotency carve-out.
 */
class ReceiverSubjectAddPolicyTest {

    static final String CLIENT_UUID = "receiver-uuid";
    static final String CLIENT_ID = "receiver";
    static final String USER_ID = "user-1";
    static final String NOTIFY_KEY = "ssf.notify." + CLIENT_ID;

    KeycloakSession session;
    RealmModel realm;
    ClientModel client;
    UserModel user;
    UserProvider userProvider;
    UserSessionProvider sessionProvider;
    SubjectManagementService service;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        realm = mock(RealmModel.class);
        client = mock(ClientModel.class);
        user = mock(UserModel.class);
        userProvider = mock(UserProvider.class);
        sessionProvider = mock(UserSessionProvider.class);

        KeycloakContext context = mock(KeycloakContext.class);
        when(session.getContext()).thenReturn(context);
        when(context.getRealm()).thenReturn(realm);
        lenient().when(session.users()).thenReturn(userProvider);
        lenient().when(session.sessions()).thenReturn(sessionProvider);

        when(client.getClientId()).thenReturn(CLIENT_ID);
        lenient().when(client.getId()).thenReturn(CLIENT_UUID);
        lenient().when(user.getId()).thenReturn(USER_ID);

        lenient().when(sessionProvider.getUserSessionsStream(realm, user)).thenAnswer(i -> Stream.empty());
        lenient().when(sessionProvider.getOfflineUserSessionsStream(realm, user)).thenAnswer(i -> Stream.empty());

        service = new SubjectManagementService(session);
    }

    void policy(String value) {
        when(client.getAttribute(ClientStreamStore.SSF_RECEIVER_SUBJECT_ADD_POLICY_KEY)).thenReturn(value);
    }

    SubjectManagementResult checkUser() {
        return service.checkReceiverMayAdd(client, new SubjectResolution.User(user));
    }

    UserSessionModel sessionWithReceiver() {
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getAuthenticatedClientSessionByClient(CLIENT_UUID))
                .thenReturn(mock(AuthenticatedClientSessionModel.class));
        return userSession;
    }

    @Test
    void defaultPolicy_unrelatedUser_denied() {
        policy(null);
        assertEquals(SubjectManagementResult.SUBJECT_NOT_PERMITTED, checkUser());
    }

    @Test
    void invalidPolicyValue_fallsBackToAuthenticated() {
        policy("bogus");
        assertEquals(SubjectManagementResult.SUBJECT_NOT_PERMITTED, checkUser());
    }

    @Test
    void authenticated_userWithSessionForReceiver_permitted() {
        policy("AUTHENTICATED");
        UserSessionModel userSession = sessionWithReceiver();
        when(sessionProvider.getUserSessionsStream(realm, user)).thenAnswer(i -> Stream.of(userSession));
        assertNull(checkUser());
    }

    @Test
    void authenticated_userWithSessionForOtherClientOnly_denied() {
        policy("AUTHENTICATED");
        UserSessionModel otherSession = mock(UserSessionModel.class);
        when(sessionProvider.getUserSessionsStream(realm, user)).thenAnswer(i -> Stream.of(otherSession));
        assertEquals(SubjectManagementResult.SUBJECT_NOT_PERMITTED, checkUser());
    }

    @Test
    void authenticated_userWithOfflineSessionForReceiver_permitted() {
        policy("AUTHENTICATED");
        UserSessionModel offlineSession = sessionWithReceiver();
        when(sessionProvider.getOfflineUserSessionsStream(realm, user)).thenAnswer(i -> Stream.of(offlineSession));
        assertNull(checkUser());
    }

    @Test
    void authenticated_userWithConsentForReceiver_permitted() {
        policy("AUTHENTICATED");
        when(userProvider.getConsentByClient(realm, USER_ID, CLIENT_UUID)).thenReturn(mock(UserConsentModel.class));
        assertNull(checkUser());
    }

    @Test
    void authenticated_alreadyNotifiedUser_permitted() {
        policy("AUTHENTICATED");
        when(user.getFirstAttribute(NOTIFY_KEY)).thenReturn("true");
        assertNull(checkUser());
    }

    @Test
    void any_unrelatedUser_permitted() {
        policy("any");
        assertNull(checkUser());
    }

    @Test
    void none_userWithSessionForReceiver_denied() {
        policy("NONE");
        UserSessionModel userSession = sessionWithReceiver();
        lenient().when(sessionProvider.getUserSessionsStream(realm, user)).thenAnswer(i -> Stream.of(userSession));
        assertEquals(SubjectManagementResult.SUBJECT_NOT_PERMITTED, checkUser());
    }

    @Test
    void any_adminIgnoredUser_denied() {
        policy("ANY");
        when(user.getFirstAttribute(NOTIFY_KEY)).thenReturn("false");
        assertEquals(SubjectManagementResult.SUBJECT_NOT_PERMITTED, checkUser());
    }

    @Test
    void authenticated_organization_denied() {
        policy("AUTHENTICATED");
        OrganizationModel org = mock(OrganizationModel.class);
        assertEquals(SubjectManagementResult.SUBJECT_NOT_PERMITTED,
                service.checkReceiverMayAdd(client, new SubjectResolution.Organization(org)));
    }

    @Test
    void any_organization_permitted() {
        policy("ANY");
        OrganizationModel org = mock(OrganizationModel.class);
        assertNull(service.checkReceiverMayAdd(client, new SubjectResolution.Organization(org)));
    }

    @Test
    void any_adminIgnoredOrganization_denied() {
        policy("ANY");
        OrganizationModel org = mock(OrganizationModel.class);
        when(org.getAttributes()).thenReturn(Map.of(NOTIFY_KEY, List.of("false")));
        assertEquals(SubjectManagementResult.SUBJECT_NOT_PERMITTED,
                service.checkReceiverMayAdd(client, new SubjectResolution.Organization(org)));
    }

    @Test
    void unresolvedSubject_passesThrough() {
        policy("NONE");
        assertNull(service.checkReceiverMayAdd(client, SubjectResolution.NOT_FOUND));
    }
}
