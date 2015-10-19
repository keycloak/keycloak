package org.keycloak.adapters.springsecurity.authentication;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.internal.util.collections.Sets;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.security.Principal;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Keycloak authentication provider tests.
 */
public class KeycloakAuthenticationProviderTest {
    private KeycloakAuthenticationProvider provider = new KeycloakAuthenticationProvider();
    private KeycloakAuthenticationToken token;
    private Set<String> roles = Sets.newSet("user", "admin");

    @Before
    public void setUp() throws Exception {
        Principal principal = mock(Principal.class);
        RefreshableKeycloakSecurityContext securityContext = mock(RefreshableKeycloakSecurityContext.class);
        KeycloakAccount account = new SimpleKeycloakAccount(principal, roles, securityContext);

        token = new KeycloakAuthenticationToken(account);
    }

    @Test
    public void testAuthenticate() throws Exception {
        Authentication result = provider.authenticate(token);
        assertNotNull(result);
        assertEquals(roles, AuthorityUtils.authorityListToSet(result.getAuthorities()));
        assertTrue(result.isAuthenticated());
        assertNotNull(result.getPrincipal());
        assertNotNull(result.getCredentials());
        assertNotNull(result.getDetails());
    }

    @Test
    public void testSupports() throws Exception {
        assertTrue(provider.supports(KeycloakAuthenticationToken.class));
        assertFalse(provider.supports(PreAuthenticatedAuthenticationToken.class));
    }

    @Test
    public void testGrantedAuthoritiesMapper() throws Exception {
        SimpleAuthorityMapper grantedAuthorityMapper = new SimpleAuthorityMapper();
        grantedAuthorityMapper.setPrefix("ROLE_");
        grantedAuthorityMapper.setConvertToUpperCase(true);
        provider.setGrantedAuthoritiesMapper(grantedAuthorityMapper);

        Authentication result = provider.authenticate(token);
        assertEquals(Sets.newSet("ROLE_USER", "ROLE_ADMIN"),
            AuthorityUtils.authorityListToSet(result.getAuthorities()));
    }
}
