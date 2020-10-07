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
import java.util.Set;
import org.apache.http.client.HttpClient;
import org.keycloak.rotation.KeyLocator;
import java.net.URI;

/**
 * Represents SAML deployment configuration.
 * 
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
        /**
         * Returns entity identifier of this IdP.
         * @return see description.
         */
        String getEntityID();

        /**
         * Returns Single sign on service configuration for this IdP.
         * @return see description.
         */
        SingleSignOnService getSingleSignOnService();

        /**
         * Returns Single logout service configuration for this IdP.
         * @return see description.
         */
        SingleLogoutService getSingleLogoutService();

        /**
         * Returns {@link KeyLocator} looking up public keys used for validation of IdP signatures.
         * @return see description.
         */
        KeyLocator getSignatureValidationKeyLocator();

        /**
         * Returns minimum time (in seconds) between issuing requests to IdP SAML descriptor.
         * Used e.g. by {@link KeyLocator} looking up public keys for validation of IdP signatures
         * to prevent too frequent requests.
         *
         * @return see description.
         */
        int getMinTimeBetweenDescriptorRequests();

        /**
         * Returns {@link HttpClient} instance that will be used for http communication with this IdP.
         * @return see description
         */
        HttpClient getClient();

        /**
         * Returns allowed time difference (in milliseconds) between IdP and SP
         * @return see description
         */
        int getAllowedClockSkew();

        public interface SingleSignOnService {
            /**
             * Returns {@code true} if the requests to IdP need to be signed by SP key.
             * @return see dscription
             */
            boolean signRequest();
            /**
             * Returns {@code true} if the complete response message from IdP should
             * be checked for valid signature.
             * @return see dscription
             */
            boolean validateResponseSignature();
            /**
             * Returns {@code true} if individual assertions in response from IdP should
             * be checked for valid signature.
             * @return see dscription
             */
            boolean validateAssertionSignature();
            Binding getRequestBinding();
            /**
             * SAML allows the client to request what binding type it wants authn responses to use. The default is
             * that the client will not request a specific binding type for responses.
             * @return
             */
            Binding getResponseBinding();
            /**
             * Returns URL for the IDP login service that the client will send requests to.
             * @return
             */
            String getRequestBindingUrl();
            /**
             * Returns URI where the IdP should send the responses to. The default is
             * that the client will not request a specific assertion consumer service URL.
             * This property is typically accompanied by the ProtocolBinding attribute.
             * @return
             */
            URI getAssertionConsumerServiceUrl();
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

    /**
     * Returns Identity Provider configuration for this SAML deployment.
     * @return see description.
     */
    public IDP getIDP();

    public boolean isConfigured();
    SslRequired getSslRequired();

    /**
     * Returns entity identifier of this SP.
     * @return see description.
     */
    String getEntityID();
    String getNameIDPolicyFormat();
    boolean isForceAuthentication();
    boolean isIsPassive();
    boolean turnOffChangeSessionIdOnLogin();
    PrivateKey getDecryptionKey();
    KeyPair getSigningKeyPair();
    String getSignatureCanonicalizationMethod();
    SignatureAlgorithm getSignatureAlgorithm();
    String getLogoutPage();

    Set<String> getRoleAttributeNames();

    /**
     * Obtains the {@link RoleMappingsProvider} that was configured for the SP.
     *
     * @return a reference to the configured {@link RoleMappingsProvider}.
     */
    RoleMappingsProvider getRoleMappingsProvider();

    enum PrincipalNamePolicy {
        FROM_NAME_ID,
        FROM_ATTRIBUTE
    }
    PrincipalNamePolicy getPrincipalNamePolicy();
    String getPrincipalAttributeName();
    boolean isAutodetectBearerOnly();

    boolean isKeepDOMAssertion();

}
