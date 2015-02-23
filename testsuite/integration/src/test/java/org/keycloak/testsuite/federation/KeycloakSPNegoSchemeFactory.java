package org.keycloak.testsuite.federation;

import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;

import org.apache.http.auth.AuthScheme;
import org.apache.http.impl.auth.SPNegoScheme;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.params.HttpParams;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.federation.kerberos.impl.KerberosUsernamePasswordAuthenticator;

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
        super(true);
        this.kerberosConfig = kerberosConfig;
    }


    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }


    @Override
    public AuthScheme newInstance(HttpParams params) {
        return new KeycloakSPNegoScheme(isStripPort());
    }


    public class KeycloakSPNegoScheme extends SPNegoScheme {

        public KeycloakSPNegoScheme(boolean stripPort) {
            super(stripPort);
        }


        @Override
        protected byte[] generateGSSToken(byte[] input, Oid oid, String authServer) throws GSSException {
            KerberosUsernamePasswordAuthenticator authenticator = new KerberosUsernamePasswordAuthenticator(kerberosConfig);
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
                byte[] outputToken = KeycloakSPNegoScheme.super.generateGSSToken(input, oid, authServer);

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
