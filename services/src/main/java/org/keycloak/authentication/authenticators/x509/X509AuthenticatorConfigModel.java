/*
 * Copyright 2016 Analytical Graphics, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.authentication.authenticators.x509;

import org.keycloak.models.AuthenticatorConfigModel;

import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.CANONICAL_DN;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.CERTIFICATE_EXTENDED_KEY_USAGE;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.CERTIFICATE_KEY_USAGE;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.CERTIFICATE_POLICY;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.CERTIFICATE_POLICY_MODE;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.CERTIFICATE_POLICY_MODE_ALL;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.CERTIFICATE_POLICY_MODE_ANY;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.CONFIRMATION_PAGE_DISALLOWED;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.CRL_ABORT_IF_NON_UPDATED;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.CRL_RELATIVE_PATH;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.CUSTOM_ATTRIBUTE_NAME;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.DEFAULT_ATTRIBUTE_NAME;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.DEFAULT_MATCH_ALL_EXPRESSION;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.ENABLE_CRL;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.ENABLE_CRLDP;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.ENABLE_OCSP;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_CERT_CERTIFICATE_PEM;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_CERT_ISSUERDN;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_CERT_SERIALNUMBER;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_CERT_SERIALNUMBER_ISSUERDN;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_CERT_SHA256_THUMBPRINT;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_CERT_SUBJECTALTNAME_EMAIL;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_CERT_SUBJECTALTNAME_OTHERNAME;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_CERT_SUBJECTDN;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_CERT_SUBJECTDN_CN;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_CERT_SUBJECTDN_EMAIL;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_SELECTION;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.OCSPRESPONDER_CERTIFICATE;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.OCSPRESPONDER_URI;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.OCSP_FAIL_OPEN;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.REGULAR_EXPRESSION;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.REVALIDATE_CERTIFICATE;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.SERIALNUMBER_HEX;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.TIMESTAMP_VALIDATION;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.USERNAME_EMAIL_MAPPER;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.USER_ATTRIBUTE_MAPPER;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.USER_MAPPER_SELECTION;

/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 10/26/2016
 */

public class X509AuthenticatorConfigModel extends AuthenticatorConfigModel {

    private static final long serialVersionUID = 1L;

    public enum IdentityMapperType {
        USER_ATTRIBUTE(USER_ATTRIBUTE_MAPPER),
        USERNAME_EMAIL(USERNAME_EMAIL_MAPPER);

        private String name;
        IdentityMapperType(String name) {
            this.name = name;
        }
        public String getName() {  return this.name; }
        static public IdentityMapperType parse(String name) throws IllegalArgumentException, IndexOutOfBoundsException {
            if (name == null || name.trim().length() == 0)
                throw new IllegalArgumentException("name");

            for (IdentityMapperType value : IdentityMapperType.values()) {
                if (value.getName().equalsIgnoreCase(name))
                    return value;
            }
            throw new IndexOutOfBoundsException("name");
        }
    }

    public enum MappingSourceType {
        SERIALNUMBER(MAPPING_SOURCE_CERT_SERIALNUMBER),
        ISSUERDN(MAPPING_SOURCE_CERT_ISSUERDN),
        SUBJECTDN_CN(MAPPING_SOURCE_CERT_SUBJECTDN_CN),
        SUBJECTDN_EMAIL(MAPPING_SOURCE_CERT_SUBJECTDN_EMAIL),
        SUBJECTALTNAME_EMAIL(MAPPING_SOURCE_CERT_SUBJECTALTNAME_EMAIL),
        SUBJECTALTNAME_OTHERNAME(MAPPING_SOURCE_CERT_SUBJECTALTNAME_OTHERNAME),
        SUBJECTDN(MAPPING_SOURCE_CERT_SUBJECTDN),
        SHA256_THUMBPRINT(MAPPING_SOURCE_CERT_SHA256_THUMBPRINT),
        SERIALNUMBER_ISSUERDN(MAPPING_SOURCE_CERT_SERIALNUMBER_ISSUERDN),
        CERTIFICATE_PEM(MAPPING_SOURCE_CERT_CERTIFICATE_PEM);
        
        private String name;
        MappingSourceType(String name) {
            this.name = name;
        }
        public String getName() {  return this.name; }
        public static MappingSourceType parse(String name) throws IllegalArgumentException, IndexOutOfBoundsException {
            if (name == null || name.trim().length() == 0)
                throw new IllegalArgumentException("name");

            for (MappingSourceType value : MappingSourceType.values()) {
                if (value.getName().equalsIgnoreCase(name))
                    return value;
            }
            throw new IndexOutOfBoundsException("name");
        }
    }

    public enum CertificatePolicyModeType {
        ALL(CERTIFICATE_POLICY_MODE_ALL),
        ANY(CERTIFICATE_POLICY_MODE_ANY);

