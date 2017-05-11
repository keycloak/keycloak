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

package org.keycloak.adapters.saml.config;

import java.io.Serializable;
import java.util.List;
import org.keycloak.adapters.cloned.AdapterHttpClientConfig;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class IDP implements Serializable {
    public static class SingleSignOnService implements Serializable {
        private boolean signRequest;
        private boolean validateResponseSignature;
        private String requestBinding;
        private String responseBinding;
        private String bindingUrl;
        private String assertionConsumerServiceUrl;
        private boolean validateAssertionSignature;

        public boolean isSignRequest() {
            return signRequest;
        }

        public void setSignRequest(boolean signRequest) {
            this.signRequest = signRequest;
        }

        public boolean isValidateResponseSignature() {
            return validateResponseSignature;
        }

        public void setValidateResponseSignature(boolean validateResponseSignature) {
            this.validateResponseSignature = validateResponseSignature;
        }

        public boolean isValidateAssertionSignature() {
            return validateAssertionSignature;
        }

        public void setValidateAssertionSignature(boolean validateAssertionSignature) {
            this.validateAssertionSignature = validateAssertionSignature;
        }

        public String getRequestBinding() {
            return requestBinding;
        }

        public void setRequestBinding(String requestBinding) {
            this.requestBinding = requestBinding;
        }

        public String getResponseBinding() {
            return responseBinding;
        }

        public void setResponseBinding(String responseBinding) {
            this.responseBinding = responseBinding;
        }

        public String getBindingUrl() {
            return bindingUrl;
        }

        public void setBindingUrl(String bindingUrl) {
            this.bindingUrl = bindingUrl;
        }

        public String getAssertionConsumerServiceUrl() {
            return assertionConsumerServiceUrl;
        }

        public void setAssertionConsumerServiceUrl(String assertionConsumerServiceUrl) {
            this.assertionConsumerServiceUrl = assertionConsumerServiceUrl;
        }
    }

    public static class SingleLogoutService implements Serializable {
        private boolean signRequest;
        private boolean signResponse;
        private boolean validateRequestSignature;
        private boolean validateResponseSignature;
        private String requestBinding;
        private String responseBinding;
        private String postBindingUrl;
        private String redirectBindingUrl;

        public boolean isSignRequest() {
            return signRequest;
        }

        public void setSignRequest(boolean signRequest) {
            this.signRequest = signRequest;
        }

        public boolean isSignResponse() {
            return signResponse;
        }

        public void setSignResponse(boolean signResponse) {
            this.signResponse = signResponse;
        }

        public boolean isValidateRequestSignature() {
            return validateRequestSignature;
        }

        public void setValidateRequestSignature(boolean validateRequestSignature) {
            this.validateRequestSignature = validateRequestSignature;
        }

        public boolean isValidateResponseSignature() {
            return validateResponseSignature;
        }

        public void setValidateResponseSignature(boolean validateResponseSignature) {
            this.validateResponseSignature = validateResponseSignature;
        }

        public String getRequestBinding() {
            return requestBinding;
        }

        public void setRequestBinding(String requestBinding) {
            this.requestBinding = requestBinding;
        }

        public String getResponseBinding() {
            return responseBinding;
        }

        public void setResponseBinding(String responseBinding) {
            this.responseBinding = responseBinding;
        }

        public String getPostBindingUrl() {
            return postBindingUrl;
        }

        public void setPostBindingUrl(String postBindingUrl) {
            this.postBindingUrl = postBindingUrl;
        }

        public String getRedirectBindingUrl() {
            return redirectBindingUrl;
        }

        public void setRedirectBindingUrl(String redirectBindingUrl) {
            this.redirectBindingUrl = redirectBindingUrl;
        }
    }

    public static class HttpClientConfig implements AdapterHttpClientConfig {

        private String truststore;
        private String truststorePassword;
        private String clientKeystore;
        private String clientKeystorePassword;
        private boolean allowAnyHostname;
        private boolean disableTrustManager;
        private int connectionPoolSize;
        private String proxyUrl;

        @Override
        public String getTruststore() {
            return truststore;
        }

        public void setTruststore(String truststore) {
            this.truststore = truststore;
        }

        @Override
        public String getTruststorePassword() {
            return truststorePassword;
        }

        public void setTruststorePassword(String truststorePassword) {
            this.truststorePassword = truststorePassword;
        }

        @Override
        public String getClientKeystore() {
            return clientKeystore;
        }

        public void setClientKeystore(String clientKeystore) {
            this.clientKeystore = clientKeystore;
        }

        @Override
        public String getClientKeystorePassword() {
            return clientKeystorePassword;
        }

        public void setClientKeystorePassword(String clientKeystorePassword) {
            this.clientKeystorePassword = clientKeystorePassword;
        }

        @Override
        public boolean isAllowAnyHostname() {
            return allowAnyHostname;
        }

        public void setAllowAnyHostname(boolean allowAnyHostname) {
            this.allowAnyHostname = allowAnyHostname;
        }

        @Override
        public boolean isDisableTrustManager() {
            return disableTrustManager;
        }

        public void setDisableTrustManager(boolean disableTrustManager) {
            this.disableTrustManager = disableTrustManager;
        }

        @Override
        public int getConnectionPoolSize() {
            return connectionPoolSize;
        }

        public void setConnectionPoolSize(int connectionPoolSize) {
            this.connectionPoolSize = connectionPoolSize;
        }

        @Override
        public String getProxyUrl() {
            return proxyUrl;
        }

        public void setProxyUrl(String proxyUrl) {
            this.proxyUrl = proxyUrl;
        }
    }

    private String entityID;
    private String signatureAlgorithm;
    private String signatureCanonicalizationMethod;
    private SingleSignOnService singleSignOnService;
    private SingleLogoutService singleLogoutService;
    private List<Key> keys;
    private AdapterHttpClientConfig httpClientConfig = new HttpClientConfig();

    public String getEntityID() {
        return entityID;
    }

    public void setEntityID(String entityID) {
        this.entityID = entityID;
    }

    public SingleSignOnService getSingleSignOnService() {
        return singleSignOnService;
    }

    public void setSingleSignOnService(SingleSignOnService singleSignOnService) {
        this.singleSignOnService = singleSignOnService;
    }

    public SingleLogoutService getSingleLogoutService() {
        return singleLogoutService;
    }

    public void setSingleLogoutService(SingleLogoutService singleLogoutService) {
        this.singleLogoutService = singleLogoutService;
    }

    public List<Key> getKeys() {
        return keys;
    }

    public void setKeys(List<Key> keys) {
        this.keys = keys;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getSignatureCanonicalizationMethod() {
        return signatureCanonicalizationMethod;
    }

    public void setSignatureCanonicalizationMethod(String signatureCanonicalizationMethod) {
        this.signatureCanonicalizationMethod = signatureCanonicalizationMethod;
    }

    public AdapterHttpClientConfig getHttpClientConfig() {
        return httpClientConfig;
    }

    public void setHttpClientConfig(AdapterHttpClientConfig httpClientConfig) {
        this.httpClientConfig = httpClientConfig;
    }

}
