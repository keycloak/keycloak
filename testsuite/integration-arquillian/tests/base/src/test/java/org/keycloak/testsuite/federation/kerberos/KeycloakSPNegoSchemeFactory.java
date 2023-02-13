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

package org.keycloak.testsuite.federation.kerberos;

import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.Credentials;
import org.apache.http.impl.auth.SPNegoScheme;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.federation.kerberos.impl.KerberosUsernamePasswordAuthenticator;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import java.security.PrivilegedExceptionAction;

/**
 * Usable for testing only. Username and password are shared for the whole factory
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeycloakSPNegoSchemeFactory extends SPNegoSchemeFactory {

    private final CommonKerberosConfig kerberosConfig;

    private String username;
    private String password;


    public KeycloakSPNegoSchemeFactory(CommonKerberosConfig kerberosConfig) {
        super(true, false);
        this.kerberosConfig = kerberosConfig;
    }


    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }


    @Override
    @SuppressWarnings("deprecation")
    public AuthScheme newInstance(HttpParams params) {
        return new KeycloakSPNegoScheme(isStripPort(), isUseCanonicalHostname());
    }

    @Override
    public AuthScheme create(HttpContext context) {
        return new KeycloakSPNegoScheme(isStripPort(), isUseCanonicalHostname());
    }

    public class KeycloakSPNegoScheme extends SPNegoScheme {

        public KeycloakSPNegoScheme(boolean stripPort, boolean useCanonicalHostname) {
            super(stripPort, useCanonicalHostname);
        }


        @Override
        protected byte[] generateGSSToken(byte[] input, Oid oid, String authServer, Credentials credentials) throws GSSException {
            KerberosUsernamePasswordAuthenticator authenticator = new KerberosUsernamePasswordAuthenticator(kerberosConfig) {

                // Disable strict check for the configured kerberos realm, which is on super-method
                @Override
                protected String getKerberosPrincipal(String username) throws LoginException {
                    if (username.contains("@")) {
                        return username;
                    } else {
                        return username + "@" + config.getKerberosRealm();
                    }
                }
            };

            try {
                Subject clientSubject = authenticator.authenticateSubject(username, password);

                ByteArrayHolder holder = Subject.doAs(clientSubject, new ClientAcceptSecContext(input, oid, authServer));

                return holder.bytes;
            } catch (Exception le) {
                throw new RuntimeException(le);
            } finally {
                authenticator.logoutSubject();
            }
        }


        private class ClientAcceptSecContext implements PrivilegedExceptionAction<ByteArrayHolder> {

            private final byte[] input;
            private final Oid oid;
            private final String authServer;

            public ClientAcceptSecContext(byte[] input, Oid oid, String authServer) {
                this.input = input;
                this.oid = oid;
                this.authServer = authServer;
            }


            @Override
            public ByteArrayHolder run() throws Exception {
                byte[] token = input;
                if (token == null) {
                    token = new byte[0];
                }
                GSSManager manager = getManager();
                String httPrincipal = kerberosConfig.getServerPrincipal().replaceFirst("/.*@", "/" + authServer + "@");
                GSSName serverName = manager.createName(httPrincipal, null);
                GSSContext gssContext = manager.createContext(
                        serverName.canonicalize(oid), oid, null, GSSContext.DEFAULT_LIFETIME);
                gssContext.requestMutualAuth(true);
                gssContext.requestCredDeleg(true);
                byte[] outputToken = gssContext.initSecContext(token, 0, token.length);

                ByteArrayHolder result = new ByteArrayHolder();
                result.bytes = outputToken;
                return result;
            }

        }


        private class ByteArrayHolder {
            private byte[] bytes;
        }
    }
}
