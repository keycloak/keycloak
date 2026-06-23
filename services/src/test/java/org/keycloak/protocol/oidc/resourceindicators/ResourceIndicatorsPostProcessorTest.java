package org.keycloak.protocol.oidc.resourceindicators;

import java.lang.reflect.Proxy;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.token.TokenInterceptorException;
import org.keycloak.protocol.oidc.token.TokenPostProcessorContext;
import org.keycloak.representations.AccessToken;

public class ResourceIndicatorsPostProcessorTest {

    @Test
    public void nullAudienceReturnsInvalidTarget() {
        TokenInterceptorException ex = Assert.assertThrows(TokenInterceptorException.class,
                () -> runWithNullAudience("urn:client:my-service"));
        Assert.assertEquals(OAuthErrorException.INVALID_TARGET, ex.getError());
        Assert.assertEquals(ResourceIndicatorConstants.ERROR_INVALID_RESOURCE, ex.getDescription());
    }

    @Test
    public void matchingResourceWithNullRefreshTokenSetsAudience() {
        AccessToken accessToken = new AccessToken();
        accessToken.audience("other-service", "my-service");
        ClientSessionContext clientSessionCtx = stubClientSessionContext("urn:client:my-service", OAuth2Constants.CLIENT_CREDENTIALS);
        TokenPostProcessorContext context = new TokenPostProcessorContext(
                null, null, null, accessToken, clientSessionCtx);

        new ResourceIndicatorsPostProcessor(null).process(context);

        Assert.assertArrayEquals(new String[]{"my-service"}, accessToken.getAudience());
    }

    private void runWithNullAudience(String resource) {
        AccessToken accessToken = new AccessToken();
        ClientSessionContext clientSessionCtx = stubClientSessionContext(resource, OAuth2Constants.CLIENT_CREDENTIALS);
        TokenPostProcessorContext context = new TokenPostProcessorContext(
                null, null, null, accessToken, clientSessionCtx);
        new ResourceIndicatorsPostProcessor(null).process(context);
    }

    private ClientSessionContext stubClientSessionContext(String resource, String grantType) {
        return (ClientSessionContext) Proxy.newProxyInstance(
                ClientSessionContext.class.getClassLoader(),
                new Class<?>[]{ClientSessionContext.class},
                (proxy, method, args) -> {
                    if ("getAttribute".equals(method.getName()) && args != null && args.length == 2) {
                        if (OAuth2Constants.RESOURCE.equals(args[0])) return resource;
                        if (Constants.GRANT_TYPE.equals(args[0])) return grantType;
                    }
                    return null;
                });
    }
}
