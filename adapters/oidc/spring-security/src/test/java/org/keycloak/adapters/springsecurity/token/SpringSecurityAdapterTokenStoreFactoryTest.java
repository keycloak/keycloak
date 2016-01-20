package org.keycloak.adapters.springsecurity.token;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.adapters.spi.AdapterSessionStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.*;

/**
 * Spring Security adapter token store factory tests.
 */
public class SpringSecurityAdapterTokenStoreFactoryTest {

    private AdapterTokenStoreFactory factory = new SpringSecurityAdapterTokenStoreFactory();

    @Mock
    private KeycloakDeployment deployment;

    @Mock
    private HttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreateAdapterTokenStore() throws Exception {
        AdapterSessionStore store = factory.createAdapterTokenStore(deployment, request);
        assertNotNull(store);
        assertTrue(store instanceof SpringSecurityTokenStore);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateAdapterTokenStoreNullDeployment() throws Exception {
        factory.createAdapterTokenStore(null, request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateAdapterTokenStoreNullRequest() throws Exception {
        factory.createAdapterTokenStore(deployment, null);
    }
}
