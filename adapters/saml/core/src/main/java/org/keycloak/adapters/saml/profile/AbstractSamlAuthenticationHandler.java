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

import org.jboss.logging.Logger;
import org.keycloak.adapters.saml.AbstractInitiateLogin;
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
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.StatementAbstractType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.RequestAbstractType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusCodeType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusType;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2AuthnRequestBuilder;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.Base64;
import org.keycloak.saml.processing.api.saml.v2.sig.SAML2Signature;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.saml.processing.web.util.PostBindingUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.IOException;
import java.net.URI;
import java.security.PublicKey;
import java.security.Signature;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            if (verifySSL()) return AuthOutcome.FAILED;
            log.debug("AUTHENTICATED: was cached");
            return handleRequest();
        }
        return initiateLogin();
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
        RequestAbstractType requestAbstractType = (RequestAbstractType) holder.getSamlObject();
        if (!requestUri.equals(requestAbstractType.getDestination().toString())) {
            log.error("expected destination '" + requestUri + "' got '" + requestAbstractType.getDestination() + "'");
            return AuthOutcome.FAILED;
        }

        if (requestAbstractType instanceof LogoutRequestType) {
            if (deployment.getIDP().getSingleLogoutService().validateRequestSignature()) {
                try {
                    validateSamlSignature(holder, postBinding, GeneralConstants.SAML_REQUEST_KEY);
                } catch (VerificationException e) {
                    log.error("Failed to verify saml request signature", e);
                    return AuthOutcome.FAILED;
                }
            }
            LogoutRequestType logout = (LogoutRequestType) requestAbstractType;
            return logoutRequest(logout, relayState);

        } else {
            log.error("unknown SAML request type");
            return AuthOutcome.FAILED;
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
        final StatusResponseType statusResponse = (StatusResponseType) holder.getSamlObject();
        // validate destination
        if (!requestUri.equals(statusResponse.getDestination())) {
            log.error("Request URI does not match SAML request destination");
            return AuthOutcome.FAILED;
        }

        if (statusResponse instanceof ResponseType) {
            try {
                if (deployment.getIDP().getSingleSignOnService().validateResponseSignature()) {
                    try {
                        validateSamlSignature(holder, postBinding, GeneralConstants.SAML_RESPONSE_KEY);
                    } catch (VerificationException e) {
                        log.error("Failed to verify saml response signature", e);

                        challenge = new AuthChallenge() {
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
                        return AuthOutcome.FAILED;
                    }
                }
                return handleLoginResponse((ResponseType) statusResponse, postBinding, onCreateSession);
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
                            return AuthOutcome.FAILED;
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

                    challenge = new AuthChallenge() {
                        @Override
                        public boolean challenge(HttpFacade exchange) {
                            SamlAuthenticationError error = new SamlAuthenticationError(SamlAuthenticationError.Reason.ERROR_STATUS, statusResponse);
                            exchange.getRequest().setError(error);
                            exchange.getResponse().sendError(403);
                            return true;
                        }

                        @Override
                        public int getResponseCode() {
                            return 403;
                        }
                    };
                    return AuthOutcome.FAILED;
                } finally {
                    sessionStore.setCurrentAction(SamlSessionStore.CurrentAction.NONE);
                }
            }
            return AuthOutcome.NOT_ATTEMPTED;
        }

    }

    private void validateSamlSignature(SAMLDocumentHolder holder, boolean postBinding, String paramKey) throws VerificationException {
        if (postBinding) {
            verifyPostBindingSignature(holder.getSamlDocument(), deployment.getIDP().getSignatureValidationKey());
        } else {
            verifyRedirectBindingSignature(deployment.getIDP().getSignatureValidationKey(), paramKey);
        }
    }

    private boolean checkStatusCodeValue(StatusCodeType statusCode, String expectedValue){
        if(statusCode != null && statusCode.getValue()!=null){
            String v = statusCode.getValue().toString();
            return expectedValue.equals(v);
        }
        return false;
    }

    protected AuthOutcome handleLoginResponse(ResponseType responseType, boolean postBinding, OnSessionCreated onCreateSession) {
        AssertionType assertion = null;
        try {
            assertion = AssertionUtil.getAssertion(responseType, deployment.getDecryptionKey());
            if (AssertionUtil.hasExpired(assertion)) {
                return initiateLogin();
            }
        } catch (Exception e) {
            log.error("Error extracting SAML assertion: " + e.getMessage());
            challenge = new AuthChallenge() {
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
        }

        if (deployment.getIDP().getSingleSignOnService().validateAssertionSignature()) {
            try {
                validateSamlSignature(new SAMLDocumentHolder(AssertionUtil.asDocument(assertion)), postBinding, GeneralConstants.SAML_RESPONSE_KEY);
            } catch (VerificationException e) {
                log.error("Failed to verify saml assertion signature", e);

                challenge = new AuthChallenge() {
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
                return AuthOutcome.FAILED;
            } catch (ProcessingException e) {
                e.printStackTrace();
            }
        }

        SubjectType subject = assertion.getSubject();
        SubjectType.STSubType subType = subject.getSubType();
        NameIDType subjectNameID = (NameIDType) subType.getBaseID();
        String principalName = subjectNameID.getValue();

        final Set<String> roles = new HashSet<>();
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

        AuthnStatementType authn = null;
        for (Object statement : assertion.getStatements()) {
            if (statement instanceof AuthnStatementType) {
                authn = (AuthnStatementType) statement;
                break;
            }
        }


        URI nameFormat = subjectNameID.getFormat();
        String nameFormatString = nameFormat == null ? JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get() : nameFormat.toString();
        final SamlPrincipal principal = new SamlPrincipal(assertion, principalName, principalName, nameFormatString, attributes, friendlyAttributes);
        String index = authn == null ? null : authn.getSessionIndex();
        final String sessionIndex = index;
        SamlSession account = new SamlSession(principal, roles, sessionIndex);
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

    private String getAttributeValue(Object attrValue) {
        String value = null;
        if (attrValue instanceof String) {
            value = (String) attrValue;
        } else if (attrValue instanceof Node) {
            Node roleNode = (Node) attrValue;
            value = roleNode.getFirstChild().getNodeValue();
        } else if (attrValue instanceof NameIDType) {
            NameIDType nameIdType = (NameIDType) attrValue;
            value = nameIdType.getValue();
        } else {
            log.warn("Unable to extract unknown SAML assertion attribute value type: " + attrValue.getClass().getName());
        }
        return value;
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


    protected AuthOutcome initiateLogin() {
        challenge = createChallenge();
        return AuthOutcome.NOT_ATTEMPTED;
    }

    protected AbstractInitiateLogin createChallenge() {
        return new AbstractInitiateLogin(deployment, sessionStore) {
            @Override
            protected void sendAuthnRequest(HttpFacade httpFacade, SAML2AuthnRequestBuilder authnRequestBuilder, BaseSAML2BindingBuilder binding) throws ProcessingException, ConfigurationException, IOException {
                Document document = authnRequestBuilder.toDocument();
                SamlDeployment.Binding samlBinding = deployment.getIDP().getSingleSignOnService().getRequestBinding();
                SamlUtil.sendSaml(true, httpFacade, deployment.getIDP().getSingleSignOnService().getRequestBindingUrl(), binding, document, samlBinding);
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

    public void verifyPostBindingSignature(Document document, PublicKey publicKey) throws VerificationException {
        SAML2Signature saml2Signature = new SAML2Signature();
        try {
            if (!saml2Signature.validate(document, publicKey)) {
                throw new VerificationException("Invalid signature on document");
            }
        } catch (ProcessingException e) {
            throw new VerificationException("Error validating signature", e);
        }
    }

    public void verifyRedirectBindingSignature(PublicKey publicKey, String paramKey) throws VerificationException {
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
            byte[] decodedSignature = Base64.decode(signature);

            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.getFromXmlMethod(decodedAlgorithm);
            Signature validator = signatureAlgorithm.createSignature(); // todo plugin signature alg
            validator.initVerify(publicKey);
            validator.update(rawQuery.getBytes("UTF-8"));
            if (!validator.verify(decodedSignature)) {
                throw new VerificationException("Invalid query param signature");
            }
        } catch (Exception e) {
            throw new VerificationException(e);
        }
    }
}
