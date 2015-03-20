package org.keycloak.broker.saml;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.VerificationException;
import org.keycloak.broker.provider.FederatedIdentity;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.saml.SAMLRequestParser;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlProtocolUtils;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.EventsManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.flows.Flows;
import org.picketlink.common.constants.GeneralConstants;
import org.picketlink.common.constants.JBossSAMLConstants;
import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.common.util.DocumentUtil;
import org.picketlink.common.util.StaxParserUtil;
import org.picketlink.identity.federation.api.saml.v2.response.SAML2Response;
import org.picketlink.identity.federation.core.parsers.saml.SAMLParser;
import org.picketlink.identity.federation.core.saml.v2.common.SAMLDocumentHolder;
import org.picketlink.identity.federation.core.util.JAXPValidationUtil;
import org.picketlink.identity.federation.core.util.XMLEncryptionUtil;
import org.picketlink.identity.federation.core.util.XMLSignatureUtil;
import org.picketlink.identity.federation.saml.v2.assertion.AssertionType;
import org.picketlink.identity.federation.saml.v2.assertion.AuthnStatementType;
import org.picketlink.identity.federation.saml.v2.assertion.EncryptedAssertionType;
import org.picketlink.identity.federation.saml.v2.assertion.NameIDType;
import org.picketlink.identity.federation.saml.v2.assertion.SubjectType;
import org.picketlink.identity.federation.saml.v2.protocol.ResponseType;
import org.picketlink.identity.federation.saml.v2.protocol.StatusResponseType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.namespace.QName;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SAMLEndpoint {
    protected static final Logger logger = Logger.getLogger(SAMLEndpoint.class);
    protected RealmModel realm;
    protected EventBuilder event;
    protected SAMLIdentityProviderConfig config;
    protected IdentityProvider.AuthenticationCallback callback;

    @Context
    private UriInfo uriInfo;

    @Context
    private KeycloakSession session;

    @Context
    private ClientConnection clientConnection;

    @Context
    private HttpRequest request;

    @Context
    private HttpHeaders headers;


    public SAMLEndpoint(RealmModel realm, SAMLIdentityProviderConfig config, IdentityProvider.AuthenticationCallback callback) {
        this.realm = realm;
        this.config = config;
        this.callback = callback;
    }

    @GET
    public Response redirectBinding(@QueryParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                    @QueryParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                    @QueryParam(GeneralConstants.RELAY_STATE) String relayState)  {
        return new RedirectBinding().execute(samlRequest, samlResponse, relayState);
    }


    /**
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postBinding(@FormParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                @FormParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                @FormParam(GeneralConstants.RELAY_STATE) String relayState) {
        return new PostBinding().execute(samlRequest, samlResponse, relayState);
    }

    protected abstract class Binding {
        private boolean checkSsl() {
            if (uriInfo.getBaseUri().getScheme().equals("https")) {
                return true;
            } else {
                return !realm.getSslRequired().isRequired(clientConnection);
            }
        }

        protected Response basicChecks(String samlRequest, String samlResponse) {
            if (!checkSsl()) {
                event.event(EventType.LOGIN);
                event.error(Errors.SSL_REQUIRED);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.HTTPS_REQUIRED);
            }
            if (!realm.isEnabled()) {
                event.event(EventType.LOGIN_ERROR);
                event.error(Errors.REALM_DISABLED);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.REALM_NOT_ENABLED);
            }

            if (samlRequest == null && samlResponse == null) {
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_REQUEST);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.INVALID_REQUEST );

            }
            return null;
        }

        protected abstract String getBindingType();
        protected abstract void verifySignature(SAMLDocumentHolder documentHolder) throws VerificationException;
        protected abstract SAMLDocumentHolder extractRequestDocument(String samlRequest);
        protected abstract SAMLDocumentHolder extractResponseDocument(String response);
        protected PublicKey getIDPKey() {
            X509Certificate certificate = null;
            try {
                certificate = XMLSignatureUtil.getX509CertificateFromKeyInfoString(config.getSigningCertificate().replaceAll("\\s", ""));
            } catch (ProcessingException e) {
                throw new RuntimeException(e);
            }
            return certificate.getPublicKey();
        }

        public Response execute(String samlRequest, String samlResponse, String relayState) {
            event = new EventsManager(realm, session, clientConnection).createEventBuilder();
            Response response = basicChecks(samlRequest, samlResponse);
            if (response != null) return response;
            if (samlRequest != null) throw new RuntimeException("NOT IMPLEMETED");//return handleSamlRequest(samlRequest, relayState);
            else return handleSamlResponse(samlResponse, relayState);
        }

        protected Response handleLoginResponse(String samlResponse, SAMLDocumentHolder holder, ResponseType responseType, String relayState) {
            if (config.isValidateSignature()) {
                try {
                    verifySignature(holder);
                } catch (VerificationException e) {
                    logger.error("validation failed", e);
                    event.event(EventType.LOGIN);
                    event.error(Errors.INVALID_SIGNATURE);
                    return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.INVALID_REQUESTER);
                }
            }

            try {
                AssertionType assertion = getAssertion(responseType);
                SubjectType subject = assertion.getSubject();
                SubjectType.STSubType subType = subject.getSubType();
                NameIDType subjectNameID = (NameIDType) subType.getBaseID();
                Map<String, String> notes = new HashMap<>();
                notes.put("SAML_FEDERATED_SUBJECT", subjectNameID.getValue());
                if (subjectNameID.getFormat() != null) notes.put("SAML_FEDERATED_SUBJECT_NAMEFORMAT", subjectNameID.getFormat().toString());
                FederatedIdentity identity = new FederatedIdentity(subjectNameID.getValue());

                identity.setUsername(subjectNameID.getValue());

                if (subjectNameID.getFormat().toString().equals(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get())) {
                    identity.setEmail(subjectNameID.getValue());
                }

                if (config.isStoreToken()) {
                    identity.setToken(samlResponse);
                }

                AuthnStatementType authn = null;
                for (Object statement : assertion.getStatements()) {
                    if (statement instanceof AuthnStatementType) {
                        authn = (AuthnStatementType)statement;
                        break;
                    }
                }
                if (authn != null && authn.getSessionIndex() != null) {
                    notes.put("SAML_FEDERATED_SESSION_INDEX", authn.getSessionIndex());
                }
                return callback.authenticated(notes, config, identity, relayState);

            } catch (Exception e) {
                throw new IdentityBrokerException("Could not process response from SAML identity provider.", e);
            }


        }

        private AssertionType getAssertion(ResponseType responseType) throws ProcessingException {
            List<ResponseType.RTChoiceType> assertions = responseType.getAssertions();

            if (assertions.isEmpty()) {
                throw new IdentityBrokerException("No assertion from response.");
            }

            ResponseType.RTChoiceType rtChoiceType = assertions.get(0);
            EncryptedAssertionType encryptedAssertion = rtChoiceType.getEncryptedAssertion();

            if (encryptedAssertion != null) {
                decryptAssertion(responseType, realm.getPrivateKey());

            }
            return responseType.getAssertions().get(0).getAssertion();
        }

        public Response handleSamlResponse(String samlResponse, String relayState) {
            SAMLDocumentHolder holder = extractResponseDocument(samlResponse);
            StatusResponseType statusResponse = (StatusResponseType)holder.getSamlObject();
            // validate destination
            if (!uriInfo.getAbsolutePath().toString().equals(statusResponse.getDestination())) {
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.error(Errors.INVALID_SAML_RESPONSE);
                event.detail(Details.REASON, "invalid_destination");
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.INVALID_REQUEST);
            }
            if (statusResponse instanceof ResponseType) {
                return handleLoginResponse(samlResponse, holder, (ResponseType)statusResponse, relayState);

            } else {
                // todo need to check that it is actually a LogoutResponse
                return handleLogoutResponse(holder, statusResponse, relayState);
            }
            //throw new RuntimeException("Unknown response type");

        }

        protected Response handleLogoutResponse(SAMLDocumentHolder holder, StatusResponseType responseType, String relayState) {
            if (config.isValidateSignature()) {
                try {
                    verifySignature(holder);
                } catch (VerificationException e) {
                    logger.error("logout response validation failed", e);
                    event.event(EventType.LOGOUT);
                    event.error(Errors.INVALID_SIGNATURE);
                    return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
                }
            }
            if (relayState == null) {
                logger.error("no valid user session");
                event.event(EventType.LOGOUT);
                event.error(Errors.USER_SESSION_NOT_FOUND);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
            }
            UserSessionModel userSession = session.sessions().getUserSession(realm, relayState);
            if (userSession == null) {
                logger.error("no valid user session");
                event.event(EventType.LOGOUT);
                event.error(Errors.USER_SESSION_NOT_FOUND);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
            }
            if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
                logger.error("usersession in different state");
                event.event(EventType.LOGOUT);
                event.error(Errors.USER_SESSION_NOT_FOUND);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.SESSION_NOT_ACTIVE);
            }
            return AuthenticationManager.finishBrowserLogout(session, realm, userSession, uriInfo, clientConnection, headers);
        }


        protected ResponseType decryptAssertion(ResponseType responseType, PrivateKey privateKey) throws ProcessingException {
            SAML2Response saml2Response = new SAML2Response();

            try {
                Document doc = saml2Response.convert(responseType);
                Element enc = DocumentUtil.getElement(doc, new QName(JBossSAMLConstants.ENCRYPTED_ASSERTION.get()));

                if (enc == null) {
                    throw new IdentityBrokerException("No encrypted assertion found.");
                }

                String oldID = enc.getAttribute(JBossSAMLConstants.ID.get());
                Document newDoc = DocumentUtil.createDocument();
                Node importedNode = newDoc.importNode(enc, true);
                newDoc.appendChild(importedNode);

                Element decryptedDocumentElement = XMLEncryptionUtil.decryptElementInDocument(newDoc, privateKey);
                SAMLParser parser = new SAMLParser();

                JAXPValidationUtil.checkSchemaValidation(decryptedDocumentElement);
                AssertionType assertion = (AssertionType) parser.parse(StaxParserUtil.getXMLEventReader(DocumentUtil
                        .getNodeAsStream(decryptedDocumentElement)));

                responseType.replaceAssertion(oldID, new ResponseType.RTChoiceType(assertion));

                return responseType;
            } catch (Exception e) {
                throw new IdentityBrokerException("Could not decrypt assertion.", e);
            }
        }


    }

    protected class PostBinding extends Binding {
        @Override
        protected void verifySignature(SAMLDocumentHolder documentHolder) throws VerificationException {
            SamlProtocolUtils.verifyDocumentSignature(documentHolder.getSamlDocument(), getIDPKey());
        }

        @Override
        protected SAMLDocumentHolder extractRequestDocument(String samlRequest) {
            return SAMLRequestParser.parseRequestPostBinding(samlRequest);
        }
        @Override
        protected SAMLDocumentHolder extractResponseDocument(String response) {
            return SAMLRequestParser.parseResponsePostBinding(response);
        }

        @Override
        protected String getBindingType() {
            return SamlProtocol.SAML_POST_BINDING;
        }
    }

    protected class RedirectBinding extends Binding {
        @Override
        protected void verifySignature(SAMLDocumentHolder documentHolder) throws VerificationException {
            PublicKey publicKey = getIDPKey();
            SamlProtocolUtils.verifyRedirectSignature(publicKey, uriInfo);
        }



        @Override
        protected SAMLDocumentHolder extractRequestDocument(String samlRequest) {
            return SAMLRequestParser.parseRequestRedirectBinding(samlRequest);
        }

        @Override
        protected SAMLDocumentHolder extractResponseDocument(String response) {
            return SAMLRequestParser.parseRequestRedirectBinding(response);
        }

        @Override
        protected String getBindingType() {
            return SamlProtocol.SAML_REDIRECT_BINDING;
        }

    }

}
