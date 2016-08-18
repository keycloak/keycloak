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

import org.keycloak.common.enums.SslRequired;
import org.keycloak.saml.SignatureAlgorithm;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface SamlDeployment {
    enum Binding {
        POST,
        REDIRECT;

        public static Binding parseBinding(String val) {
            if (val == null) return POST;
            return Binding.valueOf(val);
        }
    }

    public interface IDP {
        String getEntityID();

        SingleSignOnService getSingleSignOnService();
        SingleLogoutService getSingleLogoutService();
        PublicKey getSignatureValidationKey();

        public interface SingleSignOnService {
            boolean signRequest();
            boolean validateResponseSignature();
            boolean validateAssertionSignature();
            Binding getRequestBinding();
            Binding getResponseBinding();
            String getRequestBindingUrl();
        }
        public interface SingleLogoutService {
            boolean validateRequestSignature();
            boolean validateResponseSignature();
            boolean signRequest();
            boolean signResponse();
            Binding getRequestBinding();
            Binding getResponseBinding();
            String getRequestBindingUrl();
            String getResponseBindingUrl();
        }
    }

    public IDP getIDP();

    public boolean isConfigured();
    SslRequired getSslRequired();
    String getEntityID();
    String getNameIDPolicyFormat();
    boolean isForceAuthentication();
    boolean isIsPassive();
    boolean turnOffChangeSessionIdOnLogin();
    PrivateKey getDecryptionKey();
    KeyPair getSigningKeyPair();
    String getSignatureCanonicalizationMethod();
    SignatureAlgorithm getSignatureAlgorithm();
    String getAssertionConsumerServiceUrl();
    String getLogoutPage();

    Set<String> getRoleAttributeNames();

    enum PrincipalNamePolicy {
        FROM_NAME_ID,
        FROM_ATTRIBUTE
    }
    PrincipalNamePolicy getPrincipalNamePolicy();
    String getPrincipalAttributeName();


}
