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

package org.keycloak.federation.kerberos.impl;

import org.junit.Assert;
import org.junit.Test;
import java.lang.reflect.Field;
import org.keycloak.federation.kerberos.impl.SPNEGOAuthenticator;

/**
 * @author <a href="mailto:hokuda@redhat.com">Hisanobu Okuda</a>
 */
public class SPNEGOAuthenticatorTest {

    @Test
    public void testAuthenticatedUsername() throws Exception {
        // KEYCLOAK-3842 SPNEGO: Support for multiple kerberos realms
        // SPNEGOAuthenticator#getAuthenticatedUsername() should not check realm name
        String username = "testuser";
        String realm = "TEST.ORG";
        String usernameWithRealm = username + "@" + realm;
        
        SPNEGOAuthenticator authenticator = new SPNEGOAuthenticator(null, null, null);

        Class cls = authenticator.getClass();
        String fieldname = "authenticatedKerberosPrincipal";
        Field authenticatedKerberosPrincipal = cls.getDeclaredField(fieldname);
        authenticatedKerberosPrincipal.setAccessible(true);
        authenticatedKerberosPrincipal.set(authenticator, usernameWithRealm);
        
        String actual = authenticator.getAuthenticatedUsername();
        String expected = username;
        Assert.assertEquals(expected, actual);
    }
}
