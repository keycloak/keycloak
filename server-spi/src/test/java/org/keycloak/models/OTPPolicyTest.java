package org.keycloak.models;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.credential.OTPCredentialModel;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class OTPPolicyTest {
    private RealmModel realm;
    private UserModel user;
    private OTPPolicy instance;
    
    @Parameterized.Parameter
    public boolean loginWithEmailAllowed;
    @Parameterized.Parameter(1)
    public boolean registrationEmailAsUsername;
    @Parameterized.Parameter(2)
    public String expectedLabel;
    
    @Before
    public void initMocks() {
        realm = mock(RealmModel.class);
        user = mock(UserModel.class);
        
        when(realm.getDisplayName()).thenReturn("Display Name");
        when(user.getUsername()).thenReturn("user-name");
        when(user.getEmail()).thenReturn("mock@user.com");
    }
    
    @Before
    public void initInstance() {
        instance = new OTPPolicy(
            OTPCredentialModel.TOTP,
            Algorithm.RS512,
            0,
            6,
            1,
            30
        );
    }
    
    @Test
    public void getKeyURI_shouldWriteExpectedLabel() {
        when(realm.isLoginWithEmailAllowed()).thenReturn(loginWithEmailAllowed);
        when(realm.isRegistrationEmailAsUsername()).thenReturn(registrationEmailAsUsername);
        
        final String result = instance.getKeyURI(realm, user, "SECRET");
        final URI keyUri = URI.create(result);
        
        Assert.assertEquals("/Display Name:" + expectedLabel, keyUri.getPath());
    }
    
    @Parameterized.Parameters(name = "loginWithEmailAllowed={0} and registrationEmailAsUsername={1} should yield label {3}")
    public static Collection<Object[]> testParameters() {
        return Arrays.asList(new Object[][] {
            {true, true, "user-name"},
            {true, false, "mock@user.com"},
            {false, true, "user-name"},
            {false, false, "user-name"},
        });
    }
}
