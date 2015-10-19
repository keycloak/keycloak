package org.keycloak.federation.kerberos.impl;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;

import org.keycloak.common.util.Base64;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.jboss.logging.Logger;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.common.util.KerberosSerializationUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SPNEGOAuthenticator {

    private static final Logger log = Logger.getLogger(SPNEGOAuthenticator.class);

    private final KerberosServerSubjectAuthenticator kerberosSubjectAuthenticator;
    private final String spnegoToken;
    private final CommonKerberosConfig kerberosConfig;

    private boolean authenticated = false;
    private String authenticatedKerberosPrincipal = null;
    private GSSCredential delegationCredential;
    private String responseToken = null;

    public SPNEGOAuthenticator(CommonKerberosConfig kerberosConfig, KerberosServerSubjectAuthenticator kerberosSubjectAuthenticator, String spnegoToken) {
        this.kerberosConfig = kerberosConfig;
        this.kerberosSubjectAuthenticator = kerberosSubjectAuthenticator;
        this.spnegoToken = spnegoToken;
    }

    public void authenticate() {
        if (log.isTraceEnabled()) {
            log.trace("SPNEGO Login with token: " + spnegoToken);
        }

        try {
            Subject serverSubject = kerberosSubjectAuthenticator.authenticateServerSubject();
            authenticated = Subject.doAs(serverSubject, new AcceptSecContext());
        } catch (Exception e) {
            log.warn("SPNEGO login failed", e);
        } finally {
            kerberosSubjectAuthenticator.logoutServerSubject();
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getResponseToken() {
        return responseToken;
    }

    public String getSerializedDelegationCredential() {
        if (delegationCredential == null) {
            if (log.isTraceEnabled()) {
                log.trace("No delegation credential available.");
            }

            return null;
        }

        try {
            if (log.isTraceEnabled()) {
                log.trace("Serializing credential " + delegationCredential);
            }
            return KerberosSerializationUtils.serializeCredential(delegationCredential);
        } catch (KerberosSerializationUtils.KerberosSerializationException kse) {
            log.warn("Couldn't serialize credential: " + delegationCredential, kse);
            return null;
        }
    }

    /**
     * @return username to be used in Keycloak. Username is authenticated kerberos principal without realm name
     */
    public String getAuthenticatedUsername() {
        String[] tokens = authenticatedKerberosPrincipal.split("@");
        String username = tokens[0];
        if (!tokens[1].equalsIgnoreCase(kerberosConfig.getKerberosRealm())) {
            throw new IllegalStateException("Invalid kerberos realm. Realm from the ticket: " + tokens[1] + ", configured realm: " + kerberosConfig.getKerberosRealm());
        }
        return username;
    }


    private class AcceptSecContext implements PrivilegedExceptionAction<Boolean> {

        @Override
        public Boolean run() throws Exception {
            GSSContext gssContext = null;
            try {
                if (log.isTraceEnabled()) {
                    log.trace("Going to establish security context");
                }

                gssContext = establishContext();
                logAuthDetails(gssContext);

                if (gssContext.isEstablished()) {
                    if (gssContext.getSrcName() == null) {
                        log.warn("GSS Context accepted, but no context initiator recognized. Check your kerberos configuration and reverse DNS lookup configuration");
                        return false;
                    }

                    authenticatedKerberosPrincipal = gssContext.getSrcName().toString();

                    if (gssContext.getCredDelegState()) {
                        delegationCredential = gssContext.getDelegCred();
                    }

                    return true;
                } else {
                    return false;
                }
            } finally {
                if (gssContext != null) {
                    gssContext.dispose();
                }
            }
        }

    }


    protected GSSContext establishContext() throws GSSException, IOException {
        GSSManager manager = GSSManager.getInstance();
        GSSContext gssContext = manager.createContext((GSSCredential) null);

        byte[] inputToken = Base64.decode(spnegoToken);
        byte[] respToken = gssContext.acceptSecContext(inputToken, 0, inputToken.length);
        responseToken = Base64.encodeBytes(respToken);

        return gssContext;
    }


    protected void logAuthDetails(GSSContext gssContext) throws GSSException {
        if (log.isDebugEnabled()) {
            String message = new StringBuilder("SPNEGO Security context accepted with token: " + responseToken)
                    .append(", established: ").append(gssContext.isEstablished())
                    .append(", credDelegState: ").append(gssContext.getCredDelegState())
                    .append(", mutualAuthState: ").append(gssContext.getMutualAuthState())
                    .append(", lifetime: ").append(gssContext.getLifetime())
                    .append(", confState: ").append(gssContext.getConfState())
                    .append(", integState: ").append(gssContext.getIntegState())
                    .append(", srcName: ").append(gssContext.getSrcName())
                    .append(", targName: ").append(gssContext.getTargName())
                    .toString();
            log.debug(message);
        }
    }

}
