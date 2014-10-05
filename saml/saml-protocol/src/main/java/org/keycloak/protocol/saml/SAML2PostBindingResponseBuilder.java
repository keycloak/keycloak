package org.keycloak.protocol.saml;
/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

import org.picketlink.common.PicketLinkLogger;
import org.picketlink.common.PicketLinkLoggerFactory;
import org.picketlink.common.constants.GeneralConstants;
import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.common.exceptions.ConfigurationException;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.common.util.DocumentUtil;
import org.picketlink.identity.federation.api.saml.v2.response.SAML2Response;
import org.picketlink.identity.federation.api.saml.v2.sig.SAML2Signature;
import org.picketlink.identity.federation.core.saml.v2.common.IDGenerator;
import org.picketlink.identity.federation.core.saml.v2.factories.JBossSAMLAuthnResponseFactory;
import org.picketlink.identity.federation.core.saml.v2.holders.IDPInfoHolder;
import org.picketlink.identity.federation.core.saml.v2.holders.IssuerInfoHolder;
import org.picketlink.identity.federation.core.saml.v2.holders.SPInfoHolder;
import org.picketlink.identity.federation.core.saml.v2.util.StatementUtil;
import org.picketlink.identity.federation.core.saml.v2.util.XMLTimeUtil;
import org.picketlink.identity.federation.saml.v2.assertion.AssertionType;
import org.picketlink.identity.federation.saml.v2.assertion.AttributeStatementType;
import org.picketlink.identity.federation.saml.v2.assertion.AuthnStatementType;
import org.picketlink.identity.federation.saml.v2.protocol.ResponseType;
import org.picketlink.identity.federation.web.util.PostBindingUtil;
import org.w3c.dom.Document;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.picketlink.common.util.StringUtil.isNotNull;

/**
 * <p> Handles for dealing with SAML2 Authentication </p>
 * <p/>
 * Configuration Options:
 *
 * @author Anil.Saldhana@redhat.com
*/
public class SAML2PostBindingResponseBuilder {
    protected static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    protected List<String> roles = new LinkedList<String>();
    protected String userPrincipal;
    protected boolean multiValuedRoles;
    protected boolean disableAuthnStatement;
    protected String requestID;
    protected String responseIssuer;
    protected String authMethod;
    protected String relayState;
    protected String destination;
    protected String requestIssuer;
    protected Map<String, Object> attributes = new HashMap<String, Object>();


    public SAML2PostBindingResponseBuilder attributes(Map<String, Object> attributes) {
        this.attributes = attributes;
        return this;
    }

    public SAML2PostBindingResponseBuilder attribute(String name, Object value) {
        this.attributes.put(name, value);
        return this;
    }

    public SAML2PostBindingResponseBuilder requestID(String requestID) {
        this.requestID =requestID;
        return this;
    }

    public SAML2PostBindingResponseBuilder requestIssuer(String requestIssuer) {
        this.requestIssuer =requestIssuer;
        return this;
    }

    public SAML2PostBindingResponseBuilder responseIssuer(String issuer) {
        this.responseIssuer = issuer;
        return this;
    }

    public SAML2PostBindingResponseBuilder roles(List<String> roles) {
        this.roles = roles;
        return this;
    }

    public SAML2PostBindingResponseBuilder roles(String... roles) {
        for (String role : roles) {
            this.roles.add(role);
        }
        return this;
    }

    public SAML2PostBindingResponseBuilder authMethod(String authMethod) {
        this.authMethod = authMethod;
        return this;
    }

    public SAML2PostBindingResponseBuilder userPrincipal(String userPrincipal) {
        this.userPrincipal = userPrincipal;
        return this;
    }

    public SAML2PostBindingResponseBuilder relayState(String relayState) {
        this.relayState = relayState;
        return this;
    }

    public SAML2PostBindingResponseBuilder destination(String destination) {
        this.destination = destination;
        return this;
    }

    public SAML2PostBindingResponseBuilder multiValuedRoles(boolean multiValuedRoles) {
        this.multiValuedRoles = multiValuedRoles;
        return this;
    }

    public SAML2PostBindingResponseBuilder disableAuthnStatement(boolean disableAuthnStatement) {
        this.disableAuthnStatement = disableAuthnStatement;
        return this;
    }

    public Response error(String status)  throws ConfigurationException, ProcessingException, IOException {
        Document doc = getErrorResponse(status);
        return buildResponse(doc);


    }

