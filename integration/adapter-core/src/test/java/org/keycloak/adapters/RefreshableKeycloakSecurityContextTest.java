package org.keycloak.adapters;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.util.Time;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

public class RefreshableKeycloakSecurityContextTest
{
    private static final String TOKEN_STRING = "token_string";
    private static final String ID_TOKEN_STRING = "id_token_string";
    private static final String REFRESH_STRING = "refresh_token";

    @Mock
    private KeycloakDeployment deployment;
    @Mock
    private AdapterTokenStore  tokenStore;
    @Mock
    private AccessToken token;
    @Mock
    private IDToken     idToken;

    private String tokenString;
    private String idTokenString;
    private String refreshToken;

    @InjectMocks
    private RefreshableKeycloakSecurityContext context;

    @Before
    public void setUp() throws Exception {
        this.tokenString = TOKEN_STRING;
        this.idTokenString = ID_TOKEN_STRING;
        this.refreshToken = REFRESH_STRING;

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testIsTokenTimeToLiveSufficient() throws Exception {
        Assert.assertFalse(context.isTokenTimeToLiveSufficient(null));

        when(deployment.getTokenMinimumTimeToLive()).thenReturn(10);
        when(token.getExpiration()).thenReturn(Time.currentTime() + 20);
        Assert.assertTrue(context.isTokenTimeToLiveSufficient(token));

        when(token.getExpiration()).thenReturn(Time.currentTime() + 5);
        Assert.assertFalse(context.isTokenTimeToLiveSufficient(token));
    }

    @Test
    public void testRefreshExpiredTokenTimeToLiveForTimeToLive() throws Exception {
        when(token.isActive()).thenReturn(true);
        when(token.getIssuedAt()).thenReturn(5);
        when(token.getNotBefore()).thenReturn(4);

        when(deployment.getTokenMinimumTimeToLive()).thenReturn(10);
        when(token.getExpiration()).thenReturn(Time.currentTime() + 20);

        Assert.assertTrue(context.refreshExpiredToken(true));
        verify(tokenStore, never()).refreshCallback(context);
    }
}