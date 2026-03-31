package org.keycloak.tests.scim.tck;

import java.util.List;
import java.util.Set;

import org.keycloak.scim.resource.config.ServiceProviderConfig;
import org.keycloak.scim.resource.config.ServiceProviderConfig.AuthenticationScheme;
import org.keycloak.scim.resource.config.ServiceProviderConfig.BulkSupport;
import org.keycloak.scim.resource.config.ServiceProviderConfig.FilterSupport;
import org.keycloak.scim.resource.config.ServiceProviderConfig.Supported;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = ScimServerConfig.class)
public class ServiceProviderConfigTest extends AbstractScimTest {

    @Test
    public void testFeatureDisabled() {
        ServiceProviderConfig config = client.config().get();
        assertNotNull(config);
        BulkSupport bulk = config.getBulk();
        assertNotNull(bulk);
        assertFalse(bulk.getSupported());
        Supported etag = config.getEtag();
        assertNotNull(etag);
        assertFalse(etag.getSupported());
        Supported changePassword = config.getChangePassword();
        assertNotNull(changePassword);
        assertFalse(changePassword.getSupported());
        Supported patch = config.getPatch();
        assertNotNull(patch);
        assertTrue(patch.getSupported());
        List<AuthenticationScheme> authenticationSchemes = config.getAuthenticationSchemes();
        assertNotNull(authenticationSchemes);
        assertEquals(1, authenticationSchemes.size());
        AuthenticationScheme bearerScheme = authenticationSchemes.get(0);
        assertEquals("OAuth Bearer Token", bearerScheme.getName());
        assertEquals("Authentication scheme using the OAuth Bearer Token standard", bearerScheme.getDescription());
        assertEquals("https://tools.ietf.org/html/rfc6750", bearerScheme.getSpecUri());
        assertEquals("oauthbearertoken", bearerScheme.getType());
        FilterSupport filter = config.getFilter();
        assertNotNull(filter);
        assertTrue(filter.getSupported());
        Set<String> schemas = config.getSchemas();
        assertNotNull(schemas);
        assertEquals(1, schemas.size());
        assertTrue(schemas.contains(ServiceProviderConfig.SCHEMA));
    }
}
