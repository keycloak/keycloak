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
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProviderDataMarshaller;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.saml.JaxrsSAML2BindingBuilder;
import org.keycloak.saml.SAML2AuthnRequestBuilder;
import org.keycloak.saml.SAML2LogoutRequestBuilder;
import org.keycloak.saml.SAML2NameIDPolicyBuilder;
import org.keycloak.saml.SPMetadataDescriptor;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * @author Pedro Igor
 */
public class SAMLIdentityProvider extends AbstractIdentityProvider<SAMLIdentityProviderConfig> {
    protected static final Logger logger = Logger.getLogger(SAMLIdentityProvider.class);
    public SAMLIdentityProvider(SAMLIdentityProviderConfig config) {
        super(config);
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new SAMLEndpoint(realm, this, getConfig(), callback);
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
            JaxrsSAML2BindingBuilder binding = new JaxrsSAML2BindingBuilder()
                    .relayState(request.getState());

            if (getConfig().isWantAuthnRequestsSigned()) {
                PrivateKey privateKey = realm.getPrivateKey();
                PublicKey publicKey = realm.getPublicKey();

                if (privateKey == null) {
                    throw new IdentityBrokerException("Identity Provider [" + getConfig().getAlias() + "] wants a signed authentication request. But the Realm [" + realm.getName() + "] does not have a private key.");
                }

                if (publicKey == null) {
                    throw new IdentityBrokerException("Identity Provider [" + getConfig().getAlias() + "] wants a signed authentication request. But the Realm [" + realm.getName() + "] does not have a public key.");
                }

                KeyPair keypair = new KeyPair(publicKey, privateKey);

                binding.signWith(keypair);
                binding.signatureAlgorithm(getSignatureAlgorithm());
                binding.signDocument();
            }

            if (getConfig().isPostBindingAuthnRequest()) {
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
    public void attachUserSession(UserSessionModel userSession, ClientSessionModel clientSession, BrokeredIdentityContext context) {
        ResponseType responseType = (ResponseType)context.getContextData().get(SAMLEndpoint.SAML_LOGIN_RESPONSE);
        AssertionType assertion = (AssertionType)context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);
        SubjectType subject = assertion.getSubject();
        SubjectType.STSubType subType = subject.getSubType();
        NameIDType subjectNameID = (NameIDType) subType.getBaseID();
        userSession.setNote(SAMLEndpoint.SAML_FEDERATED_SUBJECT, subjectNameID.getValue());
        if (subjectNameID.getFormat() != null) userSession.setNote(SAMLEndpoint.SAML_FEDERATED_SUBJECT_NAMEFORMAT, subjectNameID.getFormat().toString());
        AuthnStatementType authn =  (AuthnStatementType)context.getContextData().get(SAMLEndpoint.SAML_AUTHN_STATEMENT);
        if (authn != null && authn.getSessionIndex() != null) {
            userSession.setNote(SAMLEndpoint.SAML_FEDERATED_SESSION_INDEX, authn.getSessionIndex());

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
        SAML2LogoutRequestBuilder logoutBuilder = buildLogoutRequest(userSession, uriInfo, realm, singleLogoutServiceUrl);
        JaxrsSAML2BindingBuilder binding = buildLogoutBinding(userSession, realm);
        try {
            int status = SimpleHttp.doPost(singleLogoutServiceUrl)
                    .param(GeneralConstants.SAML_REQUEST_KEY, binding.postBinding(logoutBuilder.buildDocument()).encoded())
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
                SAML2LogoutRequestBuilder logoutBuilder = buildLogoutRequest(userSession, uriInfo, realm, singleLogoutServiceUrl);
                JaxrsSAML2BindingBuilder binding = buildLogoutBinding(userSession, realm);
                return binding.postBinding(logoutBuilder.buildDocument()).request(singleLogoutServiceUrl);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    protected SAML2LogoutRequestBuilder buildLogoutRequest(UserSessionModel userSession, UriInfo uriInfo, RealmModel realm, String singleLogoutServiceUrl) {
        SAML2LogoutRequestBuilder logoutBuilder = new SAML2LogoutRequestBuilder()
                .assertionExpiration(realm.getAccessCodeLifespan())
                .issuer(getEntityId(uriInfo, realm))
                .sessionIndex(userSession.getNote(SAMLEndpoint.SAML_FEDERATED_SESSION_INDEX))
                .userPrincipal(userSession.getNote(SAMLEndpoint.SAML_FEDERATED_SUBJECT), userSession.getNote(SAMLEndpoint.SAML_FEDERATED_SUBJECT_NAMEFORMAT))
                .destination(singleLogoutServiceUrl);
        return logoutBuilder;
    }

    private JaxrsSAML2BindingBuilder buildLogoutBinding(UserSessionModel userSession, RealmModel realm) {
        JaxrsSAML2BindingBuilder binding = new JaxrsSAML2BindingBuilder()
                .relayState(userSession.getId());
        if (getConfig().isWantAuthnRequestsSigned()) {
            binding.signWith(realm.getPrivateKey(), realm.getPublicKey(), realm.getCertificate())
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
        String entityId = getEntityId(uriInfo, realm);
        String nameIDPolicyFormat = getConfig().getNameIDPolicyFormat();
        String certificatePem = realm.getCertificatePem();
        String descriptor = SPMetadataDescriptor.getSPDescriptor(authnBinding, endpoint, endpoint, wantAuthnRequestsSigned, entityId, nameIDPolicyFormat, certificatePem);
        return Response.ok(descriptor, MediaType.APPLICATION_XML_TYPE).build();
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
