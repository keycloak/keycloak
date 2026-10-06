/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.storage.ldap.idm.store.ldap;

import java.util.Hashtable;
import javax.naming.Context;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.LDAPConstants;
import org.keycloak.storage.ldap.LDAPConfig;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the admin connection environment built by {@link LDAPContextManager}.
 */
public class LDAPContextManagerTest {

    private static LDAPConfig ldapConfig(String authType, String bindDN, String connectionUrl) {
        MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>();
        if (authType != null) {
            cfg.add(LDAPConstants.AUTH_TYPE, authType);
        }
        if (bindDN != null) {
            cfg.add(LDAPConstants.BIND_DN, bindDN);
        }
        if (connectionUrl != null) {
            cfg.add(LDAPConstants.CONNECTION_URL, connectionUrl);
        }
        return new LDAPConfig(cfg);
    }

    @Test
    public void getNonAuthConnectionPropertiesHasNoBindCredentials() {
        LDAPConfig config = ldapConfig(LDAPConstants.AUTH_TYPE_SIMPLE, "uid=admin,dc=example,dc=org", "ldap://localhost:389");

        Hashtable<Object, Object> env = LDAPContextManager.getNonAuthConnectionProperties(config);

        // The base connection stays anonymous; credentials are added separately by the caller.
        Assert.assertFalse(env.containsKey(Context.SECURITY_AUTHENTICATION));
        Assert.assertFalse(env.containsKey(Context.SECURITY_PRINCIPAL));
        Assert.assertFalse(env.containsKey(Context.SECURITY_CREDENTIALS));
    }

    @Test
    public void setAuthConnectionPropertiesPutsBindCredentialsIntoInitialEnv() {
        LDAPConfig config = ldapConfig(LDAPConstants.AUTH_TYPE_SIMPLE, "uid=admin,dc=example,dc=org", "ldap://localhost:389");

        Hashtable<Object, Object> env = LDAPContextManager.getNonAuthConnectionProperties(config);
        LDAPContextManager.setAuthConnectionProperties(env, config, "theBindPassword");

        // Credentials present in the *initial* environment is what lets the JNDI pool reuse the bound
        // connection instead of re-binding the service account on every operation.
        Assert.assertEquals(LDAPConstants.AUTH_TYPE_SIMPLE, env.get(Context.SECURITY_AUTHENTICATION));
        Assert.assertEquals("uid=admin,dc=example,dc=org", env.get(Context.SECURITY_PRINCIPAL));
        Assert.assertEquals("theBindPassword", env.get(Context.SECURITY_CREDENTIALS));
    }

    @Test
    public void setAuthConnectionPropertiesDoesNotBindForAnonymousAuth() {
        LDAPConfig config = ldapConfig(LDAPConstants.AUTH_TYPE_NONE, null, "ldap://localhost:389");

        Hashtable<Object, Object> env = LDAPContextManager.getNonAuthConnectionProperties(config);
        LDAPContextManager.setAuthConnectionProperties(env, config, null);

        // Anonymous bind: no principal/credentials must be added.
        Assert.assertFalse(env.containsKey(Context.SECURITY_PRINCIPAL));
        Assert.assertFalse(env.containsKey(Context.SECURITY_CREDENTIALS));
    }
}