        private String mode;
        CertificatePolicyModeType(String mode) {
            this.mode = mode;
        }
        public String getMode() {  return this.mode; }
        public static CertificatePolicyModeType parse(String mode) throws IllegalArgumentException, IndexOutOfBoundsException {
            if (mode == null || mode.trim().length() == 0)
                throw new IllegalArgumentException("mode");

            for (CertificatePolicyModeType value : CertificatePolicyModeType.values()) {
                if (value.getMode().equalsIgnoreCase(mode))
                    return value;
            }
            throw new IndexOutOfBoundsException("mode");
        }
    }

    public X509AuthenticatorConfigModel(AuthenticatorConfigModel model) {
        this.setAlias(model.getAlias());
        this.setId(model.getId());
        this.setConfig(model.getConfig());
    }
    public X509AuthenticatorConfigModel() {

    }

    public boolean getCRLEnabled() {
        return Boolean.parseBoolean(getConfig().get(ENABLE_CRL));
    }

    public X509AuthenticatorConfigModel setCRLEnabled(boolean value) {
        getConfig().put(ENABLE_CRL, Boolean.toString(value));
        return this;
    }

    public boolean getOCSPEnabled() {
        return Boolean.parseBoolean(getConfig().get(ENABLE_OCSP));
    }

    public X509AuthenticatorConfigModel setOCSPEnabled(boolean value) {
        getConfig().put(ENABLE_OCSP, Boolean.toString(value));
        return this;
    }

    public boolean getOCSPFailOpen() {
        return Boolean.parseBoolean(getConfig().getOrDefault(OCSP_FAIL_OPEN, Boolean.toString(false)));
    }

    public X509AuthenticatorConfigModel setOCSPFailOpen(boolean value) {
        getConfig().put(OCSP_FAIL_OPEN, Boolean.toString(value));
        return this;
    }

    public boolean getCRLDistributionPointEnabled() {
        return Boolean.parseBoolean(getConfig().get(ENABLE_CRLDP));
    }

    public X509AuthenticatorConfigModel setCRLDistributionPointEnabled(boolean value) {
        getConfig().put(ENABLE_CRLDP, Boolean.toString(value));
        return this;
    }

    public String getCRLRelativePath() {
        return getConfig().getOrDefault(CRL_RELATIVE_PATH, null);
    }

    public X509AuthenticatorConfigModel setCRLRelativePath(String path) {
        if (path != null) {
            getConfig().put(CRL_RELATIVE_PATH, path);
        } else {
            getConfig().remove(CRL_RELATIVE_PATH);
        }
        return this;
    }

    public boolean getCrlAbortIfNonUpdated() {
        return Boolean.parseBoolean(getConfig().get(CRL_ABORT_IF_NON_UPDATED));
    }

    public X509AuthenticatorConfigModel setCrlAbortIfNonUpdated(boolean crlAbortIfNonUpdated) {
        getConfig().put(CRL_ABORT_IF_NON_UPDATED, Boolean.toString(crlAbortIfNonUpdated));
        return this;
    }

    public String getOCSPResponder() {
        return getConfig().getOrDefault(OCSPRESPONDER_URI, null);
    }

    public X509AuthenticatorConfigModel setOCSPResponder(String responderUri) {
        if (responderUri != null) {
            getConfig().put(OCSPRESPONDER_URI, responderUri);
        } else {
            getConfig().remove(OCSPRESPONDER_URI);
        }
        return this;
    }

    public String getOCSPResponderCertificate() {
        return getConfig().getOrDefault(OCSPRESPONDER_CERTIFICATE, null);
    }

    public X509AuthenticatorConfigModel setOCSPResponderCertificate(String responderCert) {
        if (responderCert != null) {
            getConfig().put(OCSPRESPONDER_CERTIFICATE, responderCert);
        } else {
            getConfig().remove(OCSPRESPONDER_CERTIFICATE);
        }
        return this;
    }

    public MappingSourceType getMappingSourceType() {
        return MappingSourceType.parse(getConfig().getOrDefault(MAPPING_SOURCE_SELECTION, MAPPING_SOURCE_CERT_SUBJECTDN));
    }

    public X509AuthenticatorConfigModel setMappingSourceType(MappingSourceType value) {
        getConfig().put(MAPPING_SOURCE_SELECTION, value.getName());
        return this;
    }

    public IdentityMapperType getUserIdentityMapperType() {
        return IdentityMapperType.parse(getConfig().getOrDefault(USER_MAPPER_SELECTION, USERNAME_EMAIL_MAPPER));
    }

    public X509AuthenticatorConfigModel setUserIdentityMapperType(IdentityMapperType value) {
        getConfig().put(USER_MAPPER_SELECTION, value.getName());
        return this;
    }

