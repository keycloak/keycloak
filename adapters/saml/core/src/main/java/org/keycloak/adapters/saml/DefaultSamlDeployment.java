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

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Set;

import org.keycloak.common.enums.SslRequired;
import org.keycloak.saml.SignatureAlgorithm;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DefaultSamlDeployment implements SamlDeployment {

    public static class DefaultSingleSignOnService implements IDP.SingleSignOnService {
        private boolean signRequest;
        private boolean validateResponseSignature;
        private boolean validateAssertionSignature;
        private Binding requestBinding;
        private Binding responseBinding;
        private String requestBindingUrl;

        @Override
        public boolean signRequest() {
            return signRequest;
        }

        @Override
        public boolean validateResponseSignature() {
            return validateResponseSignature;
        }

        @Override
        public boolean validateAssertionSignature() {
            return validateAssertionSignature;
        }

        @Override
        public Binding getRequestBinding() {
            return requestBinding;
        }

        @Override
        public Binding getResponseBinding() {
            return responseBinding;
        }

        @Override
        public String getRequestBindingUrl() {
            return requestBindingUrl;
        }

        public void setSignRequest(boolean signRequest) {
            this.signRequest = signRequest;
        }

        public void setValidateResponseSignature(boolean validateResponseSignature) {
            this.validateResponseSignature = validateResponseSignature;
        }

        public void setValidateAssertionSignature(boolean validateAssertionSignature) {
            this.validateAssertionSignature = validateAssertionSignature;
        }

        public void setRequestBinding(Binding requestBinding) {
            this.requestBinding = requestBinding;
        }

        public void setResponseBinding(Binding responseBinding) {
            this.responseBinding = responseBinding;
        }

        public void setRequestBindingUrl(String requestBindingUrl) {
            this.requestBindingUrl = requestBindingUrl;
        }
    }

    public static class DefaultSingleLogoutService implements IDP.SingleLogoutService {
        private boolean validateRequestSignature;
        private boolean validateResponseSignature;
        private boolean signRequest;
        private boolean signResponse;
        private Binding requestBinding;
        private Binding responseBinding;
        private String requestBindingUrl;
        private String responseBindingUrl;

        @Override
        public boolean validateRequestSignature() {
            return validateRequestSignature;
        }

        @Override
        public boolean validateResponseSignature() {
            return validateResponseSignature;
        }

        @Override
        public boolean signRequest() {
            return signRequest;
        }

        @Override
        public boolean signResponse() {
            return signResponse;
        }

        @Override
        public Binding getRequestBinding() {
            return requestBinding;
        }

        @Override
        public Binding getResponseBinding() {
            return responseBinding;
        }

        @Override
        public String getRequestBindingUrl() {
            return requestBindingUrl;
        }

        @Override
        public String getResponseBindingUrl() {
            return responseBindingUrl;
        }

        public void setValidateRequestSignature(boolean validateRequestSignature) {
            this.validateRequestSignature = validateRequestSignature;
        }

        public void setValidateResponseSignature(boolean validateResponseSignature) {
            this.validateResponseSignature = validateResponseSignature;
        }

        public void setSignRequest(boolean signRequest) {
            this.signRequest = signRequest;
        }

        public void setSignResponse(boolean signResponse) {
            this.signResponse = signResponse;
        }

        public void setRequestBinding(Binding requestBinding) {
            this.requestBinding = requestBinding;
        }

        public void setResponseBinding(Binding responseBinding) {
            this.responseBinding = responseBinding;
        }

        public void setRequestBindingUrl(String requestBindingUrl) {
            this.requestBindingUrl = requestBindingUrl;
        }

        public void setResponseBindingUrl(String responseBindingUrl) {
            this.responseBindingUrl = responseBindingUrl;
        }
    }

    public static class DefaultIDP implements IDP {

        private String entityID;
        private PublicKey signatureValidationKey;
        private SingleSignOnService singleSignOnService;
        private SingleLogoutService singleLogoutService;

        @Override
        public String getEntityID() {
            return entityID;
        }

        @Override
        public SingleSignOnService getSingleSignOnService() {
            return singleSignOnService;
        }

        @Override
        public SingleLogoutService getSingleLogoutService() {
            return singleLogoutService;
        }

        @Override
        public PublicKey getSignatureValidationKey() {
            return signatureValidationKey;
        }

        public void setEntityID(String entityID) {
            this.entityID = entityID;
        }

        public void setSignatureValidationKey(PublicKey signatureValidationKey) {
            this.signatureValidationKey = signatureValidationKey;
        }

        public void setSingleSignOnService(SingleSignOnService singleSignOnService) {
            this.singleSignOnService = singleSignOnService;
        }

        public void setSingleLogoutService(SingleLogoutService singleLogoutService) {
            this.singleLogoutService = singleLogoutService;
        }
    }

    private IDP idp;
    private boolean configured;
    private SslRequired sslRequired = SslRequired.EXTERNAL;
    private String entityID;
    private String nameIDPolicyFormat;
    private boolean forceAuthentication;
    private boolean isPassive;
    private boolean turnOffChangeSessionIdOnLogin;
    private PrivateKey decryptionKey;
    private KeyPair signingKeyPair;
    private String assertionConsumerServiceUrl;
    private Set<String> roleAttributeNames;
    private PrincipalNamePolicy principalNamePolicy = PrincipalNamePolicy.FROM_NAME_ID;
    private String principalAttributeName;
    private String logoutPage;
    private SignatureAlgorithm signatureAlgorithm;
    private String signatureCanonicalizationMethod;

    @Override
    public boolean turnOffChangeSessionIdOnLogin() {
        return turnOffChangeSessionIdOnLogin;
    }

    public void setTurnOffChangeSessionIdOnLogin(boolean turnOffChangeSessionIdOnLogin) {
        this.turnOffChangeSessionIdOnLogin = turnOffChangeSessionIdOnLogin;
    }


    @Override
    public IDP getIDP() {
        return idp;
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }

    @Override
    public SslRequired getSslRequired() {
        return sslRequired;
    }

    @Override
    public String getEntityID() {
        return entityID;
    }

    @Override
    public String getNameIDPolicyFormat() {
        return nameIDPolicyFormat;
    }

    @Override
    public boolean isForceAuthentication() {
        return forceAuthentication;
    }
    
   @Override
    public boolean isIsPassive() {
        return isPassive;
    }

    @Override
    public PrivateKey getDecryptionKey() {
        return decryptionKey;
    }

    @Override
    public KeyPair getSigningKeyPair() {
        return signingKeyPair;
    }

    @Override
    public String getAssertionConsumerServiceUrl() {
        return assertionConsumerServiceUrl;
    }

    @Override
    public Set<String> getRoleAttributeNames() {
        return roleAttributeNames;
    }

    @Override
    public PrincipalNamePolicy getPrincipalNamePolicy() {
        return principalNamePolicy;
    }

    @Override
    public String getPrincipalAttributeName() {
        return principalAttributeName;
    }

    public void setIdp(IDP idp) {
        this.idp = idp;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public void setSslRequired(SslRequired sslRequired) {
        this.sslRequired = sslRequired;
    }

    public void setEntityID(String entityID) {
        this.entityID = entityID;
    }

    public void setNameIDPolicyFormat(String nameIDPolicyFormat) {
        this.nameIDPolicyFormat = nameIDPolicyFormat;
    }

    public void setForceAuthentication(boolean forceAuthentication) {
        this.forceAuthentication = forceAuthentication;
    }
    
    public void setIsPassive(boolean isPassive){
        this.isPassive = isPassive;
    }

    public void setDecryptionKey(PrivateKey decryptionKey) {
        this.decryptionKey = decryptionKey;
    }

    public void setSigningKeyPair(KeyPair signingKeyPair) {
        this.signingKeyPair = signingKeyPair;
    }

    public void setAssertionConsumerServiceUrl(String assertionConsumerServiceUrl) {
        this.assertionConsumerServiceUrl = assertionConsumerServiceUrl;
    }

    public void setRoleAttributeNames(Set<String> roleAttributeNames) {
        this.roleAttributeNames = roleAttributeNames;
    }

    public void setPrincipalNamePolicy(PrincipalNamePolicy principalNamePolicy) {
        this.principalNamePolicy = principalNamePolicy;
    }

    public void setPrincipalAttributeName(String principalAttributeName) {
        this.principalAttributeName = principalAttributeName;
    }

    @Override
    public String getLogoutPage() {
        return logoutPage;
    }

    public void setLogoutPage(String logoutPage) {
        this.logoutPage = logoutPage;
    }

    @Override
    public String getSignatureCanonicalizationMethod() {
        return signatureCanonicalizationMethod;
    }

    public void setSignatureCanonicalizationMethod(String signatureCanonicalizationMethod) {
        this.signatureCanonicalizationMethod = signatureCanonicalizationMethod;
    }

    @Override
    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(SignatureAlgorithm signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }
}
