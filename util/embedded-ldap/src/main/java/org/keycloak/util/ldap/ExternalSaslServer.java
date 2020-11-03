/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

/* This file is originally copied from ApacheDS
 * directory-server/tree/master/protocol-ldap/src/main/java/org/apache/directory/server/ldap/handlers/sasl/external/certificate
 * and modified to support client certificate authentication for the admin user too
 */

package org.keycloak.util.ldap;


import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.OperationEnum;
import org.apache.directory.server.core.api.OperationManager;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.sasl.AbstractSaslServer;
import org.apache.directory.server.ldap.handlers.sasl.SaslConstants;
import org.apache.mina.filter.ssl.SslFilter;

import javax.naming.Context;
import javax.net.ssl.SSLSession;
import javax.security.sasl.SaslException;
import java.security.cert.Certificate;


/**
 * A SaslServer implementation for certificate based SASL EXTERNAL mechanism.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ExternalSaslServer extends AbstractSaslServer
{
    /**
     * The possible states for the negotiation of a EXTERNAL mechanism.
     */
    private enum NegotiationState
    {
        INITIALIZED, // Negotiation has just started
        COMPLETED // The user/password have been received
    }

    /** The current negotiation state */
    private NegotiationState state;

    /**
     *
     * Creates a new instance of ExternalSaslServer.
     *
     * @param ldapSession The associated LdapSession instance
     * @param adminSession The Administrator session
     * @param bindRequest The associated BindRequest object
     */
    ExternalSaslServer( LdapSession ldapSession, CoreSession adminSession, BindRequest bindRequest )
    {
        super( ldapSession, adminSession, bindRequest );
        state = NegotiationState.INITIALIZED;
    }


    /**
     * {@inheritDoc}
     */
    public String getMechanismName()
    {
        return SupportedSaslMechanisms.EXTERNAL;
    }


    /**
     * {@inheritDoc}
     */
    public byte[] evaluateResponse( byte[] initialResponse ) throws SaslException
    {
        try
        {
            SSLSession sslSession = ( SSLSession ) getLdapSession().getIoSession().getAttribute( SslFilter.SSL_SESSION );
            Certificate[] peerCertificates = sslSession.getPeerCertificates();

            if ( null == peerCertificates || 1 > peerCertificates.length )
            {
                throw new SaslException( "No peer certificate provided - cancel bind." );
            }

            getLdapSession().setCoreSession( authenticate( peerCertificates[0] ) );
            state = NegotiationState.COMPLETED;
        }
        catch ( Exception e )
        {
            throw new SaslException( "Error authentication using client certificate: " + ExceptionUtils.getStackTrace( e ), e );
        }

        return Strings.EMPTY_BYTES;
    }


    /**
     * Provides {@code true} if negationstate is {@link NegotiationState#COMPLETED}
     *
     * @return {@code true} if completed, otherwise {@code false}
     */
    public boolean isComplete()
    {
        return state == NegotiationState.COMPLETED;
    }


    /**
     * Try to authenticate the user against the underlying LDAP server.
     * We identify the user using the provided peercertificate.
     */
    private CoreSession authenticate( Certificate peerCertificate ) throws Exception
    {
        // search for client certificate from users
        CoreSession session = searchUserWithCertificate(peerCertificate, getLdapSession().getLdapServer().getSearchBaseDn());
        if (session != null) {
            return session;
        }

        // search for client certificate for admin user
        session = searchUserWithCertificate(peerCertificate, "uid=admin,ou=system");
        if (session != null) {
            return session;
        }

        throw new LdapAuthenticationException("Cannot authenticate user cert=" + peerCertificate);
    }

    private CoreSession searchUserWithCertificate(Certificate peerCertificate, String baseDn) throws Exception
    {
        LdapSession ldapSession = getLdapSession();
        CoreSession adminSession = getAdminSession();
        DirectoryService directoryService = adminSession.getDirectoryService();
        OperationManager operationManager = directoryService.getOperationManager();

        // find user by userCertificate
        EqualityNode<String> filter = new EqualityNode<>(
                directoryService.getSchemaManager().getAttributeType(SchemaConstants.USER_CERTIFICATE_AT),
                new Value(peerCertificate.getEncoded()));

        SearchOperationContext searchContext = new SearchOperationContext( directoryService.getAdminSession() );
        searchContext.setDn( directoryService.getDnFactory().create( baseDn ) );
        searchContext.setScope( SearchScope.SUBTREE );
        searchContext.setFilter( filter );
        searchContext.setSizeLimit( 1 );
        searchContext.setNoAttributes( true );

        try ( EntryFilteringCursor cursor = operationManager.search( searchContext ) )
        {
            if (cursor.next()) {
                Entry entry = cursor.get();

                BindOperationContext bindContext = new BindOperationContext(ldapSession.getCoreSession());
                bindContext.setDn(entry.getDn());
                bindContext.setSaslMechanism(getMechanismName());
                bindContext.setSaslAuthId(getBindRequest().getName());
                bindContext.setIoSession(ldapSession.getIoSession());
                bindContext.setInterceptors(directoryService.getInterceptors(OperationEnum.BIND));

                operationManager.bind(bindContext);

                ldapSession.putSaslProperty(SaslConstants.SASL_AUTHENT_USER, new LdapPrincipal(
                        directoryService.getSchemaManager(), entry.getDn(), AuthenticationLevel.STRONG));
                getLdapSession().putSaslProperty(Context.SECURITY_PRINCIPAL, getBindRequest().getName());

                return bindContext.getSession();
            }
        }

        return null;
    }
}
