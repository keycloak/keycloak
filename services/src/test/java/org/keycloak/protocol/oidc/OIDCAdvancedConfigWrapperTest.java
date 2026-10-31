package org.keycloak.protocol.oidc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;

import static org.junit.Assert.assertEquals;

public class OIDCAdvancedConfigWrapperTest {

    @Test
    public void testGetAttributeMultivaluedTrimsSingleAndMultipleValues() {
        ClientRepresentation rep = new ClientRepresentation();
        Map<String, String> attributes = new HashMap<>();
        rep.setAttributes(attributes);
        OIDCAdvancedConfigWrapper wrapper = OIDCAdvancedConfigWrapper.fromClientRepresentation(rep);

        // Single value with trailing space (Issue #54421)
        attributes.put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "https://url1/ ");
        List<String> uris = wrapper.getPostLogoutRedirectUris();
        assertEquals(Collections.singletonList("https://url1/"), uris);

        // Single value with leading and trailing space
        attributes.put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "  https://url1/  ");
        uris = wrapper.getPostLogoutRedirectUris();
        assertEquals(Collections.singletonList("https://url1/"), uris);

        // Multiple values with trailing delimiter and spaces
        attributes.put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "https://url1/ ##https://url2/ ##");
        uris = wrapper.getPostLogoutRedirectUris();
        assertEquals(Arrays.asList("https://url1/", "https://url2/"), uris);
    }

    @Test
    public void testSetAttributeMultivaluedTrimsValues() {
        ClientRepresentation rep = new ClientRepresentation();
        OIDCAdvancedConfigWrapper wrapper = OIDCAdvancedConfigWrapper.fromClientRepresentation(rep);

        wrapper.setPostLogoutRedirectUris(Arrays.asList("  https://url1/ ", " https://url2/  "));
        assertEquals("https://url1/##https://url2/", rep.getAttributes().get(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS));
        assertEquals(Arrays.asList("https://url1/", "https://url2/"), wrapper.getPostLogoutRedirectUris());
    }
}
