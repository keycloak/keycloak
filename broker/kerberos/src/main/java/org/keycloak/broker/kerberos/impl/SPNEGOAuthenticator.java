package org.keycloak.broker.kerberos.impl;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;

import net.iharder.Base64;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.Oid;
import org.jboss.logging.Logger;
import org.keycloak.broker.kerberos.KerberosConstants;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SPNEGOAuthenticator {

    private static final Logger logger = Logger.getLogger(SPNEGOAuthenticator.class);

    private static final GSSManager GSS_MANAGER = GSSManager.getInstance();

    private final KerberosServerSubjectAuthenticator kerberosSubjectAuthenticator;
    private final String spnegoToken;

    private boolean authenticated = false;
    private String principal = null;
    private GSSCredential delegationCredential;
    private String responseToken = null;

    public SPNEGOAuthenticator(KerberosServerSubjectAuthenticator kerberosSubjectAuthenticator, String spnegoToken) {
        this.kerberosSubjectAuthenticator = kerberosSubjectAuthenticator;
        this.spnegoToken = spnegoToken;
    }

    public void authenticate() {
        // TODO: debug
        logger.info("SPNEGO Login with token: " + spnegoToken);

        try {
            Subject serverSubject = kerberosSubjectAuthenticator.authenticateServerSubject();
            authenticated = Subject.doAs(serverSubject, new AcceptSecContext());
        } catch (Exception e) {
            logger.warn("SPNEGO login failed: " + e.getMessage());

            // TODO: debug and check if it is shown in the log
            if (logger.isInfoEnabled()) {
                logger.info("SPNEGO login failed: " + e.getMessage(), e);
            }
        } finally {
            kerberosSubjectAuthenticator.logoutServerSubject();
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getPrincipal() {
        return principal;
    }

    public String getResponseToken() {
        return responseToken;
    }

    public GSSCredential getDelegationCredential() {
        return delegationCredential;
    }

    private class AcceptSecContext implements PrivilegedExceptionAction<Boolean> {

        @Override
        public Boolean run() throws Exception {
            GSSContext gssContext = null;
            try {
                // TODO: debug
                logger.info("Going to establish security context");
                gssContext = establishContext();
                logAuthDetails(gssContext);

                // What should be done with delegation credential? Figure out if there are use-cases for storing it as claims in FederatedIdentity
                if (gssContext.getCredDelegState()) {
                    delegationCredential = gssContext.getDelegCred();
                }

                if (gssContext.isEstablished()) {
                    principal = gssContext.getSrcName().toString();
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
        Oid spnegoOid = new Oid(KerberosConstants.SPNEGO_OID);
        GSSCredential credential = GSS_MANAGER.createCredential(null,
                GSSCredential.DEFAULT_LIFETIME,
                spnegoOid,
                GSSCredential.ACCEPT_ONLY);
        GSSContext gssContext = GSS_MANAGER.createContext(credential);

        byte[] inputToken = Base64.decode(spnegoToken);
        byte[] respToken = gssContext.acceptSecContext(inputToken, 0, inputToken.length);
        responseToken = Base64.encodeBytes(respToken);

        return gssContext;
    }

    protected void logAuthDetails(GSSContext gssContext) throws GSSException {

        // TODO: debug
        if (logger.isInfoEnabled()) {
            String message = new StringBuilder("SPNEGO Security context accepted with token: " + responseToken)
                    .append(", established: " + gssContext.isEstablished())
                    .append(", credDelegState: " + gssContext.getCredDelegState())
                    .append(", mutualAuthState: " + gssContext.getMutualAuthState())
                    .append(", lifetime: " + gssContext.getLifetime())
                    .append(", confState: " + gssContext.getConfState())
                    .append(", integState: " + gssContext.getIntegState())
                    .append(", srcName: " + gssContext.getSrcName())
                    .append(", targName: " + gssContext.getTargName())
                    .toString();
            logger.info(message);
        }
    }

}
