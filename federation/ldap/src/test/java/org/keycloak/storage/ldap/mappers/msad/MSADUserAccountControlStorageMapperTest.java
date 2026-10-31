/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.storage.ldap.mappers.msad;

import java.util.stream.Stream;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.utils.KeycloakSessionUtil;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests that the MSAD account control mapper reports the "the password must be changed in the directory" state on the
 * authentication session when the LDAP provider cannot perform the password change itself (edit mode is not writable),
 * so that the authentication flow is able to answer with a distinct error instead of "Invalid user credentials".
 */
public class MSADUserAccountControlStorageMapperTest {

    private static final String REALM_NAME = "test";

    @After
    public void clearSession() {
        KeycloakSessionUtil.setKeycloakSession(null);
    }

    @Test
    public void testMustChangePasswordIsReportedWhenEditModeIsNotWritable() {
        AuthenticationSessionModel authSession = mock(AuthenticationSessionModel.class);
        MSADUserAccountControlStorageMapper mapper = mapper(UserStorageProvider.EditMode.READ_ONLY, authSession);

        Assert.assertFalse("Login must fail, as the password cannot be changed in a directory which is not writable",
                mapper.processAuthErrorCode("773", user()));
        verify(authSession).setAuthNote(LDAPConstants.PASSWORD_CHANGE_REQUIRED_IN_DIRECTORY, "true");
    }

    @Test
    public void testExpiredPasswordIsReportedWhenEditModeIsNotWritable() {
        AuthenticationSessionModel authSession = mock(AuthenticationSessionModel.class);
        MSADUserAccountControlStorageMapper mapper = mapper(UserStorageProvider.EditMode.UNSYNCED, authSession);

        Assert.assertFalse(mapper.processAuthErrorCode("532", user()));
        verify(authSession).setAuthNote(LDAPConstants.PASSWORD_CHANGE_REQUIRED_IN_DIRECTORY, "true");
    }

    @Test
    public void testWrongPasswordIsNotReportedAsPasswordChange() {
        AuthenticationSessionModel authSession = mock(AuthenticationSessionModel.class);
        MSADUserAccountControlStorageMapper mapper = mapper(UserStorageProvider.EditMode.READ_ONLY, authSession);

        Assert.assertFalse("Login must fail for a wrong password as well", mapper.processAuthErrorCode("52e", user()));
        verify(authSession, never()).setAuthNote(LDAPConstants.PASSWORD_CHANGE_REQUIRED_IN_DIRECTORY, "true");
    }

    @Test
    public void testWritableModeStillAddsTheRequiredAction() {
        AuthenticationSessionModel authSession = mock(AuthenticationSessionModel.class);
        MSADUserAccountControlStorageMapper mapper = mapper(UserStorageProvider.EditMode.WRITABLE, authSession);

        Assert.assertTrue("User is allowed to login and is asked to change the password",
                mapper.processAuthErrorCode("773", user()));
        verify(authSession).addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
        verify(authSession, never()).setAuthNote(LDAPConstants.PASSWORD_CHANGE_REQUIRED_IN_DIRECTORY, "true");
    }

    private static MSADUserAccountControlStorageMapper mapper(UserStorageProvider.EditMode editMode, AuthenticationSessionModel authSession) {
        KeycloakContext context = mock(KeycloakContext.class);
        RealmModel realm = mock(RealmModel.class);
        when(realm.getName()).thenReturn(REALM_NAME);
        when(context.getRealm()).thenReturn(realm);
        when(context.getAuthenticationSession()).thenReturn(authSession);

        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getContext()).thenReturn(context);
        KeycloakSessionUtil.setKeycloakSession(session);

        MultivaluedHashMap<String, String> ldapConfigMap = new MultivaluedHashMap<>();
        ldapConfigMap.add(LDAPConstants.EDIT_MODE, editMode.toString());
        LDAPIdentityStore ldapIdentityStore = new LDAPIdentityStore(null, new LDAPConfig(ldapConfigMap));
        LDAPStorageProvider ldapProvider = new LDAPStorageProvider(null, session, new ComponentModel(), ldapIdentityStore);

        return new MSADUserAccountControlStorageMapper(new ComponentModel(), ldapProvider);
    }

    private static UserModel user() {
        return new UserModelDelegate(null) {
            @Override
            public String getId() {
                return "john";
            }

            @Override
            public String getUsername() {
                return "john";
            }

            @Override
            public Stream<String> getRequiredActionsStream() {
                return Stream.empty();
            }
        };
    }
}