    public String getRegularExpression() {
        return getConfig().getOrDefault(REGULAR_EXPRESSION,DEFAULT_MATCH_ALL_EXPRESSION);
    }

    public X509AuthenticatorConfigModel setRegularExpression(String value) {
        if (value != null) {
            getConfig().put(REGULAR_EXPRESSION, value);
        } else {
            getConfig().remove(REGULAR_EXPRESSION);
        }
        return this;
    }

    public String getCustomAttributeName() {
        return getConfig().getOrDefault(CUSTOM_ATTRIBUTE_NAME, DEFAULT_ATTRIBUTE_NAME);
    }

    public X509AuthenticatorConfigModel setCustomAttributeName(String value) {
        if (value != null) {
            getConfig().put(CUSTOM_ATTRIBUTE_NAME, value);
        } else {
            getConfig().remove(CUSTOM_ATTRIBUTE_NAME);
        }
        return this;
    }

    public String getKeyUsage() {
        return getConfig().getOrDefault(CERTIFICATE_KEY_USAGE, null);
    }

    public X509AuthenticatorConfigModel setKeyUsage(String value) {
        if (value != null) {
            getConfig().put(CERTIFICATE_KEY_USAGE, value);
        } else {
            getConfig().remove(CERTIFICATE_KEY_USAGE);
        }
        return this;
    }

    public String getExtendedKeyUsage() {
        return getConfig().getOrDefault(CERTIFICATE_EXTENDED_KEY_USAGE, null);
    }

    public X509AuthenticatorConfigModel setExtendedKeyUsage(String value) {
        if (value != null) {
            getConfig().put(CERTIFICATE_EXTENDED_KEY_USAGE, value);
        } else {
            getConfig().remove(CERTIFICATE_EXTENDED_KEY_USAGE);
        }
        return this;
    }

    public String getCertificatePolicy() {
        return getConfig().getOrDefault(CERTIFICATE_POLICY, null);
    }

    public X509AuthenticatorConfigModel setCertificatePolicy(String value) {
        if (value != null) {
            getConfig().put(CERTIFICATE_POLICY, value);
        } else {
            getConfig().remove(CERTIFICATE_POLICY);
        }
        return this;
    }

    public CertificatePolicyModeType getCertificatePolicyMode() {
        return CertificatePolicyModeType.parse(getConfig().getOrDefault(CERTIFICATE_POLICY_MODE, CERTIFICATE_POLICY_MODE_ALL));
    }

    public X509AuthenticatorConfigModel setCertificatePolicyMode(CertificatePolicyModeType value) {
        getConfig().put(CERTIFICATE_POLICY_MODE, value.getMode());
        return this;
    }

    public boolean getConfirmationPageDisallowed() {
        return Boolean.parseBoolean(getConfig().get(CONFIRMATION_PAGE_DISALLOWED));
    }

    public boolean getConfirmationPageAllowed() {
        return !Boolean.parseBoolean(getConfig().get(CONFIRMATION_PAGE_DISALLOWED));
    }

    public X509AuthenticatorConfigModel setConfirmationPageDisallowed(boolean value) {
        getConfig().put(CONFIRMATION_PAGE_DISALLOWED, Boolean.toString(value));
        return this;
    }

    public X509AuthenticatorConfigModel setConfirmationPageAllowed(boolean value) {
        getConfig().put(CONFIRMATION_PAGE_DISALLOWED, Boolean.toString(!value));
        return this;
    }

    public boolean isCanonicalDnEnabled() {
        return Boolean.parseBoolean(getConfig().get(CANONICAL_DN));
    }

    public X509AuthenticatorConfigModel setCanonicalDnEnabled(boolean value) {
        getConfig().put(CANONICAL_DN, Boolean.toString(value));
        return this;
    }

    public boolean isCertValidationEnabled() {
        return Boolean.parseBoolean(getConfig().get(TIMESTAMP_VALIDATION));
    }

    public X509AuthenticatorConfigModel setCertValidationEnabled(boolean value) {
        getConfig().put(TIMESTAMP_VALIDATION, Boolean.toString(value));
        return this;
    }
    
    public boolean isSerialnumberHex() {
        return Boolean.parseBoolean(getConfig().get(SERIALNUMBER_HEX));
    }

    public X509AuthenticatorConfigModel setSerialnumberHex(boolean value) {
        getConfig().put(SERIALNUMBER_HEX, Boolean.toString(value));
        return this;
    }

    public boolean getRevalidateCertificateEnabled() {
        return Boolean.parseBoolean(getConfig().get(REVALIDATE_CERTIFICATE));
    }

    public X509AuthenticatorConfigModel setRevalidateCertificateEnabled(boolean value) {
        getConfig().put(REVALIDATE_CERTIFICATE, Boolean.toString(value));
        return this;
    }
}
