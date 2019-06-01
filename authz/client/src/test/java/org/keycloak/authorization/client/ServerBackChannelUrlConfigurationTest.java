package org.keycloak.authorization.client;

import org.junit.Test;
import org.keycloak.authorization.client.representation.ServerConfiguration;

import static org.junit.Assert.assertEquals;

public class ServerBackChannelUrlConfigurationTest {

    @Test
    public void init_ShouldResolveAllUrlsBasedOnGivenUrlAndRealm() {
        ServerConfiguration delegate = new ServerConfiguration();
        String url = "https://test.app/auth";
        String realm = "test";

        ServerBackChannelUrlConfiguration config = new ServerBackChannelUrlConfiguration(delegate, url, realm);

        assertEquals(url + "/realms/test/protocol/openid-connect/token", config.getTokenEndpoint());
        assertEquals(url + "/realms/test/protocol/openid-connect/token/introspect", config.getTokenIntrospectionEndpoint());
        assertEquals(url + "/realms/test/protocol/openid-connect/userinfo", config.getUserinfoEndpoint());
        assertEquals(url + "/realms/test/protocol/openid-connect/logout", config.getLogoutEndpoint());
        assertEquals(url + "/realms/test/protocol/openid-connect/certs", config.getJwksUri());
        assertEquals(url + "/realms/test/clients-registrations/openid-connect", config.getRegistrationEndpoint());
        assertEquals(url + "/realms/test/authz/protection/resource_set", config.getResourceRegistrationEndpoint());
        assertEquals(url + "/realms/test/authz/protection/permission", config.getPermissionEndpoint());
        assertEquals(url + "/realms/test/authz/protection/uma-policy", config.getPolicyEndpoint());
    }
}
