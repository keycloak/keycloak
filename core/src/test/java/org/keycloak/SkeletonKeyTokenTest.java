package org.keycloak;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SkeletonKeyTokenTest {
    @Test
    public void testToken() throws Exception {
        AccessToken token = createSimpleToken();

        String json = JsonSerialization.writeValueAsString(token);
        token = JsonSerialization.readValue(json, AccessToken.class);
        Assert.assertEquals("111", token.getId());
        AccessToken.Access foo = token.getResourceAccess("foo");
        Assert.assertNotNull(foo);
        Assert.assertTrue(foo.isUserInRole("admin"));

    }

    @Test
    public void testRSA() throws Exception {
        AccessToken token = createSimpleToken();
        token.id("111");
        token.addAccess("foo").addRole("admin");
        token.addAccess("bar").addRole("user");

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        String encoded = new JWSBuilder()
                .jsonContent(token)
                .rsa256(keyPair.getPrivate());

        JWSInput input = new JWSInput(encoded);

        token = input.readJsonContent(AccessToken.class);
        Assert.assertEquals("111", token.getId());
        Assert.assertTrue(RSAProvider.verify(input, keyPair.getPublic()));
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

        KeycloakSecurityContext ctx = new KeycloakSecurityContext(encoded, token, encodedIdToken, idToken);
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

    private AccessToken createSimpleToken() {
        AccessToken token = new AccessToken();
        token.id("111");
        token.issuer("acme");
        token.addAccess("foo").addRole("admin");
        token.addAccess("bar").addRole("user");
        return token;
    }
}
