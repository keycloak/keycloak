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

package org.keycloak.adapters.saml;

import org.keycloak.adapters.saml.SamlDeployment.IDP.SingleSignOnService;
import org.jboss.logging.Logger;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2AuthnRequestBuilder;
import org.keycloak.saml.SAML2NameIDPolicyBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;

import java.io.IOException;
import java.security.KeyPair;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractInitiateLogin implements AuthChallenge {
    protected static Logger log = Logger.getLogger(AbstractInitiateLogin.class);

    protected SamlDeployment deployment;
    protected SamlSessionStore sessionStore;

    public AbstractInitiateLogin(SamlDeployment deployment, SamlSessionStore sessionStore) {
        this.deployment = deployment;
        this.sessionStore = sessionStore;
    }

    @Override
    public int getResponseCode() {
        return 0;
    }

    @Override
    public boolean challenge(HttpFacade httpFacade) {
        try {
            SAML2AuthnRequestBuilder authnRequestBuilder = buildSaml2AuthnRequestBuilder(deployment);
            BaseSAML2BindingBuilder binding = createSaml2Binding(deployment);
            sessionStore.saveRequest();

            sendAuthnRequest(httpFacade, authnRequestBuilder, binding);
            sessionStore.setCurrentAction(SamlSessionStore.CurrentAction.LOGGING_IN);
        } catch (Exception e) {
            throw new RuntimeException("Could not create authentication request.", e);
        }
        return true;
    }

    public static BaseSAML2BindingBuilder createSaml2Binding(SamlDeployment deployment) {
        BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder();

        if (deployment.getIDP().getSingleSignOnService().signRequest()) {

            binding.signatureAlgorithm(deployment.getSignatureAlgorithm());
            KeyPair keypair = deployment.getSigningKeyPair();
            if (keypair == null) {
                throw new RuntimeException("Signing keys not configured");
            }
            if (deployment.getSignatureCanonicalizationMethod() != null) {
                binding.canonicalizationMethod(deployment.getSignatureCanonicalizationMethod());
            }

            binding.signWith(null, keypair);
            // TODO: As part of KEYCLOAK-3810, add KeyID to the SAML document
            //   <related DocumentBuilder>.addExtension(new KeycloakKeySamlExtensionGenerator(<key ID>));
            binding.signDocument();
        }
        return binding;
    }

    public static SAML2AuthnRequestBuilder buildSaml2AuthnRequestBuilder(SamlDeployment deployment) {
        String issuerURL = deployment.getEntityID();
        String nameIDPolicyFormat = deployment.getNameIDPolicyFormat();

        if (nameIDPolicyFormat == null) {
            nameIDPolicyFormat =  JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get();
        }

        SingleSignOnService sso = deployment.getIDP().getSingleSignOnService();
        SAML2AuthnRequestBuilder authnRequestBuilder = new SAML2AuthnRequestBuilder()
                .destination(sso.getRequestBindingUrl())
                .issuer(issuerURL)
                .forceAuthn(deployment.isForceAuthentication()).isPassive(deployment.isIsPassive())
                .nameIdPolicy(SAML2NameIDPolicyBuilder
                    .format(nameIDPolicyFormat)
                    .setAllowCreate(Boolean.TRUE));
        if (sso.getResponseBinding() != null) {
            String protocolBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get();
            if (sso.getResponseBinding() == SamlDeployment.Binding.POST) {
                protocolBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get();
            }
            authnRequestBuilder.protocolBinding(protocolBinding);

        }
        if (sso.getAssertionConsumerServiceUrl() != null) {
            authnRequestBuilder.assertionConsumerUrl(sso.getAssertionConsumerServiceUrl());
        }
        return authnRequestBuilder;
    }

    protected abstract void sendAuthnRequest(HttpFacade httpFacade, SAML2AuthnRequestBuilder authnRequestBuilder, BaseSAML2BindingBuilder binding) throws ProcessingException, ConfigurationException, IOException;

}