    public Document getErrorResponse(String status) {
        Document samlResponse = null;
        ResponseType responseType = null;

        SAML2Response saml2Response = new SAML2Response();

        // Create a response type
        String id = IDGenerator.create("ID_");

        IssuerInfoHolder issuerHolder = new IssuerInfoHolder(responseIssuer);
        issuerHolder.setStatusCode(status);

        IDPInfoHolder idp = new IDPInfoHolder();
        idp.setNameIDFormatValue(null);
        idp.setNameIDFormat(JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get());

        SPInfoHolder sp = new SPInfoHolder();
        sp.setResponseDestinationURI(destination);

        responseType = saml2Response.createResponseType(id);
        responseType.setStatus(JBossSAMLAuthnResponseFactory.createStatusTypeForResponder(status));
        responseType.setDestination(destination);

        // Lets see how the response looks like
        if (logger.isTraceEnabled()) {
            StringWriter sw = new StringWriter();
            try {
                saml2Response.marshall(responseType, sw);
            } catch (ProcessingException e) {
                logger.trace(e);
            }
            logger.trace("SAML Response Document: " + sw.toString());
        }

        /*
        if (supportSignature) {
            try {
                SAML2Signature ss = new SAML2Signature();
                samlResponse = ss.sign(responseType, keyManager.getSigningKeyPair());
            } catch (Exception e) {
                logger.trace(e);
                throw new RuntimeException(logger.signatureError(e));
            }
        } else
            try {
                samlResponse = saml2Response.convert(responseType);
            } catch (Exception e) {
                logger.trace(e);
            }
            */

        return samlResponse;
    }

    public Response build() throws ConfigurationException, ProcessingException, IOException {
        Document responseDoc = getResponse();
        return buildResponse(responseDoc);
    }

    protected Response buildResponse(Document responseDoc) throws ProcessingException, ConfigurationException, IOException {
        byte[] responseBytes = DocumentUtil.getDocumentAsString(responseDoc).getBytes("UTF-8");
        String samlResponse = PostBindingUtil.base64Encode(new String(responseBytes));

        if (destination == null) {
            throw logger.nullValueError("Destination is null");
        }

        StringBuilder builder = new StringBuilder();

        String key = GeneralConstants.SAML_RESPONSE_KEY;
        builder.append("<HTML>");
        builder.append("<HEAD>");

        builder.append("<TITLE>HTTP Post Binding Response (Response)</TITLE>");
        builder.append("</HEAD>");
        builder.append("<BODY Onload=\"document.forms[0].submit()\">");

        builder.append("<FORM METHOD=\"POST\" ACTION=\"" + destination + "\">");
        builder.append("<INPUT TYPE=\"HIDDEN\" NAME=\"" + key + "\"" + " VALUE=\"" + samlResponse + "\"/>");

        if (isNotNull(relayState)) {
            builder.append("<INPUT TYPE=\"HIDDEN\" NAME=\"RelayState\" " + "VALUE=\"" + relayState + "\"/>");
        }

        builder.append("<NOSCRIPT>");
        builder.append("<P>JavaScript is disabled. We strongly recommend to enable it. Click the button below to continue.</P>");
        builder.append("<INPUT TYPE=\"SUBMIT\" VALUE=\"CONTINUE\" />");
        builder.append("</NOSCRIPT>");

        builder.append("</FORM></BODY></HTML>");

        String str = builder.toString();

        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        return Response.ok(str, MediaType.TEXT_HTML_TYPE)
                       .header("Pragma", "no-cache")
                       .header("Cache-Control", "no-cache, no-store").build();
    }

    public Document getResponse() throws ConfigurationException, ProcessingException {

        Document samlResponseDocument = null;

        ResponseType responseType = null;

        SAML2Response saml2Response = new SAML2Response();

        // Create a response type
        String id = IDGenerator.create("ID_");

        IssuerInfoHolder issuerHolder = new IssuerInfoHolder(responseIssuer);
        issuerHolder.setStatusCode(JBossSAMLURIConstants.STATUS_SUCCESS.get());

        IDPInfoHolder idp = new IDPInfoHolder();
        idp.setNameIDFormatValue(userPrincipal);
        idp.setNameIDFormat(JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get());

        SPInfoHolder sp = new SPInfoHolder();
        sp.setResponseDestinationURI(destination);
        sp.setRequestID(requestID);
        sp.setIssuer(requestIssuer);
        responseType = saml2Response.createResponseType(id, sp, idp, issuerHolder);

        // Add information on the roles
        AssertionType assertion = responseType.getAssertions().get(0).getAssertion();

        // Create an AuthnStatementType
        if (!disableAuthnStatement) {
            String authContextRef = JBossSAMLURIConstants.AC_PASSWORD.get();
            if (isNotNull(authMethod))
                authContextRef = authMethod;

            AuthnStatementType authnStatement = StatementUtil.createAuthnStatement(XMLTimeUtil.getIssueInstant(),
                    authContextRef);

            authnStatement.setSessionIndex(assertion.getID());

            assertion.addStatement(authnStatement);
        }

        if (roles != null && !roles.isEmpty()) {
            AttributeStatementType attrStatement = StatementUtil.createAttributeStatementForRoles(roles, multiValuedRoles);
            assertion.addStatement(attrStatement);
        }

        // Add in the attributes information
        if (attributes != null && attributes.size() > 0) {
            AttributeStatementType attStatement = StatementUtil.createAttributeStatement(attributes);
            assertion.addStatement(attStatement);
        }

        try {
            samlResponseDocument = saml2Response.convert(responseType);

            if (logger.isTraceEnabled()) {
                logger.trace("SAML Response Document: " + DocumentUtil.asString(samlResponseDocument));
            }
        } catch (Exception e) {
            throw logger.samlAssertionMarshallError(e);
        }

        return samlResponseDocument;
    }
}
