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

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.Oid;
import org.jboss.logging.Logger;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.KerberosSerializationUtils;
import org.keycloak.federation.kerberos.CommonKerberosConfig;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;
import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;
import java.util.Set;

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
    private KerberosTicket kerberosTicket;
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

            // kerberosTicketis available in IBM JDK in case that GSSContext supports delegated credentials
            Set<KerberosTicket> kerberosTickets = serverSubject.getPrivateCredentials(KerberosTicket.class);
            Iterator<KerberosTicket> iterator = kerberosTickets.iterator();
            if (iterator.hasNext()) {
                kerberosTicket = iterator.next();
            }

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
            return KerberosSerializationUtils.serializeCredential(kerberosTicket, delegationCredential);
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

        Oid[] supportedMechs = new Oid[] { KerberosConstants.KRB5_OID, KerberosConstants.SPNEGO_OID };
        GSSCredential gssCredential = manager.createCredential(null, GSSCredential.INDEFINITE_LIFETIME, supportedMechs, GSSCredential.ACCEPT_ONLY);
        GSSContext gssContext = manager.createContext(gssCredential);

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
