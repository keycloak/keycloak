package org.keycloak.ssf.transmitter.subject;


import java.util.Map;
import java.util.Properties;

import org.keycloak.common.Profile;
import org.keycloak.common.profile.PropertiesProfileConfigResolver;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.ssf.event.stream.SsfStreamVerificationEvent;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.metadata.DefaultSubjects;
import org.keycloak.ssf.subject.ComplexSubjectId;
import org.keycloak.ssf.subject.OpaqueSubjectId;
import org.keycloak.ssf.transmitter.stream.StreamConfig;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SubjectSubscriptionFilter} covering the
 * ALL/NONE × true/false/absent attribute matrix.
 */
class SubjectSubscriptionFilterTest {

    static final String CLIENT_ID = "receiver-client-id";
    static final String USER_ID = "user-123";

    @BeforeAll
    static void initProfile() {
        Profile.configure(new PropertiesProfileConfigResolver(new Properties()));
    }

    SubjectSubscriptionFilter filter;
    KeycloakSession session;
    RealmModel realm;
    UserModel user;
    UserProvider userProvider;
    StreamConfig stream;

    @BeforeEach
    void setUp() {
        filter = new SubjectSubscriptionFilter();

        session = mock(KeycloakSession.class);
        realm = mock(RealmModel.class);
        user = mock(UserModel.class);
        userProvider = mock(UserProvider.class);

        KeycloakContext context = mock(KeycloakContext.class);
        when(session.getContext()).thenReturn(context);
        when(context.getRealm()).thenReturn(realm);
        when(session.users()).thenReturn(userProvider);
        when(userProvider.getUserById(realm, USER_ID)).thenReturn(user);
        lenient().when(user.getId()).thenReturn(USER_ID);

        stream = new StreamConfig();
        stream.setStreamId("stream-1");
        stream.setClientId(CLIENT_ID);
    }

    private SsfSecurityEventToken eventWithUserSubject() {
        SsfSecurityEventToken token = new SsfSecurityEventToken();
        ComplexSubjectId complex = new ComplexSubjectId();
        OpaqueSubjectId userSub = new OpaqueSubjectId();
        userSub.setId(USER_ID);
        complex.setUser(userSub);
        token.setSubjectId(complex);
        token.setJti("jti-1");
        return token;
    }

    private SsfSecurityEventToken verificationEvent() {
        SsfSecurityEventToken token = new SsfSecurityEventToken();
        token.setJti("jti-verify");
        token.setEvents(Map.of(SsfStreamVerificationEvent.TYPE, new SsfStreamVerificationEvent()));
        OpaqueSubjectId sub = new OpaqueSubjectId();
        sub.setId("stream-id");
        token.setSubjectId(sub);
        return token;
    }

    // ----- ALL mode -----

    @Test
    void all_noAttribute_delivers() {
        stream.setDefaultSubjects(DefaultSubjects.ALL);
        when(user.getFirstAttribute(SsfNotifyAttributes.attributeKey(CLIENT_ID))).thenReturn(null);

        assertTrue(filter.shouldDispatch(eventWithUserSubject(), stream, CLIENT_ID, session));
    }

    @Test
    void all_trueAttribute_delivers() {
        stream.setDefaultSubjects(DefaultSubjects.ALL);
        when(user.getFirstAttribute(SsfNotifyAttributes.attributeKey(CLIENT_ID))).thenReturn("true");

        assertTrue(filter.shouldDispatch(eventWithUserSubject(), stream, CLIENT_ID, session));
    }

    @Test
    void all_falseAttribute_skips() {
        stream.setDefaultSubjects(DefaultSubjects.ALL);
        when(user.getFirstAttribute(SsfNotifyAttributes.attributeKey(CLIENT_ID))).thenReturn("false");

        assertFalse(filter.shouldDispatch(eventWithUserSubject(), stream, CLIENT_ID, session));
    }

    @Test
    void nullDefaultSubjects_treatedAsNone_noAttribute_blocks() {
        stream.setDefaultSubjects(null);
        when(user.getFirstAttribute(SsfNotifyAttributes.attributeKey(CLIENT_ID))).thenReturn(null);

        assertFalse(filter.shouldDispatch(eventWithUserSubject(), stream, CLIENT_ID, session));
    }

    @Test
    void nullDefaultSubjects_treatedAsNone_trueAttribute_delivers() {
        stream.setDefaultSubjects(null);
        when(user.getFirstAttribute(SsfNotifyAttributes.attributeKey(CLIENT_ID))).thenReturn("true");

        assertTrue(filter.shouldDispatch(eventWithUserSubject(), stream, CLIENT_ID, session));
    }

