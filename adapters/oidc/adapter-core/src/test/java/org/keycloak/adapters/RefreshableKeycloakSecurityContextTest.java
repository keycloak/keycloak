package org.keycloak.adapters;

import org.junit.Test;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author github.com/tubbynl
 *
 */
public class RefreshableKeycloakSecurityContextTest {

	@Test
	public void isActive() {
		TokenMetadataRepresentation token = new TokenMetadataRepresentation();
		token.setActive(true);
		token.issuedNow();
		RefreshableKeycloakSecurityContext sut = new RefreshableKeycloakSecurityContext(null,null,null,token,null, null, null);
		
		// verify false if null deployment (KEYCLOAK-3050; yielded a npe)
		assertFalse(sut.isActive());
	}

	@Test
	public void sameIssuedAtAsNotBeforeIsActiveKEYCLOAK10013() {
		KeycloakDeployment keycloakDeployment = new KeycloakDeployment();
		keycloakDeployment.setNotBefore(5000);

		TokenMetadataRepresentation token = new TokenMetadataRepresentation();
		token.setActive(true);
		token.issuedAt(4999);

		RefreshableKeycloakSecurityContext sut = new RefreshableKeycloakSecurityContext(keycloakDeployment,null,null,token,null, null, null);

		assertFalse(sut.isActive());

		token.issuedAt(5000);
		assertTrue(sut.isActive());
	}
}
