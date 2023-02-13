package org.keycloak.adapters;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

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
	
	private AccessToken createSimpleToken() {
		AccessToken token = new AccessToken();
		token.id("111");
		token.issuer("http://localhost:8080/auth/acme");
		token.addAccess("foo").addRole("admin");
		token.addAccess("bar").addRole("user");
		return token;
	}

	@Test
	public void testSerialization() throws Exception {
		AccessToken token = createSimpleToken();
		IDToken idToken = new IDToken();

		idToken.setEmail("joe@email.cz");

		KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

		String encoded = new JWSBuilder()
				.jsonContent(token)
				.rsa256(keyPair.getPrivate());
		String encodedIdToken = new JWSBuilder()
				.jsonContent(idToken)
				.rsa256(keyPair.getPrivate());

		KeycloakDeployment keycloakDeployment = new KeycloakDeployment();
		keycloakDeployment.setNotBefore(5000);
		
		KeycloakSecurityContext ctx = new RefreshableKeycloakSecurityContext(keycloakDeployment,null, encoded, token,encodedIdToken, null, null);
		KeycloakPrincipal principal = new KeycloakPrincipal("joe", ctx);

		// Serialize
		ByteArrayOutputStream bso = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bso);
		oos.writeObject(principal);
		oos.close();

		// Deserialize
		byte[] bytes = bso.toByteArray();
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bis);
		principal = (KeycloakPrincipal)ois.readObject();
		ctx = principal.getKeycloakSecurityContext();
		token = ctx.getToken();
		idToken = ctx.getIdToken();

		System.out.println("Size of serialized principal: " + bytes.length);

		Assert.assertEquals(encoded, ctx.getTokenString());
		Assert.assertEquals(encodedIdToken, ctx.getIdTokenString());
		Assert.assertEquals("111", token.getId());
		Assert.assertEquals("111", token.getId());
		Assert.assertTrue(token.getResourceAccess("foo").isUserInRole("admin"));
		Assert.assertTrue(token.getResourceAccess("bar").isUserInRole("user"));
		Assert.assertEquals("joe@email.cz", idToken.getEmail());
		Assert.assertEquals("acme", ctx.getRealm());
		ois.close();
	}
}
