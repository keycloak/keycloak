/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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


    @Test
    public void testTokenWithoutResourceAccess() throws Exception {
        AccessToken token = new AccessToken();
        token.id("111");
        token.issuer("http://localhost:8080/auth/acme");

        String json = JsonSerialization.writeValueAsString(token);

        // Assert JSON doesn't contain "realm_access" or "resource_access" fields as it doesn't have any roles specified
        Assert.assertFalse(json.contains("realm_access"));
        Assert.assertFalse(json.contains("resource_access"));

        token = JsonSerialization.readValue(json, AccessToken.class);

        Assert.assertNull(token.getRealmAccess());
        Assert.assertTrue(token.getResourceAccess() != null && token.getResourceAccess().isEmpty());
        Assert.assertNull(token.getResourceAccess("foo"));
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
    public void testZipException() throws Exception {
        // KEYCLOAK-2479
        // Example of LogoutAction, which shows the exception to STDERR during Base64.decode . Need to use flag DONT_GUNZIP to avoid it.
        String logoutAction = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6ImUwYmRmMjQyLWJjZGItNGVjMy1hMGU4LTNjN2YyOTUzOTk5MC0xNDU1NzgyNTU2NjAyIiwiZXhwaXJhdGlvbiI6MTQ1NTc4MjU4NiwicmVzb3VyY2UiOiJwcm9kdWN0LXBvcnRhbCIsImFjdGlvbiI6IkxPR09VVCIsImFkYXB0ZXJTZXNzaW9uSWRzIjpbImx2c0oxNUpSX01XUE13aTIwbWRhTkJFRVZQZzQtMTkzVUZKem42M1EiXSwibm90QmVmb3JlIjowLCJrZXljbG9ha1Nlc3Npb25JZHMiOlsiOThkNWE3YTYtYjNmNi00ZTg3LWI5OTktOTg1N2YzMDRiZjY4Il19.H4vo7YXW8oQgYsIo9VPYeSsp1jXJR0TwJUwmiXjQJSyxFoKhHgIh3Y63ldVUeBRppxX9xhjOdYEckeppAn-1XnNxUmbExXWXirRIw8tiEtUPPCPztdkKsM0y6xWRd3Sjgg4fWB_1sMn6EWvCAvO7ahs6Rbb2Vo18nlHfxYRSTWw";
        JWSInput input = new JWSInput(logoutAction);
    }
}
