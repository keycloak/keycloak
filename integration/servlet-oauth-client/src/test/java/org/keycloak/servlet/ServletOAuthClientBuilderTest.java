package org.keycloak.servlet;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.enums.RelativeUrlsUsed;
import org.keycloak.representations.idm.CredentialRepresentation;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ServletOAuthClientBuilderTest {

    @Test
    public void testBuilder() {
        ServletOAuthClient oauthClient = ServletOAuthClientBuilder.build(getClass().getResourceAsStream("/keycloak.json"));
        Assert.assertEquals("https://localhost:8443/auth/realms/demo/protocol/openid-connect/auth", oauthClient.getDeployment().getAuthUrl().clone().build().toString());
        Assert.assertEquals("https://backend:8443/auth/realms/demo/protocol/openid-connect/token", oauthClient.getDeployment().getTokenUrl());
        assertEquals(RelativeUrlsUsed.NEVER, oauthClient.getRelativeUrlsUsed());
        Assert.assertEquals("customer-portal", oauthClient.getClientId());
        Assert.assertEquals("234234-234234-234234", oauthClient.getCredentials().get(CredentialRepresentation.SECRET));
        Assert.assertEquals(true, oauthClient.isPublicClient());
    }
}
