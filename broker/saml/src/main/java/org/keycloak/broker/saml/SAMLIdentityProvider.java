/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.broker.saml;

import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.saml.SAML2AuthnRequestBuilder;
import org.keycloak.protocol.saml.SAML2LogoutRequestBuilder;
import org.keycloak.protocol.saml.SAML2NameIDPolicyBuilder;
import org.picketlink.common.constants.JBossSAMLURIConstants;

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
    public SAMLIdentityProvider(SAMLIdentityProviderConfig config) {
        super(config);
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback) {
        return new SAMLEndpoint(realm, getConfig(), callback);
    }

    @Override
    public Response handleRequest(AuthenticationRequest request) {
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
                    .nameIdPolicy(SAML2NameIDPolicyBuilder.format(nameIDPolicyFormat))
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

                authnRequestBuilder.signWith(keypair);
                authnRequestBuilder.signDocument();
            }

            if (getConfig().isPostBindingAuthnRequest()) {
                return authnRequestBuilder.postBinding().request();
            } else {
                return authnRequestBuilder.redirectBinding().request();
            }
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not create authentication request.", e);
        }
    }

    private String getEntityId(UriInfo uriInfo, RealmModel realm) {
        return UriBuilder.fromUri(uriInfo.getBaseUri()).path("realms").path(realm.getName()).build().toString();
    }

    @Override
    public Response retrieveToken(FederatedIdentityModel identity) {
        return Response.ok(identity.getToken()).build();
    }

    @Override
    public Response keycloakInitiatedBrowserLogout(UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
        if (getConfig().getSingleLogoutServiceUrl() == null) return null;

        SAML2LogoutRequestBuilder logoutBuilder = new SAML2LogoutRequestBuilder()
                .issuer(getEntityId(uriInfo, realm))
                .sessionIndex(userSession.getNote("SAML_FEDERATED_SESSION_INDEX"))
                .userPrincipal(userSession.getNote("SAML_FEDERATED_SUBJECT"), userSession.getNote("SAML_FEDERATED_SUBJECT_NAMEFORMAT"))
                .destination(getConfig().getSingleLogoutServiceUrl());
        if (getConfig().isWantAuthnRequestsSigned()) {
            logoutBuilder.signWith(realm.getPrivateKey(), realm.getPublicKey(), realm.getCertificate())
                    .signDocument();
        }
        try {
            return logoutBuilder.relayState(userSession.getId()).postBinding().request();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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



        String descriptor =
                "<EntityDescriptor entityID=\"" + getEntityId(uriInfo, realm) + "\">\n" +
                "    <SPSSODescriptor AuthnRequestsSigned=\"" + getConfig().isWantAuthnRequestsSigned() + "\"\n" +
                "            protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol urn:oasis:names:tc:SAML:1.1:protocol http://schemas.xmlsoap.org/ws/2003/07/secext\">\n" +
                "        <NameIDFormat>" + getConfig().getNameIDPolicyFormat() + "\n" +
                "        </NameIDFormat>\n" +
                "        <SingleLogoutService Binding=\"" + authnBinding + "\" Location=\"" + endpoint + "\"/>\n" +
                "        <AssertionConsumerService\n" +
                "                Binding=\"" + authnBinding + "\" Location=\"" + endpoint + "\"\n" +
                "                index=\"1\" isDefault=\"true\" />\n";
        if (getConfig().isWantAuthnRequestsSigned()) {
            descriptor +=
                "        <KeyDescriptor use=\"signing\">\n" +
                "            <dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "                <dsig:X509Data>\n" +
                "                    <dsig:X509Certificate>\n" + realm.getCertificatePem() + "\n" +
                "                    </dsig:X509Certificate>\n" +
                "                </dsig:X509Data>\n" +
                "            </dsig:KeyInfo>\n" +
                "        </KeyDescriptor>\n";
        }
        descriptor +=
                "    </SPSSODescriptor>\n" +
                "</EntityDescriptor>\n";
        return Response.ok(descriptor, MediaType.APPLICATION_XML_TYPE).build();
    }
}
