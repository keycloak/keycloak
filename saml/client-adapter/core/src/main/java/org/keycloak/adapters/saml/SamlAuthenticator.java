package org.keycloak.adapters.saml;

import org.jboss.logging.Logger;
import org.keycloak.VerificationException;
import org.keycloak.adapters.AuthChallenge;
import org.keycloak.adapters.AuthOutcome;
import org.keycloak.adapters.HttpFacade;
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
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2LogoutRequestBuilder;
import org.keycloak.saml.SAML2LogoutResponseBuilder;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.sig.SAML2Signature;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.saml.processing.web.util.PostBindingUtil;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;
import org.keycloak.util.KeycloakUriBuilder;
import org.keycloak.util.MultivaluedHashMap;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.IOException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class SamlAuthenticator {
    protected static Logger log = Logger.getLogger(SamlAuthenticator.class);

    protected HttpFacade facade;
    protected AuthChallenge challenge;
    protected SamlDeployment deployment;
    protected SamlSessionStore sessionStore;

    public SamlAuthenticator(HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
        this.facade = facade;
        this.deployment = deployment;
        this.sessionStore = sessionStore;
    }

    public AuthChallenge getChallenge() {
        return challenge;
    }

    public AuthOutcome authenticate() {


        String samlRequest = facade.getRequest().getFirstParam(GeneralConstants.SAML_REQUEST_KEY);
        String samlResponse = facade.getRequest().getFirstParam(GeneralConstants.SAML_RESPONSE_KEY);
        String relayState = facade.getRequest().getFirstParam(GeneralConstants.RELAY_STATE);
        boolean globalLogout = "true".equals(facade.getRequest().getQueryParamValue("GLO"));
        if (samlRequest != null) {
            return handleSamlRequest(samlRequest, relayState);
        } else if (samlResponse != null) {
            return handleSamlResponse(samlResponse, relayState);
        }  else if (sessionStore.isLoggedIn()) {
            if (globalLogout) {
                return globalLogout();
            }
            if (verifySSL()) return AuthOutcome.FAILED;
            log.debug("AUTHENTICATED: was cached");
            return AuthOutcome.AUTHENTICATED;
        }
        return initiateLogin();
    }

    protected AuthOutcome globalLogout() {
        SamlSession account = sessionStore.getAccount();
        SAML2LogoutRequestBuilder logoutBuilder = new SAML2LogoutRequestBuilder()
                .assertionExpiration(30)
                .issuer(deployment.getEntityID())
                .sessionIndex(account.getSessionIndex())
                .userPrincipal(account.getPrincipal().getSamlSubject(), account.getPrincipal().getNameIDFormat())
                .destination(deployment.getIDP().getSingleLogoutService().getRequestBindingUrl());
        BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder();
        if (deployment.getIDP().getSingleLogoutService().signRequest()) {
            binding.signWith(deployment.getSigningKeyPair())
                    .signDocument();
        }

        binding.relayState("logout");

        try {
            SamlUtil.sendSaml(facade, deployment.getIDP().getSingleLogoutService().getRequestBindingUrl(), binding, logoutBuilder.buildDocument(), deployment.getIDP().getSingleLogoutService().getRequestBinding());
        } catch (ProcessingException e) {
            throw new RuntimeException(e);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParsingException e) {
            throw new RuntimeException(e);
        }
        return AuthOutcome.NOT_ATTEMPTED;
    }

    protected AuthOutcome handleSamlRequest(String samlRequest, String relayState) {
        SAMLDocumentHolder holder = null;
        boolean postBinding = false;
        if (facade.getRequest().getMethod().equalsIgnoreCase("GET")) {
            holder = SAMLRequestParser.parseRequestRedirectBinding(samlRequest);
        } else {
            postBinding = true;
            holder = SAMLRequestParser.parseRequestPostBinding(samlRequest);
        }
        RequestAbstractType requestAbstractType = (RequestAbstractType) holder.getSamlObject();
        if (!facade.getRequest().getURI().toString().equals(requestAbstractType.getDestination())) {
            throw new RuntimeException("destination not equal to request");
        }

        if (requestAbstractType instanceof LogoutRequestType) {
            if (deployment.getIDP().getSingleLogoutService().validateRequestSignature()) {
                validateSamlSignature(holder, postBinding, GeneralConstants.SAML_REQUEST_KEY);
            }
            LogoutRequestType logout = (LogoutRequestType) requestAbstractType;
            return logoutRequest(logout, relayState);

        } else {
            throw new RuntimeException("unknown SAML request type");
        }
    }

    protected AuthOutcome logoutRequest(LogoutRequestType request, String relayState) {
        if (request.getSessionIndex() == null || request.getSessionIndex().isEmpty()) {
            sessionStore.logoutByPrincipal(request.getNameID().getValue());
        }  else {
            sessionStore.logoutBySsoId(request.getSessionIndex());
        }

        String issuerURL = deployment.getEntityID();
        SAML2LogoutResponseBuilder builder = new SAML2LogoutResponseBuilder();
        builder.logoutRequestID(request.getID());
        builder.destination(deployment.getIDP().getSingleLogoutService().getResponseBindingUrl());
        builder.issuer(issuerURL);
        BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder().relayState(relayState);
        if (deployment.getIDP().getSingleLogoutService().signResponse()) {
            binding.signWith(deployment.getSigningKeyPair())
                    .signDocument();
            binding.canonicalizationMethod(deployment.getIDP().getSingleLogoutService().getSignatureCanonicalizationMethod());
        }


        try {
            SamlUtil.sendSaml(facade, deployment.getIDP().getSingleLogoutService().getResponseBindingUrl(), binding, builder.buildDocument(),
                    deployment.getIDP().getSingleLogoutService().getResponseBinding());
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        } catch (ProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return AuthOutcome.NOT_ATTEMPTED;

    }


    protected AuthOutcome handleSamlResponse(String samlResponse, String relayState) {
        SAMLDocumentHolder holder = null;
        boolean postBinding = false;
        if (facade.getRequest().getMethod().equalsIgnoreCase("GET")) {
            holder = extractRedirectBindingResponse(samlResponse);
        } else {
            postBinding = true;
            holder = extractPostBindingResponse(samlResponse);
        }
        StatusResponseType statusResponse = (StatusResponseType)holder.getSamlObject();
        // validate destination
        if (!facade.getRequest().getURI().toString().equals(statusResponse.getDestination())) {
            throw new RuntimeException("destination not equal to request");
        }
        if (statusResponse instanceof ResponseType) {
            if (deployment.getIDP().getSingleSignOnService().validateResponseSignature()) {
                validateSamlSignature(holder, postBinding, GeneralConstants.SAML_REQUEST_KEY);
            }
            return handleLoginResponse((ResponseType)statusResponse);

        } else {
            if (deployment.getIDP().getSingleLogoutService().validateResponseSignature()) {
                validateSamlSignature(holder, postBinding, GeneralConstants.SAML_REQUEST_KEY);
            }
            // todo need to check that it is actually a LogoutResponse
            return handleLogoutResponse(holder, statusResponse, relayState);
        }

    }

    private void validateSamlSignature(SAMLDocumentHolder holder, boolean postBinding, String paramKey) {
        try {
            if (postBinding) {
                verifyPostBindingSignature(holder.getSamlDocument(), deployment.getIDP().getSignatureValidationKey());
            } else {
                verifyRedirectBindingSignature(deployment.getIDP().getSignatureValidationKey(), paramKey);
            }
        } catch (VerificationException e) {
            log.error("validation failed", e);
            throw new RuntimeException("invalid document signature");
        }
    }

    protected AuthOutcome handleLoginResponse(ResponseType responseType)  {
        AssertionType assertion = null;
        try {
            assertion = AssertionUtil.getAssertion(responseType, deployment.getDecryptionKey());
            if (AssertionUtil.hasExpired(assertion)) {
                return initiateLogin();
            }
        } catch (ParsingException e) {
            throw new RuntimeException(e);
        } catch (ProcessingException e) {
            throw new RuntimeException(e);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
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
                                    attributes.add(attr.getFriendlyName(), value);
                                }
                            }
                        }
                    }

                }
            }
        }
        if (deployment.getPrincipalNamePolicy() == SamlDeployment.PrincipalNamePolicy.FROM_ATTRIBUTE_NAME) {
            if (deployment.getPrincipalAttributeName() != null) {
                String attribute = attributes.getFirst(deployment.getPrincipalAttributeName());
                if (attribute != null) principalName = attribute;
            }
        } else   if (deployment.getPrincipalNamePolicy() == SamlDeployment.PrincipalNamePolicy.FROM_FRIENDLY_ATTRIBUTE_NAME) {
            if (deployment.getPrincipalAttributeName() != null) {
                String attribute = friendlyAttributes.getFirst(deployment.getPrincipalAttributeName());
                if (attribute != null) principalName = attribute;
            }
        }

        AuthnStatementType authn = null;
        for (Object statement : assertion.getStatements()) {
            if (statement instanceof AuthnStatementType) {
                authn = (AuthnStatementType)statement;
                break;
            }
        }


        final SamlPrincipal principal = new SamlPrincipal(principalName, principalName, subjectNameID.getFormat().toString(), attributes, friendlyAttributes);
        String index = authn == null ? null : authn.getSessionIndex();
        final String sessionIndex = index;
        SamlSession account = new SamlSession() {
            @Override
            public SamlPrincipal getPrincipal() {
                return principal;
            }

            @Override
            public Set<String> getRoles() {
                return roles;
            }

            @Override
            public String getSessionIndex() {
                return sessionIndex;
            }
        };
        sessionStore.saveAccount(account);
        completeAuthentication(account);

        // redirect to original request, it will be restored
        facade.getResponse().setHeader("Location", sessionStore.getRedirectUri());
        facade.getResponse().setStatus(302);
        facade.getResponse().end();


        return AuthOutcome.AUTHENTICATED;
    }

    protected abstract void completeAuthentication(SamlSession account);

    private String getAttributeValue(Object attrValue) {
        String value;
        if (attrValue instanceof String) {
            value = (String)attrValue;
        } else if (attrValue instanceof Node) {
            Node roleNode = (Node) attrValue;
            value = roleNode.getFirstChild().getNodeValue();
        } else if (attrValue instanceof NameIDType) {
            NameIDType nameIdType = (NameIDType) attrValue;
            value = nameIdType.getValue();
        } else
            throw new RuntimeException("Unknown attribute value type: " + attrValue.getClass().getName());
        return value;
    }

    protected boolean isRole(AttributeType attribute) {
        return deployment.getRoleAttributeNames().contains(attribute.getName()) || deployment.getRoleAttributeFriendlyNames().contains(attribute.getFriendlyName());
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
        String xml = new String(samlBytes);
        return SAMLRequestParser.parseResponseDocument(samlBytes);
    }



    protected AuthOutcome initiateLogin() {
        challenge = new InitiateLogin(deployment, sessionStore);
        return AuthOutcome.NOT_ATTEMPTED;
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

        if (request == null) throw new VerificationException("SAM was null");
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
            byte[] decodedSignature = RedirectBindingUtil.urlBase64Decode(signature);

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
