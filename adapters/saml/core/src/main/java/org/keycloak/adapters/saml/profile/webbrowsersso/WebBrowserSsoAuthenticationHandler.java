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

package org.keycloak.adapters.saml.profile.webbrowsersso;

import org.keycloak.adapters.saml.OnSessionCreated;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.saml.SamlUtil;
import org.keycloak.adapters.saml.profile.AbstractSamlAuthenticationHandler;
import org.keycloak.adapters.saml.profile.SamlAuthenticationHandler;
import org.keycloak.adapters.saml.profile.SamlInvocationContext;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2LogoutRequestBuilder;
import org.keycloak.saml.SAML2LogoutResponseBuilder;
import org.keycloak.saml.common.constants.GeneralConstants;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class WebBrowserSsoAuthenticationHandler extends AbstractSamlAuthenticationHandler {

    public static SamlAuthenticationHandler create(HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
        return new WebBrowserSsoAuthenticationHandler(facade, deployment, sessionStore);
    }

    WebBrowserSsoAuthenticationHandler(HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
        super(facade, deployment, sessionStore);
    }

    @Override
    public AuthOutcome handle(OnSessionCreated onCreateSession) {
        return doHandle(new SamlInvocationContext(facade.getRequest().getFirstParam(GeneralConstants.SAML_REQUEST_KEY),
                facade.getRequest().getFirstParam(GeneralConstants.SAML_RESPONSE_KEY),
                facade.getRequest().getFirstParam(GeneralConstants.RELAY_STATE)), onCreateSession);
    }

    @Override
    protected AuthOutcome handleRequest() {
        boolean globalLogout = "true".equals(facade.getRequest().getQueryParamValue("GLO"));

        if (globalLogout) {
            return globalLogout();
        }

        return AuthOutcome.AUTHENTICATED;
    }

    @Override
    protected AuthOutcome logoutRequest(LogoutRequestType request, String relayState) {
        if (request.getSessionIndex() == null || request.getSessionIndex().isEmpty()) {
            sessionStore.logoutByPrincipal(request.getNameID().getValue());
        } else {
            sessionStore.logoutBySsoId(request.getSessionIndex());
        }

        String issuerURL = deployment.getEntityID();
        SAML2LogoutResponseBuilder builder = new SAML2LogoutResponseBuilder();
        builder.logoutRequestID(request.getID());
        builder.destination(deployment.getIDP().getSingleLogoutService().getResponseBindingUrl());
        builder.issuer(issuerURL);
        BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder().relayState(relayState);
        if (deployment.getIDP().getSingleLogoutService().signResponse()) {
            if (deployment.getSignatureCanonicalizationMethod() != null)
                binding.canonicalizationMethod(deployment.getSignatureCanonicalizationMethod());
            binding.signatureAlgorithm(deployment.getSignatureAlgorithm())
                    .signWith(null, deployment.getSigningKeyPair())
                    .signDocument();
            // TODO: As part of KEYCLOAK-3810, add KeyID to the SAML document
            //   <related DocumentBuilder>.addExtension(new KeycloakKeySamlExtensionGenerator(<key ID>));
        }


        try {
            SamlUtil.sendSaml(false, facade, deployment.getIDP().getSingleLogoutService().getResponseBindingUrl(), binding, builder.buildDocument(),
                    deployment.getIDP().getSingleLogoutService().getResponseBinding());
        } catch (Exception e) {
            log.error("Could not send logout response SAML request", e);
            return AuthOutcome.FAILED;
        }
        return AuthOutcome.NOT_ATTEMPTED;
    }

    private AuthOutcome globalLogout() {
        SamlSession account = sessionStore.getAccount();
        if (account == null) {
            return AuthOutcome.NOT_ATTEMPTED;
        }
        SAML2LogoutRequestBuilder logoutBuilder = new SAML2LogoutRequestBuilder()
                .assertionExpiration(30)
                .issuer(deployment.getEntityID())
                .sessionIndex(account.getSessionIndex())
                .nameId(account.getPrincipal().getNameID())
                .destination(deployment.getIDP().getSingleLogoutService().getRequestBindingUrl());
        BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder();
        if (deployment.getIDP().getSingleLogoutService().signRequest()) {
            if (deployment.getSignatureCanonicalizationMethod() != null)
                binding.canonicalizationMethod(deployment.getSignatureCanonicalizationMethod());
            binding.signatureAlgorithm(deployment.getSignatureAlgorithm());
            binding.signWith(null, deployment.getSigningKeyPair())
                    .signDocument();
            // TODO: As part of KEYCLOAK-3810, add KeyID to the SAML document
            //   <related DocumentBuilder>.addExtension(new KeycloakKeySamlExtensionGenerator(<key ID>));
        }

        binding.relayState("logout");

        try {
            SamlUtil.sendSaml(true, facade, deployment.getIDP().getSingleLogoutService().getRequestBindingUrl(), binding, logoutBuilder.buildDocument(), deployment.getIDP().getSingleLogoutService().getRequestBinding());
            sessionStore.setCurrentAction(SamlSessionStore.CurrentAction.LOGGING_OUT);
        } catch (Exception e) {
            log.error("Could not send global logout SAML request", e);
            return AuthOutcome.FAILED;
        }
        return AuthOutcome.NOT_ATTEMPTED;
    }
}