    @Test
    void nullDefaultSubjects_treatedAsNone_falseAttribute_blocks() {
        stream.setDefaultSubjects(null);
        when(user.getFirstAttribute(SsfNotifyAttributes.attributeKey(CLIENT_ID))).thenReturn("false");

        assertFalse(filter.shouldDispatch(eventWithUserSubject(), stream, CLIENT_ID, session));
    }

    // ----- NONE mode -----

    @Test
    void none_noAttribute_skips() {
        stream.setDefaultSubjects(DefaultSubjects.NONE);
        when(user.getFirstAttribute(SsfNotifyAttributes.attributeKey(CLIENT_ID))).thenReturn(null);

        assertFalse(filter.shouldDispatch(eventWithUserSubject(), stream, CLIENT_ID, session));
    }

    @Test
    void none_trueAttribute_delivers() {
        stream.setDefaultSubjects(DefaultSubjects.NONE);
        when(user.getFirstAttribute(SsfNotifyAttributes.attributeKey(CLIENT_ID))).thenReturn("true");

        assertTrue(filter.shouldDispatch(eventWithUserSubject(), stream, CLIENT_ID, session));
    }

    @Test
    void none_falseAttribute_skips() {
        stream.setDefaultSubjects(DefaultSubjects.NONE);
        when(user.getFirstAttribute(SsfNotifyAttributes.attributeKey(CLIENT_ID))).thenReturn("false");

        assertFalse(filter.shouldDispatch(eventWithUserSubject(), stream, CLIENT_ID, session));
    }

    // ----- No subject (verification events) -----

    @Test
    void all_noSubject_alwaysDelivers() {
        stream.setDefaultSubjects(DefaultSubjects.ALL);
        assertTrue(filter.shouldDispatch(verificationEvent(), stream, CLIENT_ID, session));
    }

    @Test
    void none_noSubject_alwaysDelivers() {
        stream.setDefaultSubjects(DefaultSubjects.NONE);
        assertTrue(filter.shouldDispatch(verificationEvent(), stream, CLIENT_ID, session));
    }

    // ----- Complex subject -----

    @Test
    void none_complexSubjectWithUser_trueAttribute_delivers() {
        stream.setDefaultSubjects(DefaultSubjects.NONE);
        when(user.getFirstAttribute(SsfNotifyAttributes.attributeKey(CLIENT_ID))).thenReturn("true");

        SsfSecurityEventToken token = new SsfSecurityEventToken();
        ComplexSubjectId complex = new ComplexSubjectId();
        OpaqueSubjectId userSub = new OpaqueSubjectId();
        userSub.setId(USER_ID);
        complex.setUser(userSub);
        token.setSubjectId(complex);
        token.setJti("jti-complex");

        assertTrue(filter.shouldDispatch(token, stream, CLIENT_ID, session));
    }

    @Test
    void all_complexSubjectWithUser_falseAttribute_skips() {
        stream.setDefaultSubjects(DefaultSubjects.ALL);
        when(user.getFirstAttribute(SsfNotifyAttributes.attributeKey(CLIENT_ID))).thenReturn("false");

        SsfSecurityEventToken token = new SsfSecurityEventToken();
        ComplexSubjectId complex = new ComplexSubjectId();
        OpaqueSubjectId userSub = new OpaqueSubjectId();
        userSub.setId(USER_ID);
        complex.setUser(userSub);
        token.setSubjectId(complex);
        token.setJti("jti-complex-excl");

        assertFalse(filter.shouldDispatch(token, stream, CLIENT_ID, session));
    }

    // ----- Unresolvable user -----

    @Test
    void none_unresolvableUserSubject_blocks() {
        stream.setDefaultSubjects(DefaultSubjects.NONE);
        when(userProvider.getUserById(realm, "unknown")).thenReturn(null);

        // Complex subject with a user component that can't be resolved
        SsfSecurityEventToken token = new SsfSecurityEventToken();
        ComplexSubjectId complex = new ComplexSubjectId();
        OpaqueSubjectId userSub = new OpaqueSubjectId();
        userSub.setId("unknown");
        complex.setUser(userSub);
        token.setSubjectId(complex);
        token.setJti("jti-unresolvable");

        // User subject present but unresolvable → block in NONE mode
        assertFalse(filter.shouldDispatch(token, stream, CLIENT_ID, session));
    }

    @Test
    void none_unknownEventWithOpaqueSubject_blocks() {
        stream.setDefaultSubjects(DefaultSubjects.NONE);

        // Non-stream event with an opaque subject — no user can be
        // resolved, and it's not a stream management event, so it
        // should be blocked in NONE mode.
        SsfSecurityEventToken token = new SsfSecurityEventToken();
        OpaqueSubjectId sub = new OpaqueSubjectId();
        sub.setId("some-opaque-id");
        token.setSubjectId(sub);
        token.setJti("jti-unknown");

        assertFalse(filter.shouldDispatch(token, stream, CLIENT_ID, session));
    }
}
