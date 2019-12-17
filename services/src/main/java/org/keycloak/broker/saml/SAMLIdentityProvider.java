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
package org.keycloak.broker.saml;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.*;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.events.EventBuilder;
import org.keycloak.keys.RsaKeyMetadata;
import org.keycloak.models.*;
import org.keycloak.protocol.saml.JaxrsSAML2BindingBuilder;
import org.keycloak.protocol.saml.SamlSessionUtils;
import org.keycloak.protocol.saml.preprocessor.SamlAuthenticationPreprocessor;
import org.keycloak.saml.*;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder.NodeGenerator;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.util.KeycloakKeySamlExtensionGenerator;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.security.KeyPair;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Pedro Igor
 */
public class SAMLIdentityProvider extends AbstractIdentityProvider<SAMLIdentityProviderConfig> {
    protected static final Logger logger = Logger.getLogger(SAMLIdentityProvider.class);
    private final DestinationValidator destinationValidator;
    public SAMLIdentityProvider(KeycloakSession session, SAMLIdentityProviderConfig config, DestinationValidator destinationValidator) {
        super(session, config);
        this.destinationValidator = destinationValidator;
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new SAMLEndpoint(realm, this, getConfig(), callback, destinationValidator);
    }

    @Override
    public Response performLogin(AuthenticationRequest request) {
        try {
            UriInfo uriInfo = request.getUriInfo();
            RealmModel realm = request.getRealm();
            String issuerURL = getEntityId(uriInfo, realm);
            String destinationUrl = getConfig().getSingleSignOnServiceUrl();
            String nameIDPolicyFormat = getConfig().getNameIDPolicyFormat();

            if (nameIDPolicyFormat == null) {
                nameIDPolicyFormat =  JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get();
            }

            String protocolBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get();

            String assertionConsumerServiceUrl = request.getRedirectUri();

            if (getConfig().isPostBindingResponse()) {
                protocolBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get();
            }

            SAML2AuthnRequestBuilder authnRequestBuilder = new SAML2AuthnRequestBuilder()
                    .assertionConsumerUrl(assertionConsumerServiceUrl)
                    .destination(destinationUrl)
                    .issuer(issuerURL)
                    .forceAuthn(getConfig().isForceAuthn())
                    .protocolBinding(protocolBinding)
                    .nameIdPolicy(SAML2NameIDPolicyBuilder.format(nameIDPolicyFormat));
            JaxrsSAML2BindingBuilder binding = new JaxrsSAML2BindingBuilder(session)
                    .relayState(request.getState().getEncoded());
            boolean postBinding = getConfig().isPostBindingAuthnRequest();

            if (getConfig().isWantAuthnRequestsSigned()) {
                KeyManager.ActiveRsaKey keys = session.keys().getActiveRsaKey(realm);

                KeyPair keypair = new KeyPair(keys.getPublicKey(), keys.getPrivateKey());

                String keyName = getConfig().getXmlSigKeyInfoKeyNameTransformer().getKeyName(keys.getKid(), keys.getCertificate());
                binding.signWith(keyName, keypair);
                binding.signatureAlgorithm(getSignatureAlgorithm());
                binding.signDocument();
                if (! postBinding && getConfig().isAddExtensionsElementWithKeyInfo()) {    // Only include extension if REDIRECT binding and signing whole SAML protocol message
                    authnRequestBuilder.addExtension(new KeycloakKeySamlExtensionGenerator(keyName));
                }
            }

            AuthnRequestType authnRequest = authnRequestBuilder.createAuthnRequest();
            for(Iterator<SamlAuthenticationPreprocessor> it = SamlSessionUtils.getSamlAuthenticationPreprocessorIterator(session); it.hasNext(); ) {
                authnRequest = it.next().beforeSendingLoginRequest(authnRequest, request.getAuthenticationSession());
            }

            if (postBinding) {
                return binding.postBinding(authnRequestBuilder.toDocument()).request(destinationUrl);
            } else {
                return binding.redirectBinding(authnRequestBuilder.toDocument()).request(destinationUrl);
            }
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not create authentication request.", e);
        }
    }

