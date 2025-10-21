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

package org.keycloak.adapters.saml.profile;

import static org.keycloak.adapters.saml.SamlPrincipal.DEFAULT_ROLE_ATTRIBUTE_NAME;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import org.jboss.logging.Logger;
import org.keycloak.adapters.saml.AbstractInitiateLogin;
import org.keycloak.adapters.saml.AdapterConstants;
import org.keycloak.adapters.saml.OnSessionCreated;
import org.keycloak.adapters.saml.SamlAuthenticationError;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlPrincipal;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.saml.SamlUtil;
import org.keycloak.adapters.saml.profile.webbrowsersso.WebBrowserSsoAuthenticationHandler;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.StatementAbstractType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.protocol.ExtensionsType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.RequestAbstractType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusCodeType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusType;
import org.keycloak.rotation.KeyLocator;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2AuthnRequestBuilder;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.sig.SAML2Signature;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.saml.processing.core.util.KeycloakKeySamlExtensionGenerator;
import org.keycloak.saml.processing.core.util.RedirectBindingSignatureUtil;
import org.keycloak.saml.processing.core.util.XMLEncryptionUtil;
import org.keycloak.saml.processing.web.util.PostBindingUtil;
import org.keycloak.saml.validators.ConditionsValidator;
import org.keycloak.saml.validators.DestinationValidator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public abstract class AbstractSamlAuthenticationHandler implements SamlAuthenticationHandler {

    protected static Logger log = Logger.getLogger(WebBrowserSsoAuthenticationHandler.class);

    protected final HttpFacade facade;
    protected final SamlSessionStore sessionStore;
    protected  final SamlDeployment deployment;
    protected AuthChallenge challenge;
    private final DestinationValidator destinationValidator = DestinationValidator.forProtocolMap(null);
    private static final AuthChallenge CHALLENGE_EXTRACTION_FAILURE =  new AuthChallenge() {
            @Override
            public boolean challenge(HttpFacade exchange) {
                SamlAuthenticationError error = new SamlAuthenticationError(SamlAuthenticationError.Reason.EXTRACTION_FAILURE);
                exchange.getRequest().setError(error);
                exchange.getResponse().sendError(403);
                return true;
            }

            @Override
            public int getResponseCode() {
                return 403;
            }
        };
    private static final AuthChallenge CHALLENGE_INVALID_SIGNATURE = new AuthChallenge() {
            @Override
            public boolean challenge(HttpFacade exchange) {
                SamlAuthenticationError error = new SamlAuthenticationError(SamlAuthenticationError.Reason.INVALID_SIGNATURE);
                exchange.getRequest().setError(error);
                exchange.getResponse().sendError(403);
                return true;
            }

            @Override
            public int getResponseCode() {
                return 403;
            }
        };

    public AbstractSamlAuthenticationHandler(HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
        this.facade = facade;
        this.deployment = deployment;
        this.sessionStore = sessionStore;
    }

    public AuthOutcome doHandle(SamlInvocationContext context, OnSessionCreated onCreateSession) {
        String samlRequest = context.getSamlRequest();
        String samlResponse = context.getSamlResponse();
        String relayState = context.getRelayState();
        if (samlRequest != null) {
            return handleSamlRequest(samlRequest, relayState);
        } else if (samlResponse != null) {
            return handleSamlResponse(samlResponse, relayState, onCreateSession);
        } else if (sessionStore.isLoggedIn()) {
            if (verifySSL()) return failedTerminal();
            log.debug("AUTHENTICATED: was cached");
            return handleRequest();
        }
        return initiateLogin(true);
    }

    protected AuthOutcome handleRequest() {
        return AuthOutcome.AUTHENTICATED;
    }

    @Override
    public AuthChallenge getChallenge() {
        return this.challenge;
    }

    protected AuthOutcome handleSamlRequest(String samlRequest, String relayState) {
        SAMLDocumentHolder holder = null;
        boolean postBinding = false;
        String requestUri = facade.getRequest().getURI();
        if (facade.getRequest().getMethod().equalsIgnoreCase("GET")) {
            // strip out query params
            int index = requestUri.indexOf('?');
            if (index > -1) {
                requestUri = requestUri.substring(0, index);
            }
            holder = SAMLRequestParser.parseRequestRedirectBinding(samlRequest);
        } else {
            postBinding = true;
            holder = SAMLRequestParser.parseRequestPostBinding(samlRequest);
        }
        if (holder == null) {
            log.error("Error parsing SAML document");
            return failedTerminal();
        }
        RequestAbstractType requestAbstractType = (RequestAbstractType) holder.getSamlObject();
        if (requestAbstractType.getDestination() == null && containsUnencryptedSignature(holder, postBinding)) {
            log.error("Destination field required.");
            return failed(CHALLENGE_EXTRACTION_FAILURE);
        }
        if (! destinationValidator.validate(requestUri, requestAbstractType.getDestination())) {
            log.error("Expected destination '" + requestUri + "' got '" + requestAbstractType.getDestination() + "'");
            return failedTerminal();
        }

        if (requestAbstractType instanceof LogoutRequestType) {
            if (deployment.getIDP().getSingleLogoutService().validateRequestSignature()) {
                try {
                    validateSamlSignature(holder, postBinding, GeneralConstants.SAML_REQUEST_KEY);
                } catch (VerificationException e) {
                    log.error("Failed to verify saml request signature", e);
                    return failedTerminal();
                }
            }
            LogoutRequestType logout = (LogoutRequestType) requestAbstractType;
            return logoutRequest(logout, relayState);

        } else {
            log.error("unknown SAML request type");
            return failedTerminal();
        }
    }

    protected abstract AuthOutcome logoutRequest(LogoutRequestType request, String relayState);

    protected AuthOutcome handleSamlResponse(String samlResponse, String relayState, OnSessionCreated onCreateSession) {
        SAMLDocumentHolder holder = null;
        boolean postBinding = false;
        String requestUri = facade.getRequest().getURI();
        if (facade.getRequest().getMethod().equalsIgnoreCase("GET")) {
            int index = requestUri.indexOf('?');
            if (index > -1) {
                requestUri = requestUri.substring(0, index);
            }
            holder = extractRedirectBindingResponse(samlResponse);
        } else {
            postBinding = true;
            holder = extractPostBindingResponse(samlResponse);
        }
        if (holder == null) {
            log.error("Error parsing SAML document");
            return failed(CHALLENGE_EXTRACTION_FAILURE);
        }
        final StatusResponseType statusResponse = (StatusResponseType) holder.getSamlObject();
        // validate destination
        if (statusResponse.getDestination() == null && containsUnencryptedSignature(holder, postBinding)) {
            log.error("Destination field required.");
            return failed(CHALLENGE_EXTRACTION_FAILURE);
        }
        if (! destinationValidator.validate(requestUri, statusResponse.getDestination())) {
            log.error("Request URI '" + requestUri + "' does not match SAML request destination '" + statusResponse.getDestination() + "'");
            return failedTerminal();
        }

        if (statusResponse instanceof ResponseType) {
            try {
                if (deployment.getIDP().getSingleSignOnService().validateResponseSignature()) {
                    try {
                        validateSamlSignature(holder, postBinding, GeneralConstants.SAML_RESPONSE_KEY);
                    } catch (VerificationException e) {
                        log.error("Failed to verify saml response signature", e);

                        return failed(CHALLENGE_INVALID_SIGNATURE);
                    }
                }
                return handleLoginResponse(holder, postBinding, onCreateSession);
            } finally {
                sessionStore.setCurrentAction(SamlSessionStore.CurrentAction.NONE);
            }

        } else {
            if (sessionStore.isLoggingOut()) {
                try {
                    if (deployment.getIDP().getSingleLogoutService().validateResponseSignature()) {
                        try {
                            validateSamlSignature(holder, postBinding, GeneralConstants.SAML_RESPONSE_KEY);
                        } catch (VerificationException e) {
                            log.error("Failed to verify saml response signature", e);
                            return failedTerminal();
                        }
                    }
                    return handleLogoutResponse(holder, statusResponse, relayState);
                } finally {
                    sessionStore.setCurrentAction(SamlSessionStore.CurrentAction.NONE);
                }

            } else if (sessionStore.isLoggingIn()) {

                try {
                    // KEYCLOAK-2107 - handle user not authenticated due passive mode. Return special outcome so different authentication mechanisms can behave accordingly.
                    StatusType status = statusResponse.getStatus();
                    if(checkStatusCodeValue(status.getStatusCode(), JBossSAMLURIConstants.STATUS_RESPONDER.get()) && checkStatusCodeValue(status.getStatusCode().getStatusCode(), JBossSAMLURIConstants.STATUS_NO_PASSIVE.get())){
                        log.debug("Not authenticated due passive mode Status found in SAML response: " + status.toString());
                        return AuthOutcome.NOT_AUTHENTICATED;
                    }

                    return failed(createAuthChallenge403(statusResponse));
                } finally {
                    sessionStore.setCurrentAction(SamlSessionStore.CurrentAction.NONE);
                }
            }

            log.warn("Keycloak Adapter obtained Response, that is not understood. This may be because the containers " +
                    "cookies are not properly configured with SameSite settings. Refer to KEYCLOAK-14103 for more details.");

            return AuthOutcome.NOT_ATTEMPTED;
        }

    }

    private boolean containsUnencryptedSignature(SAMLDocumentHolder documentHolder, boolean postBinding) {
        if (postBinding) {
            Document signedDoc = documentHolder.getSamlDocument();
            NodeList nl = signedDoc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            return nl != null && nl.getLength() > 0;
        } else {
            String algorithm = facade.getRequest().getQueryParamValue(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY);
            return algorithm != null;
        }
    }

    private void validateSamlSignature(SAMLDocumentHolder holder, boolean postBinding, String paramKey) throws VerificationException {
        KeyLocator signatureValidationKey = deployment.getIDP().getSignatureValidationKeyLocator();
        if (postBinding) {
            verifyPostBindingSignature(holder.getSamlDocument(), signatureValidationKey);
        } else {
            String keyId = getMessageSigningKeyId(holder.getSamlObject());
            verifyRedirectBindingSignature(paramKey, signatureValidationKey, keyId);
        }
    }

    private String getMessageSigningKeyId(SAML2Object doc) {
        final ExtensionsType extensions;
        if (doc instanceof RequestAbstractType) {
            extensions = ((RequestAbstractType) doc).getExtensions();
        } else if (doc instanceof StatusResponseType) {
            extensions = ((StatusResponseType) doc).getExtensions();
        } else {
            return null;
        }

        if (extensions == null) {
            return null;
        }

        for (Object ext : extensions.getAny()) {
            if (! (ext instanceof Element)) {
                continue;
            }

            String res = KeycloakKeySamlExtensionGenerator.getMessageSigningKeyIdFromElement((Element) ext);

            if (res != null) {
                return res;
            }
        }

        return null;
    }

    private boolean checkStatusCodeValue(StatusCodeType statusCode, String expectedValue){
        if(statusCode != null && statusCode.getValue()!=null){
            String v = statusCode.getValue().toString();
            return expectedValue.equals(v);
        }
        return false;
    }

    protected AuthOutcome handleLoginResponse(SAMLDocumentHolder responseHolder, boolean postBinding, OnSessionCreated onCreateSession) {
        if (!sessionStore.isLoggingIn()) {
            log.warn("Adapter obtained LoginResponse, however containers session is not aware of sending any request. " +
                    "This may be because the session cookies created by container are not properly configured " +
                    "with SameSite settings. Refer to KEYCLOAK-14103 for more details.");
        }

    	final ResponseType responseType = (ResponseType) responseHolder.getSamlObject();
        AssertionType assertion = null;
        if (isRetrayableSamlResponse(responseType)) {
            // initiate the login but do not save the request cos it's /saml
            return initiateLogin(false);
        } else if (!isSuccessfulSamlResponse(responseType) || responseType.getAssertions() == null || responseType.getAssertions().isEmpty()) {
            return failed(createAuthChallenge403(responseType));
        }
        try {
            assertion = AssertionUtil.getAssertion(responseHolder, responseType, deployment.getDecryptionKey());
            ConditionsValidator.Builder cvb = new ConditionsValidator.Builder(assertion.getID(), assertion.getConditions(), destinationValidator);
            try {
                cvb.clockSkewInMillis(deployment.getIDP().getAllowedClockSkew());
                cvb.addAllowedAudience(URI.create(deployment.getEntityID()));
                if (responseType.getDestination() != null) {
                  // getDestination has been validated to match request URL already so it matches SAML endpoint
                  cvb.addAllowedAudience(URI.create(responseType.getDestination()));
                }
            } catch (IllegalArgumentException ex) {
                // warning has been already emitted in DeploymentBuilder
            }
            if (! cvb.build().isValid()) {
                // initiate the login but do not save the request cos it's /saml
                return initiateLogin(false);
            }
        } catch (Exception e) {
            log.error("Error extracting SAML assertion: " + e.getMessage());
            return failed(CHALLENGE_EXTRACTION_FAILURE);
        }

        Element assertionElement = null;
        if (deployment.getIDP().getSingleSignOnService().validateAssertionSignature()) {
            try {
                assertionElement = getAssertionFromResponse(responseHolder);
                if (!AssertionUtil.isSignatureValid(assertionElement, deployment.getIDP().getSignatureValidationKeyLocator())) {
                    log.error("Failed to verify saml assertion signature");
                    return failed(CHALLENGE_INVALID_SIGNATURE);
                }
            } catch (Exception e) {
                log.error("Error processing validation of SAML assertion: " + e.getMessage());
                return failed(CHALLENGE_EXTRACTION_FAILURE);
            }
        }

        SubjectType subject = assertion.getSubject();
        SubjectType.STSubType subType = subject.getSubType();
        NameIDType subjectNameID = subType == null ? null : (NameIDType) subType.getBaseID();
        String principalName = subjectNameID == null ? null : subjectNameID.getValue();

        Set<String> roles = new HashSet<>();
        MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> friendlyAttributes = new MultivaluedHashMap<>();

        Set<StatementAbstractType> statements = assertion.getStatements();
        for (StatementAbstractType statement : statements) {
            if (statement instanceof AttributeStatementType) {
                AttributeStatementType attributeStatement = (AttributeStatementType) statement;
                List<AttributeStatementType.ASTChoiceType> attList = attributeStatement.getAttributes();
                for (AttributeStatementType.ASTChoiceType obj : attList) {
                    AttributeType attr = obj.getAttribute();
                    if (isRole(attr)) {
                        List<Object> attributeValues = attr.getAttributeValue();
                        if (attributeValues != null) {
                            for (Object attrValue : attributeValues) {
                                String role = getAttributeValue(attrValue);
                                log.debugv("Add role: {0}", role);
                                roles.add(role);
                            }
                        }
                    } else {
                        List<Object> attributeValues = attr.getAttributeValue();
                        if (attributeValues != null) {
                            for (Object attrValue : attributeValues) {
                                String value = getAttributeValue(attrValue);
                                if (attr.getName() != null) {
                                    attributes.add(attr.getName(), value);
                                }
                                if (attr.getFriendlyName() != null) {
                                    friendlyAttributes.add(attr.getFriendlyName(), value);
                                }
                            }
                        }
                    }

                }
            }
        }

        if (deployment.getPrincipalNamePolicy() == SamlDeployment.PrincipalNamePolicy.FROM_ATTRIBUTE) {
            if (deployment.getPrincipalAttributeName() != null) {
                String attribute = attributes.getFirst(deployment.getPrincipalAttributeName());
                if (attribute != null) principalName = attribute;
                else {
                    attribute = friendlyAttributes.getFirst(deployment.getPrincipalAttributeName());
                    if (attribute != null) principalName = attribute;
                }
            }
        }

        // use the configured role mappings provider to map roles if necessary.
        if (deployment.getRoleMappingsProvider() != null)  {
            roles = deployment.getRoleMappingsProvider().map(principalName, roles);
        }

        // roles should also be there as regular attributes
        // this mainly required for elytron and its ABAC nature
        attributes.put(DEFAULT_ROLE_ATTRIBUTE_NAME, new ArrayList<>(roles));

        AuthnStatementType authn = null;
        for (Object statement : assertion.getStatements()) {
            if (statement instanceof AuthnStatementType) {
                authn = (AuthnStatementType) statement;
                break;
            }
        }


        URI nameFormat = subjectNameID == null ? null : subjectNameID.getFormat();
        String nameFormatString = nameFormat == null ? JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get() : nameFormat.toString();
        if (deployment.isKeepDOMAssertion() && assertionElement == null) {
            // obtain the assertion from the response to add the DOM document to the principal
            assertionElement = getAssertionFromResponseNoException(responseHolder);
        }
        final SamlPrincipal principal = new SamlPrincipal(assertion,
                deployment.isKeepDOMAssertion()? getAssertionDocumentFromElement(assertionElement) : null,
                principalName, principalName, nameFormatString, attributes, friendlyAttributes);
        final String sessionIndex = authn == null ? null : authn.getSessionIndex();
        final XMLGregorianCalendar sessionNotOnOrAfter = authn == null ? null : authn.getSessionNotOnOrAfter();
        SamlSession account = new SamlSession(principal, roles, sessionIndex, sessionNotOnOrAfter);
        sessionStore.saveAccount(account);
        onCreateSession.onSessionCreated(account);

        // redirect to original request, it will be restored
        String redirectUri = sessionStore.getRedirectUri();
        if (redirectUri != null) {
            facade.getResponse().setHeader("Location", redirectUri);
            facade.getResponse().setStatus(302);
            facade.getResponse().end();
        } else {
            log.debug("IDP initiated invocation");
        }
        log.debug("AUTHENTICATED authn");

        return AuthOutcome.AUTHENTICATED;
    }

    private AuthOutcome failed(AuthChallenge challenge) {
        this.challenge = challenge;
        return AuthOutcome.FAILED;
    }

    /**
     * Used to indicate failure without returning a challenge back to caller.
     * @param challenge
     * @return
     */
    private AuthOutcome failedTerminal() {
        return failed(null);
    }

    private boolean isSuccessfulSamlResponse(ResponseType responseType) {
        return responseType != null
          && responseType.getStatus() != null
          && responseType.getStatus().getStatusCode() != null
          && responseType.getStatus().getStatusCode().getValue() != null
          && Objects.equals(responseType.getStatus().getStatusCode().getValue().toString(), JBossSAMLURIConstants.STATUS_SUCCESS.get());
    }

    private boolean isRetrayableSamlResponse(ResponseType responseType) {
        if (responseType == null || responseType.getStatus() == null) {
            return false;
        }

        StatusType status = responseType.getStatus();
        return status.getStatusCode() != null
          && AdapterConstants.AUTHENTICATION_EXPIRED_MESSAGE.equals(status.getStatusMessage())
          && status.getStatusCode().getValue() != null
          && Objects.equals(status.getStatusCode().getValue().toString(), JBossSAMLURIConstants.STATUS_RESPONDER.get())
          && status.getStatusCode().getStatusCode() != null
          && status.getStatusCode().getStatusCode().getValue() != null
          && Objects.equals(status.getStatusCode().getStatusCode().getValue().toString(), JBossSAMLURIConstants.STATUS_AUTHNFAILED.get());
    }

    private Element getAssertionFromResponse(final SAMLDocumentHolder responseHolder) throws ConfigurationException, ProcessingException {
        Element encryptedAssertion = DocumentUtil.getElement(responseHolder.getSamlDocument(), new QName(JBossSAMLConstants.ENCRYPTED_ASSERTION.get()));
        if (encryptedAssertion != null) {
            // encrypted assertion.
            // We'll need to decrypt it first.
            Document encryptedAssertionDocument = DocumentUtil.createDocument();
            encryptedAssertionDocument.appendChild(encryptedAssertionDocument.importNode(encryptedAssertion, true));

            return XMLEncryptionUtil.decryptElementInDocument(encryptedAssertionDocument, data -> Collections.singletonList(deployment.getDecryptionKey()));
        }
        return DocumentUtil.getElement(responseHolder.getSamlDocument(), new QName(JBossSAMLConstants.ASSERTION.get()));
    }

    private Element getAssertionFromResponseNoException(final SAMLDocumentHolder responseHolder) {
        try {
            return getAssertionFromResponse(responseHolder);
        } catch (ConfigurationException|ProcessingException e) {
            log.warn("Cannot obtain DOM assertion element", e);
            return null;
        }
    }

    private Document getAssertionDocumentFromElement(final Element assertionElement) {
        if (assertionElement == null) {
            return null;
        }
        try {
            Document assertionDoc = DocumentUtil.createDocument();
            assertionDoc.adoptNode(assertionElement);
            assertionDoc.appendChild(assertionElement);
            return assertionDoc;
        } catch (ConfigurationException e) {
            log.warn("Cannot obtain DOM assertion document", e);
            return null;
        }
    }

    private String getAttributeValue(Object attrValue) {
        if (attrValue == null) {
            return "";
        } else if (attrValue instanceof String) {
            return (String) attrValue;
        } else if (attrValue instanceof Node) {
            Node roleNode = (Node) attrValue;
            return roleNode.getFirstChild().getNodeValue();
        } else if (attrValue instanceof NameIDType) {
            NameIDType nameIdType = (NameIDType) attrValue;
            return nameIdType.getValue();
        } else {
            log.warn("Unable to extract unknown SAML assertion attribute value type: " + attrValue.getClass().getName());
        }
        return null;
    }

    protected boolean isRole(AttributeType attribute) {
        return (attribute.getName() != null && deployment.getRoleAttributeNames().contains(attribute.getName())) || (attribute.getFriendlyName() != null && deployment.getRoleAttributeNames().contains(attribute.getFriendlyName()));
    }

    protected AuthOutcome handleLogoutResponse(SAMLDocumentHolder holder, StatusResponseType responseType, String relayState) {
        boolean loggedIn = sessionStore.isLoggedIn();
        if (!loggedIn || !"logout".equals(relayState)) {
            return AuthOutcome.NOT_ATTEMPTED;
        }
        sessionStore.logoutAccount();
        return AuthOutcome.LOGGED_OUT;
    }

    protected SAMLDocumentHolder extractRedirectBindingResponse(String response) {
        return SAMLRequestParser.parseRequestRedirectBinding(response);
    }


    protected SAMLDocumentHolder extractPostBindingResponse(String response) {
        byte[] samlBytes = PostBindingUtil.base64Decode(response);
        return SAMLRequestParser.parseResponseDocument(samlBytes);
    }


    protected AuthOutcome initiateLogin(boolean saveRequestUri) {
        challenge = createChallenge(saveRequestUri);
        return AuthOutcome.NOT_ATTEMPTED;
    }

    protected AbstractInitiateLogin createChallenge(boolean saveRequestUri) {
        return new AbstractInitiateLogin(deployment, sessionStore, saveRequestUri) {
            @Override
            protected void sendAuthnRequest(HttpFacade httpFacade, SAML2AuthnRequestBuilder authnRequestBuilder, BaseSAML2BindingBuilder binding) throws ProcessingException, ConfigurationException, IOException {
                if (isAutodetectedBearerOnly(httpFacade.getRequest())) {
                    httpFacade.getResponse().setStatus(401);
                    httpFacade.getResponse().end();
                }
                else {
                    Document document = authnRequestBuilder.toDocument();
                    SamlDeployment.Binding samlBinding = deployment.getIDP().getSingleSignOnService().getRequestBinding();
                    SamlUtil.sendSaml(true, httpFacade, deployment.getIDP().getSingleSignOnService().getRequestBindingUrl(), binding, document, samlBinding);
                }
            }
        };
    }

    protected boolean verifySSL() {
        if (!facade.getRequest().isSecure() && deployment.getSslRequired().isRequired(facade.getRequest().getRemoteAddr())) {
            log.warn("SSL is required to authenticate");
            return true;
        }
        return false;
    }

    public void verifyPostBindingSignature(Document document, KeyLocator keyLocator) throws VerificationException {
        SAML2Signature saml2Signature = new SAML2Signature();
        try {
            if (!saml2Signature.validate(document, keyLocator)) {
                throw new VerificationException("Invalid signature on document");
            }
        } catch (ProcessingException e) {
            throw new VerificationException("Error validating signature", e);
        }
    }

    private void verifyRedirectBindingSignature(String paramKey, KeyLocator keyLocator, String keyId) throws VerificationException {
        String request = facade.getRequest().getQueryParamValue(paramKey);
        String algorithm = facade.getRequest().getQueryParamValue(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY);
        String signature = facade.getRequest().getQueryParamValue(GeneralConstants.SAML_SIGNATURE_REQUEST_KEY);
        String decodedAlgorithm = facade.getRequest().getQueryParamValue(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY);

        if (request == null) {
            throw new VerificationException("SAML Request was null");
        }
        if (algorithm == null) throw new VerificationException("SigAlg was null");
        if (signature == null) throw new VerificationException("Signature was null");

        // Shibboleth doesn't sign the document for redirect binding.
        // todo maybe a flag?

        String relayState = facade.getRequest().getQueryParamValue(GeneralConstants.RELAY_STATE);
        KeycloakUriBuilder builder = KeycloakUriBuilder.fromPath("/")
                .queryParam(paramKey, request);
        if (relayState != null) {
            builder.queryParam(GeneralConstants.RELAY_STATE, relayState);
        }
        builder.queryParam(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY, algorithm);
        String rawQuery = builder.build().getRawQuery();

        try {
            //byte[] decodedSignature = RedirectBindingUtil.urlBase64Decode(signature);
            byte[] decodedSignature = Base64.getDecoder().decode(signature);
            byte[] rawQueryBytes = rawQuery.getBytes(StandardCharsets.UTF_8);

            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.getFromXmlMethod(decodedAlgorithm);

            if (!RedirectBindingSignatureUtil.validateRedirectBindingSignature(signatureAlgorithm, rawQueryBytes, decodedSignature, keyLocator, keyId)) {
                throw new VerificationException("Invalid query param signature");
            }
        } catch (Exception e) {
            throw new VerificationException(e);
        }
    }

    protected boolean isAutodetectedBearerOnly(HttpFacade.Request request) {
        if (!deployment.isAutodetectBearerOnly()) return false;

        String headerValue = facade.getRequest().getHeader(GeneralConstants.HTTP_HEADER_X_REQUESTED_WITH);
        if (headerValue != null && headerValue.equalsIgnoreCase("XMLHttpRequest")) {
            return true;
        }

        headerValue = facade.getRequest().getHeader("Faces-Request");
        if (headerValue != null && headerValue.startsWith("partial/")) {
            return true;
        }

        headerValue = facade.getRequest().getHeader("SOAPAction");
        if (headerValue != null) {
            return true;
        }

        List<String> accepts = facade.getRequest().getHeaders("Accept");
        if (accepts == null) accepts = Collections.emptyList();

        for (String accept : accepts) {
            if (accept.contains("text/html") || accept.contains("text/*") || accept.contains("*/*")) {
                return false;
            }
        }

        return true;
    }

    private static AuthChallenge createAuthChallenge(final int httpError, final SamlAuthenticationError error) {
        return new AuthChallenge() {
            @Override
            public boolean challenge(HttpFacade exchange) {
                exchange.getRequest().setError(error);
                exchange.getResponse().sendError(httpError);
                return true;
            }

            @Override
            public int getResponseCode() {
                return httpError;
            }
        };
    }

    private static AuthChallenge createAuthChallenge403(final StatusResponseType responseType) {
        return createAuthChallenge(403, new SamlAuthenticationError(SamlAuthenticationError.Reason.ERROR_STATUS, responseType));
    }

}
