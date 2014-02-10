package org.keycloak;

import junit.framework.Assert;
import org.junit.Test;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.representations.SkeletonKeyScope;
import org.keycloak.representations.SkeletonKeyToken;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SkeletonKeyTokenTest {
    private static class Parser implements Runnable {
        private String json;

        private Parser(String json) {
            this.json = json;
        }

        @Override
        public void run() {
            for (int i = 0; i < 10000; i++) {
                try {
                    SkeletonKeyScope scope = JsonSerialization.readValue(json.getBytes(), SkeletonKeyScope.class);
                } catch (IOException e) {

                }
            }
        }
    }

    @Test
    public void testScope() throws Exception {
        SkeletonKeyScope scope2 = new SkeletonKeyScope();

        scope2.add("one", "admin");
        scope2.add("one", "buyer");
        scope2.add("two", "seller");
        String json = JsonSerialization.writeValueAsString(scope2);
        System.out.println(json);

        /*

        Thread[] threads = new Thread[1000];
        for (int i = 0; i < 1000; i++) {
            threads[i] = new Thread(new Parser(json));
        }
        long start = System.currentTimeMillis();
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        long end = System.currentTimeMillis() - start;
        System.out.println("Time took: " + end);
        */


    }

    @Test
    public void testToken() throws Exception {
        SkeletonKeyToken token = new SkeletonKeyToken();
        token.id("111");
        token.addAccess("foo").addRole("admin");
        token.addAccess("bar").addRole("user");

        String json = JsonSerialization.writeValueAsString(token);
        System.out.println(json);

        token = JsonSerialization.readValue(json, SkeletonKeyToken.class);
        Assert.assertEquals("111", token.getId());
        SkeletonKeyToken.Access foo = token.getResourceAccess("foo");
        Assert.assertNotNull(foo);
        Assert.assertTrue(foo.isUserInRole("admin"));

    }

    @Test
    public void testRSA() throws Exception {
        SkeletonKeyToken token = new SkeletonKeyToken();
        token.id("111");
        token.addAccess("foo").addRole("admin");
        token.addAccess("bar").addRole("user");

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        String encoded = new JWSBuilder()
                .jsonContent(token)
                .rsa256(keyPair.getPrivate());

        System.out.println(encoded);

        JWSInput input = new JWSInput(encoded);

        token = input.readJsonContent(SkeletonKeyToken.class);
        Assert.assertEquals("111", token.getId());
        Assert.assertTrue(RSAProvider.verify(input, keyPair.getPublic()));
    }
}
