package org.keycloak.protocol.saml;

import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.common.exceptions.ConfigurationException;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.identity.federation.api.saml.v2.response.SAML2Response;
import org.picketlink.identity.federation.core.saml.v2.common.IDGenerator;
import org.picketlink.identity.federation.core.saml.v2.factories.JBossSAMLAuthnResponseFactory;
import org.picketlink.identity.federation.core.saml.v2.holders.IDPInfoHolder;
import org.picketlink.identity.federation.core.saml.v2.holders.IssuerInfoHolder;
import org.picketlink.identity.federation.core.saml.v2.holders.SPInfoHolder;
import org.picketlink.identity.federation.saml.v2.protocol.ResponseType;
import org.w3c.dom.Document;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SAML2ErrorResponseBuilder extends SAML2BindingBuilder<SAML2ErrorResponseBuilder> {

    public Document buildDocument(String status) throws ProcessingException {
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

        encryptAndSign(samlResponse);
        return samlResponse;
    }

    public BindingBuilder binding(String status) throws ConfigurationException, ProcessingException {

        Document samlResponseDocument = buildDocument(status);

        return new BindingBuilder(samlResponseDocument);
    }

}
