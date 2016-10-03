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

package org.keycloak.example.kerberos;

import org.ietf.jgss.GSSCredential;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.KerberosSerializationUtils;
import org.keycloak.representations.AccessToken;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.security.sasl.Sasl;
import javax.servlet.http.HttpServletRequest;
import java.util.Hashtable;

/**
 * Sample client able to authenticate against ApacheDS LDAP server with Krb5 GSS Credential.
 *
 * Credential was previously retrieved from SPNEGO authentication against Keycloak auth-server and transmitted from
 * Keycloak to the application in OIDC access token
 *
 * We can use GSSCredential to further GSS API calls . Note that if you will use GSS API directly, you can
 * attach GSSCredential when creating GSSContext like this:
 * GSSContext context = gssManager.createContext(serviceName, KerberosSerializationUtils.KRB5_OID, deserializedGssCredential, GSSContext.DEFAULT_LIFETIME);
 *
 * In this example we authenticate against LDAP server, which calls GSS API under the hood when credential is attached to env under Sasl.CREDENTIALS key
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GSSCredentialsClient {

    public static LDAPUser getUserFromLDAP(HttpServletRequest req) throws Exception {
        KeycloakPrincipal keycloakPrincipal = (KeycloakPrincipal) req.getUserPrincipal();
        AccessToken accessToken = keycloakPrincipal.getKeycloakSecurityContext().getToken();
        String username = accessToken.getPreferredUsername();

        // Retrieve kerberos credential from accessToken and deserialize it
        String serializedGssCredential = (String) accessToken.getOtherClaims().get(KerberosConstants.GSS_DELEGATION_CREDENTIAL);
        GSSCredential deserializedGssCredential = KerberosSerializationUtils.deserializeCredential(serializedGssCredential);

        // First try to invoke without gssCredential. It should fail. This is here just for illustration purposes
        try {
            invokeLdap(null, username);
            throw new RuntimeException("Not expected to authenticate to LDAP without credential");
        } catch (NamingException nse) {
            System.out.println("GSSCredentialsClient: Expected exception: " + nse.getMessage());
        }

        return invokeLdap(deserializedGssCredential, username);
    }

    private static LDAPUser invokeLdap(GSSCredential gssCredential, String username) throws NamingException {
        Hashtable env = new Hashtable(11);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://localhost:10389");

        if (gssCredential != null) {
            env.put(Context.SECURITY_AUTHENTICATION, "GSSAPI");
            env.put(Sasl.CREDENTIALS, gssCredential);
        }

        DirContext ctx = new InitialDirContext(env);
        try {
            Attributes attrs = ctx.getAttributes("uid=" + username + ",ou=People,dc=keycloak,dc=org");
            String uid = username;
            String cn = (String) attrs.get("cn").get();
            String sn = (String) attrs.get("sn").get();
            return new LDAPUser(uid, cn, sn);
        } finally {
            ctx.close();
        }
    }

    public static class LDAPUser {

        private final String uid;
        private final String cn;
        private final String sn;

        public LDAPUser(String uid, String cn, String sn) {
            this.uid = uid;
            this.cn = cn;
            this.sn = sn;
        }

        public String getUid() {
            return uid;
        }

        public String getCn() {
            return cn;
        }

        public String getSn() {
            return sn;
        }
    }
}
