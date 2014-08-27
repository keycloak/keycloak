package org.keycloak;

import junit.framework.Assert;
import org.junit.Test;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.JsonSerialization;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SkeletonKeyTokenTest {
    @Test
    public void testToken() throws Exception {
        AccessToken token = new AccessToken();
        token.id("111");
        token.addAccess("foo").addRole("admin");
        token.addAccess("bar").addRole("user");

        String json = JsonSerialization.writeValueAsString(token);
        token = JsonSerialization.readValue(json, AccessToken.class);
        Assert.assertEquals("111", token.getId());
        AccessToken.Access foo = token.getResourceAccess("foo");
        Assert.assertNotNull(foo);
        Assert.assertTrue(foo.isUserInRole("admin"));

    }

    @Test
    public void testRSA() throws Exception {
        AccessToken token = new AccessToken();
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
}
