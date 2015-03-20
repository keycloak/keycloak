package org.keycloak.protocol.saml;

import org.picketlink.common.PicketLinkLogger;
import org.picketlink.common.PicketLinkLoggerFactory;
import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.common.exceptions.ConfigurationException;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.common.util.DocumentUtil;
import org.picketlink.identity.federation.api.saml.v2.response.SAML2Response;
import org.picketlink.identity.federation.core.saml.v2.common.IDGenerator;
import org.picketlink.identity.federation.core.saml.v2.holders.IDPInfoHolder;
import org.picketlink.identity.federation.core.saml.v2.holders.IssuerInfoHolder;
import org.picketlink.identity.federation.core.saml.v2.holders.SPInfoHolder;
import org.picketlink.identity.federation.core.saml.v2.util.StatementUtil;
import org.picketlink.identity.federation.core.saml.v2.util.XMLTimeUtil;
import org.picketlink.identity.federation.saml.v2.assertion.AssertionType;
import org.picketlink.identity.federation.saml.v2.assertion.AttributeStatementType;
import org.picketlink.identity.federation.saml.v2.assertion.AuthnStatementType;
import org.picketlink.identity.federation.saml.v2.protocol.ResponseType;
import org.w3c.dom.Document;

import java.util.LinkedList;
import java.util.List;

import static org.picketlink.common.util.StringUtil.isNotNull;

/**
 * <p> Handles for dealing with SAML2 Authentication </p>
 * <p/>
 * Configuration Options:
 *
 * @author bburke@redhat.com
*/
public class SALM2LoginResponseBuilder {
    protected static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    protected String destination;
    protected String issuer;
    protected String nameId;
    protected String nameIdFormat;
    protected boolean multiValuedRoles;
    protected boolean disableAuthnStatement;
    protected String requestID;
    protected String authMethod;
    protected String requestIssuer;
    protected String sessionIndex;


    public SALM2LoginResponseBuilder sessionIndex(String sessionIndex) {
        this.sessionIndex = sessionIndex;
        return this;
    }

    public SALM2LoginResponseBuilder destination(String destination) {
        this.destination = destination;
        return this;
    }

    public SALM2LoginResponseBuilder issuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public SALM2LoginResponseBuilder requestID(String requestID) {
        this.requestID =requestID;
        return this;
    }

    public SALM2LoginResponseBuilder requestIssuer(String requestIssuer) {
        this.requestIssuer =requestIssuer;
        return this;
    }

    public SALM2LoginResponseBuilder authMethod(String authMethod) {
        this.authMethod = authMethod;
        return this;
    }

    public SALM2LoginResponseBuilder nameIdentifier(String nameIdFormat, String nameId) {
        this.nameIdFormat = nameIdFormat;
        this.nameId = nameId;
        return this;
    }

    public SALM2LoginResponseBuilder multiValuedRoles(boolean multiValuedRoles) {
        this.multiValuedRoles = multiValuedRoles;
        return this;
    }

    public SALM2LoginResponseBuilder disableAuthnStatement(boolean disableAuthnStatement) {
        this.disableAuthnStatement = disableAuthnStatement;
        return this;
    }

    public Document buildDocument(ResponseType responseType) throws ConfigurationException, ProcessingException {
        Document samlResponseDocument = null;

        try {
            SAML2Response docGen = new SAML2Response();
            samlResponseDocument = docGen.convert(responseType);

            if (logger.isTraceEnabled()) {
                logger.trace("SAML Response Document: " + DocumentUtil.asString(samlResponseDocument));
            }
        } catch (Exception e) {
            throw logger.samlAssertionMarshallError(e);
        }

        return samlResponseDocument;
    }

    public ResponseType buildModel() throws ConfigurationException, ProcessingException {
        ResponseType responseType = null;

        SAML2Response saml2Response = new SAML2Response();

        // Create a response type
        String id = IDGenerator.create("ID_");

        IssuerInfoHolder issuerHolder = new IssuerInfoHolder(issuer);
        issuerHolder.setStatusCode(JBossSAMLURIConstants.STATUS_SUCCESS.get());

        IDPInfoHolder idp = new IDPInfoHolder();
        idp.setNameIDFormatValue(nameId);
        idp.setNameIDFormat(nameIdFormat);

        SPInfoHolder sp = new SPInfoHolder();
        sp.setResponseDestinationURI(destination);
        sp.setRequestID(requestID);
        sp.setIssuer(requestIssuer);
        responseType = saml2Response.createResponseType(id, sp, idp, issuerHolder);

        // Add information on the roles
        AssertionType assertion = responseType.getAssertions().get(0).getAssertion();

        // Create an AuthnStatementType
        if (!disableAuthnStatement) {
            String authContextRef = JBossSAMLURIConstants.AC_UNSPECIFIED.get();
            if (isNotNull(authMethod))
                authContextRef = authMethod;

            AuthnStatementType authnStatement = StatementUtil.createAuthnStatement(XMLTimeUtil.getIssueInstant(),
                    authContextRef);
            if (sessionIndex != null) authnStatement.setSessionIndex(sessionIndex);
            else authnStatement.setSessionIndex(assertion.getID());

            assertion.addStatement(authnStatement);
        }
        return responseType;
    }

}