    private String getEntityId(UriInfo uriInfo, RealmModel realm) {
        return UriBuilder.fromUri(uriInfo.getBaseUri()).path("realms").path(realm.getName()).build().toString();
    }

    @Override
    public void authenticationFinished(AuthenticationSessionModel authSession, BrokeredIdentityContext context)  {
        ResponseType responseType = (ResponseType)context.getContextData().get(SAMLEndpoint.SAML_LOGIN_RESPONSE);
        AssertionType assertion = (AssertionType)context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);
        SubjectType subject = assertion.getSubject();
        SubjectType.STSubType subType = subject.getSubType();
        NameIDType subjectNameID = (NameIDType) subType.getBaseID();
        authSession.setUserSessionNote(SAMLEndpoint.SAML_FEDERATED_SUBJECT_NAMEID, subjectNameID.serializeAsString());
        AuthnStatementType authn =  (AuthnStatementType)context.getContextData().get(SAMLEndpoint.SAML_AUTHN_STATEMENT);
        if (authn != null && authn.getSessionIndex() != null) {
            authSession.setUserSessionNote(SAMLEndpoint.SAML_FEDERATED_SESSION_INDEX, authn.getSessionIndex());

        }
    }

    @Override
    public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity) {
        return Response.ok(identity.getToken()).build();
    }

    @Override
    public void backchannelLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
        String singleLogoutServiceUrl = getConfig().getSingleLogoutServiceUrl();
        if (singleLogoutServiceUrl == null || singleLogoutServiceUrl.trim().equals("") || !getConfig().isBackchannelSupported()) return;
        JaxrsSAML2BindingBuilder binding = buildLogoutBinding(session, userSession, realm);
        try {
            LogoutRequestType logoutRequest = buildLogoutRequest(userSession, uriInfo, realm, singleLogoutServiceUrl);
            int status = SimpleHttp.doPost(singleLogoutServiceUrl, session)
                    .param(GeneralConstants.SAML_REQUEST_KEY, binding.postBinding(SAML2Request.convert(logoutRequest)).encoded())
                    .param(GeneralConstants.RELAY_STATE, userSession.getId()).asStatus();
            boolean success = status >=200 && status < 400;
            if (!success) {
                logger.warn("Failed saml backchannel broker logout to: " + singleLogoutServiceUrl);
            }
        } catch (Exception e) {
            logger.warn("Failed saml backchannel broker logout to: " + singleLogoutServiceUrl, e);
        }

    }

    @Override
    public Response keycloakInitiatedBrowserLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
        String singleLogoutServiceUrl = getConfig().getSingleLogoutServiceUrl();
        if (singleLogoutServiceUrl == null || singleLogoutServiceUrl.trim().equals("")) return null;

        if (getConfig().isBackchannelSupported()) {
            backchannelLogout(session, userSession, uriInfo, realm);
            return null;
       } else {
            try {
                LogoutRequestType logoutRequest = buildLogoutRequest(userSession, uriInfo, realm, singleLogoutServiceUrl);
                JaxrsSAML2BindingBuilder binding = buildLogoutBinding(session, userSession, realm);
                if (getConfig().isPostBindingLogout()) {
                    return binding.postBinding(SAML2Request.convert(logoutRequest)).request(singleLogoutServiceUrl);
                } else {
                    return binding.redirectBinding(SAML2Request.convert(logoutRequest)).request(singleLogoutServiceUrl);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected LogoutRequestType buildLogoutRequest(UserSessionModel userSession, UriInfo uriInfo, RealmModel realm, String singleLogoutServiceUrl, NodeGenerator... extensions) throws ConfigurationException {
        SAML2LogoutRequestBuilder logoutBuilder = new SAML2LogoutRequestBuilder()
                .assertionExpiration(realm.getAccessCodeLifespan())
                .issuer(getEntityId(uriInfo, realm))
                .sessionIndex(userSession.getNote(SAMLEndpoint.SAML_FEDERATED_SESSION_INDEX))
                .nameId(NameIDType.deserializeFromString(userSession.getNote(SAMLEndpoint.SAML_FEDERATED_SUBJECT_NAMEID)))
                .destination(singleLogoutServiceUrl);
        LogoutRequestType logoutRequest = logoutBuilder.createLogoutRequest();
        for (NodeGenerator extension : extensions) {
            logoutBuilder.addExtension(extension);
        }
        for (Iterator<SamlAuthenticationPreprocessor> it = SamlSessionUtils.getSamlAuthenticationPreprocessorIterator(session); it.hasNext();) {
            logoutRequest = it.next().beforeSendingLogoutRequest(logoutRequest, userSession, null);
        }
        return logoutRequest;
    }

    private JaxrsSAML2BindingBuilder buildLogoutBinding(KeycloakSession session, UserSessionModel userSession, RealmModel realm) {
        JaxrsSAML2BindingBuilder binding = new JaxrsSAML2BindingBuilder(session)
                .relayState(userSession.getId());
        if (getConfig().isWantAuthnRequestsSigned()) {
            KeyManager.ActiveRsaKey keys = session.keys().getActiveRsaKey(realm);
            String keyName = getConfig().getXmlSigKeyInfoKeyNameTransformer().getKeyName(keys.getKid(), keys.getCertificate());
            binding.signWith(keyName, keys.getPrivateKey(), keys.getPublicKey(), keys.getCertificate())
                    .signatureAlgorithm(getSignatureAlgorithm())
                    .signDocument();
        }
        return binding;
    }

    @Override
    public Response export(UriInfo uriInfo, RealmModel realm, String format) {

        String authnBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get();

        if (getConfig().isPostBindingAuthnRequest()) {
            authnBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get();
        }

        String endpoint = uriInfo.getBaseUriBuilder()
                .path("realms").path(realm.getName())
                .path("broker")
                .path(getConfig().getAlias())
                .path("endpoint")
                .build().toString();


        boolean wantAuthnRequestsSigned = getConfig().isWantAuthnRequestsSigned();
        boolean wantAssertionsSigned = getConfig().isWantAssertionsSigned();
        boolean wantAssertionsEncrypted = getConfig().isWantAssertionsEncrypted();
        String entityId = getEntityId(uriInfo, realm);
        String nameIDPolicyFormat = getConfig().getNameIDPolicyFormat();

        StringBuilder signingKeysString = new StringBuilder();
        StringBuilder encryptionKeysString = new StringBuilder();
        Set<RsaKeyMetadata> keys = new TreeSet<>((o1, o2) -> o1.getStatus() == o2.getStatus() // Status can be only PASSIVE OR ACTIVE, push PASSIVE to end of list
          ? (int) (o2.getProviderPriority() - o1.getProviderPriority())
          : (o1.getStatus() == KeyStatus.PASSIVE ? 1 : -1));
        keys.addAll(session.keys().getRsaKeys(realm));
        for (RsaKeyMetadata key : keys) {
            addKeyInfo(signingKeysString, key, KeyTypes.SIGNING.value());

            if (key.getStatus() == KeyStatus.ACTIVE) {
                addKeyInfo(encryptionKeysString, key, KeyTypes.ENCRYPTION.value());
            }
        }
        String descriptor = SPMetadataDescriptor.getSPDescriptor(authnBinding, endpoint, endpoint,
          wantAuthnRequestsSigned, wantAssertionsSigned, wantAssertionsEncrypted,
          entityId, nameIDPolicyFormat, signingKeysString.toString(), encryptionKeysString.toString());

        return Response.ok(descriptor, MediaType.APPLICATION_XML_TYPE).build();
    }

    private static void addKeyInfo(StringBuilder target, RsaKeyMetadata key, String purpose) {
        if (key == null) {
            return;
        }

        target.append(SPMetadataDescriptor.xmlKeyInfo("        ", key.getKid(), PemUtils.encodeCertificate(key.getCertificate()), purpose, true));
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        String alg = getConfig().getSignatureAlgorithm();
        if (alg != null) {
            SignatureAlgorithm algorithm = SignatureAlgorithm.valueOf(alg);
            if (algorithm != null) return algorithm;
        }
        return SignatureAlgorithm.RSA_SHA256;
    }

    @Override
    public IdentityProviderDataMarshaller getMarshaller() {
        return new SAMLDataMarshaller();
    }
}
