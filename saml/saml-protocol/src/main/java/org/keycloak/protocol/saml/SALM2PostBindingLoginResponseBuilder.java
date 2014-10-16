package org.keycloak.protocol.saml;

import org.picketlink.common.PicketLinkLogger;
import org.picketlink.common.PicketLinkLoggerFactory;
import org.picketlink.common.constants.GeneralConstants;
import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.common.exceptions.ConfigurationException;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.common.util.DocumentUtil;
import org.picketlink.identity.federation.api.saml.v2.response.SAML2Response;
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
 * @author bburke@redhat.com
*/
public class SALM2PostBindingLoginResponseBuilder extends SAML2PostBindingBuilder<SALM2PostBindingLoginResponseBuilder> {
    protected static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    protected List<String> roles = new LinkedList<String>();
    protected String userPrincipal;
    protected boolean multiValuedRoles;
    protected boolean disableAuthnStatement;
    protected String requestID;
    protected String authMethod;
    protected String requestIssuer;
    protected Map<String, Object> attributes = new HashMap<String, Object>();


    public SALM2PostBindingLoginResponseBuilder attributes(Map<String, Object> attributes) {
        this.attributes = attributes;
        return this;
    }

    public SALM2PostBindingLoginResponseBuilder attribute(String name, Object value) {
        this.attributes.put(name, value);
        return this;
    }

    public SALM2PostBindingLoginResponseBuilder requestID(String requestID) {
        this.requestID =requestID;
        return this;
    }

    public SALM2PostBindingLoginResponseBuilder requestIssuer(String requestIssuer) {
        this.requestIssuer =requestIssuer;
        return this;
    }

    public SALM2PostBindingLoginResponseBuilder roles(List<String> roles) {
        this.roles = roles;
        return this;
    }

    public SALM2PostBindingLoginResponseBuilder roles(String... roles) {
        for (String role : roles) {
            this.roles.add(role);
        }
        return this;
    }

    public SALM2PostBindingLoginResponseBuilder authMethod(String authMethod) {
        this.authMethod = authMethod;
        return this;
    }

    public SALM2PostBindingLoginResponseBuilder userPrincipal(String userPrincipal) {
        this.userPrincipal = userPrincipal;
        return this;
    }

   public SALM2PostBindingLoginResponseBuilder multiValuedRoles(boolean multiValuedRoles) {
        this.multiValuedRoles = multiValuedRoles;
        return this;
    }

    public SALM2PostBindingLoginResponseBuilder disableAuthnStatement(boolean disableAuthnStatement) {
        this.disableAuthnStatement = disableAuthnStatement;
        return this;
    }

    public Response buildLoginResponse() throws ConfigurationException, ProcessingException, IOException {
        Document responseDoc = getResponse();
        return buildResponse(responseDoc);
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

        encryptAndSign(samlResponseDocument);

        return samlResponseDocument;
    }

}
