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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static java.util.Arrays.asList;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.ServicesLogger;

import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.*;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.CERTIFICATE_KEY_USAGE;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.CONFIRMATION_PAGE_DISALLOWED;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.CRL_RELATIVE_PATH;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.CUSTOM_ATTRIBUTE_NAME;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.DEFAULT_ATTRIBUTE_NAME;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.DEFAULT_MATCH_ALL_EXPRESSION;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.ENABLE_CRL;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.ENABLE_CRLDP;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.ENABLE_OCSP;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_CERT_ISSUERDN;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_CERT_SERIALNUMBER;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_CERT_SUBJECTALTNAME_EMAIL;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_CERT_SUBJECTALTNAME_OTHERNAME;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_CERT_SUBJECTDN;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_CERT_SUBJECTDN_CN;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_CERT_SUBJECTDN_EMAIL;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.MAPPING_SOURCE_SELECTION;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.OCSPRESPONDER_CERTIFICATE;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.OCSPRESPONDER_URI;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.REGULAR_EXPRESSION;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.USERNAME_EMAIL_MAPPER;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.USER_ATTRIBUTE_MAPPER;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.USER_MAPPER_SELECTION;
import static org.keycloak.provider.ProviderConfigProperty.BOOLEAN_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.MULTIVALUED_STRING_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.TEXT_TYPE;

/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @date 7/31/2016
 */

public abstract class AbstractX509ClientCertificateAuthenticatorFactory implements AuthenticatorFactory {

    protected static ServicesLogger logger = ServicesLogger.LOGGER;

    private static final String[] mappingSources = {
            MAPPING_SOURCE_CERT_SUBJECTDN,
            MAPPING_SOURCE_CERT_SUBJECTDN_EMAIL,
            MAPPING_SOURCE_CERT_SUBJECTALTNAME_EMAIL,
            MAPPING_SOURCE_CERT_SUBJECTALTNAME_OTHERNAME,
            MAPPING_SOURCE_CERT_SUBJECTDN_CN,
            MAPPING_SOURCE_CERT_ISSUERDN,
            MAPPING_SOURCE_CERT_SERIALNUMBER,
            MAPPING_SOURCE_CERT_SERIALNUMBER_ISSUERDN,
            MAPPING_SOURCE_CERT_SHA256_THUMBPRINT,
            MAPPING_SOURCE_CERT_CERTIFICATE_PEM
    };

    private static final String[] userModelMappers = {
            USER_ATTRIBUTE_MAPPER,
            USERNAME_EMAIL_MAPPER
    };

    protected static final List<ProviderConfigProperty> configProperties;
    static {
        List<String> mappingSourceTypes = new LinkedList<>();
        Collections.addAll(mappingSourceTypes, mappingSources);
        ProviderConfigProperty mappingMethodList = new ProviderConfigProperty();
        mappingMethodList.setType(ProviderConfigProperty.LIST_TYPE);
        mappingMethodList.setName(MAPPING_SOURCE_SELECTION);
        mappingMethodList.setLabel("User Identity Source");
        mappingMethodList.setHelpText("Choose how to extract user identity from X509 certificate or the certificate fields. For example, SubjectDN will match the custom regular expression specified below to the value of certificate's SubjectDN field.");
        mappingMethodList.setDefaultValue(mappingSources[0]);
        mappingMethodList.setOptions(mappingSourceTypes);

        ProviderConfigProperty canonicalDn = new ProviderConfigProperty();
        canonicalDn.setType(BOOLEAN_TYPE);
        canonicalDn.setName(CANONICAL_DN);
        canonicalDn.setLabel("Canonical DN representation enabled");
        canonicalDn.setDefaultValue(false);
        canonicalDn.setHelpText("Use the canonical format to determine the distinguished name. This option is relevant for authenticators using a distinguished name.");

        ProviderConfigProperty serialnumberHex = new ProviderConfigProperty();
        serialnumberHex.setType(BOOLEAN_TYPE);
        serialnumberHex.setName(SERIALNUMBER_HEX);
        serialnumberHex.setLabel("Enable Serial Number hexadecimal representation");
        serialnumberHex.setDefaultValue(false);
        serialnumberHex.setHelpText("Use the hex representation of the serial number. This option is relevant for authenticators using serial number.");

        
        ProviderConfigProperty regExp = new ProviderConfigProperty();
        regExp.setType(STRING_TYPE);
        regExp.setName(REGULAR_EXPRESSION);
        regExp.setLabel("A regular expression to extract user identity");
        regExp.setDefaultValue(DEFAULT_MATCH_ALL_EXPRESSION);
        regExp.setHelpText("The regular expression to extract a user identity. The expression must contain a single group. For example, 'uniqueId=(.*?)(?:,|$)' will match 'uniqueId=somebody@company.org, CN=somebody' and give somebody@company.org");

        List<String> mapperTypes = new LinkedList<>();
        Collections.addAll(mapperTypes, userModelMappers);

        ProviderConfigProperty userMapperList = new ProviderConfigProperty();
        userMapperList.setType(ProviderConfigProperty.LIST_TYPE);
        userMapperList.setName(USER_MAPPER_SELECTION);
        userMapperList.setHelpText("Choose how to map extracted user identities to users");
        userMapperList.setLabel("User mapping method");
        userMapperList.setDefaultValue(userModelMappers[0]);
        userMapperList.setOptions(mapperTypes);

        ProviderConfigProperty attributeOrPropertyValue = new ProviderConfigProperty();
        attributeOrPropertyValue.setType(MULTIVALUED_STRING_TYPE);
        attributeOrPropertyValue.setName(CUSTOM_ATTRIBUTE_NAME);
        attributeOrPropertyValue.setDefaultValue(DEFAULT_ATTRIBUTE_NAME);
        attributeOrPropertyValue.setLabel("A name of user attribute");
        attributeOrPropertyValue.setHelpText("A name of user attribute to map the extracted user identity to existing user. The name must be a valid, existing user attribute if User Mapping Method is set to Custom Attribute Mapper. " +
                "Multiple values are relevant when attribute mapping is related to multiple values, e.g. 'Certificate Serial Number and IssuerDN'");

        ProviderConfigProperty timestampValidationValue = new ProviderConfigProperty();
        timestampValidationValue.setType(BOOLEAN_TYPE);
        timestampValidationValue.setName(TIMESTAMP_VALIDATION);
        timestampValidationValue.setLabel("Check certificate validity");
        timestampValidationValue.setDefaultValue(true);
        timestampValidationValue.setHelpText("Will verify that the certificate has not expired yet and is already valid by checking the attributes 'notBefore' and 'notAfter'.");

        ProviderConfigProperty crlCheckingEnabled = new ProviderConfigProperty();
        crlCheckingEnabled.setType(BOOLEAN_TYPE);
        crlCheckingEnabled.setName(ENABLE_CRL);
        crlCheckingEnabled.setHelpText("Enable Certificate Revocation Checking using CRL");
        crlCheckingEnabled.setLabel("CRL Checking Enabled");

        ProviderConfigProperty crlDPEnabled = new ProviderConfigProperty();
        crlDPEnabled.setType(BOOLEAN_TYPE);
        crlDPEnabled.setName(ENABLE_CRLDP);
        crlDPEnabled.setDefaultValue(false);
        crlDPEnabled.setLabel("Enable CRL Distribution Point to check certificate revocation status");
        crlDPEnabled.setHelpText("CRL Distribution Point is a starting point for CRL. If this is ON, then CRL checking will be done based on the CRL distribution points included" +
                " in the checked certificates. CDP is optional, but most PKI authorities include CDP in their certificates.");

        ProviderConfigProperty cRLRelativePath = new ProviderConfigProperty();
        cRLRelativePath.setType(MULTIVALUED_STRING_TYPE);
        cRLRelativePath.setName(CRL_RELATIVE_PATH);
        cRLRelativePath.setDefaultValue("crl.pem");
        cRLRelativePath.setLabel("CRL Path");
        cRLRelativePath.setHelpText("Applied just if CRL checking is ON and CRL Distribution point is OFF. It contains the URL (typically 'http' or 'ldap') " +
                "where the CRL is available. Alternatively it can contain the path to a CRL file that contains a list of revoked certificates. Paths are assumed to be relative to $jboss.server.config.dir. " +
                "Multiple CRLs can be included, however it can affect performance as the certificate will be checked against all listed CRLs."
        );

        ProviderConfigProperty oCspCheckingEnabled = new ProviderConfigProperty();
        oCspCheckingEnabled.setType(BOOLEAN_TYPE);
        oCspCheckingEnabled.setName(ENABLE_OCSP);
        oCspCheckingEnabled.setHelpText("Enable Certificate Revocation Checking using OCSP");
        oCspCheckingEnabled.setLabel("OCSP Checking Enabled");

        ProviderConfigProperty ocspResponderUri = new ProviderConfigProperty();
        ocspResponderUri.setType(STRING_TYPE);
        ocspResponderUri.setName(OCSPRESPONDER_URI);
        ocspResponderUri.setLabel("OCSP Responder Uri");
        ocspResponderUri.setHelpText("Clients use OCSP Responder Uri to check certificate revocation status.");

        ProviderConfigProperty ocspResponderCert = new ProviderConfigProperty();
        ocspResponderCert.setType(TEXT_TYPE);
        ocspResponderCert.setName(OCSPRESPONDER_CERTIFICATE);
        ocspResponderCert.setLabel("OCSP Responder Certificate");
        ocspResponderCert.setHelpText("Optional certificate used by the responder to sign the responses. The certificate should be in PEM format without BEGIN and END tags. It is only used if the OCSP Responder URI is set. By default, the certificate of the OCSP responder is that of the issuer of the certificate being validated or one with the OCSPSigning extension and also issued by the same CA. This option identifies the certificate of the OCSP responder when the defaults do not apply.");

        ProviderConfigProperty keyUsage = new ProviderConfigProperty();
        keyUsage.setType(STRING_TYPE);
        keyUsage.setName(CERTIFICATE_KEY_USAGE);
        keyUsage.setLabel("Validate Key Usage");
        keyUsage.setHelpText("Validates that the purpose of the key contained in the certificate (encipherment, signature, etc.) matches its intended purpose. Leaving the field blank will disable Key Usage validation. For example, 'digitalSignature, keyEncipherment' will check if the digitalSignature and keyEncipherment bits (bit 0 and bit 2 respectively) are set in certificate's X509 Key Usage extension. See RFC 5280 for a detailed definition of X509 Key Usage extension.");

        ProviderConfigProperty extendedKeyUsage = new ProviderConfigProperty();
        extendedKeyUsage.setType(STRING_TYPE);
        extendedKeyUsage.setName(CERTIFICATE_EXTENDED_KEY_USAGE);
        extendedKeyUsage.setLabel("Validate Extended Key Usage");
        extendedKeyUsage.setHelpText("Validates the extended purposes of the certificate's key using certificate's Extended Key Usage extension. Leaving the field blank will disable Extended Key Usage validation. See RFC 5280 for a detailed definition of X509 Extended Key Usage extension.");

        ProviderConfigProperty identityConfirmationPageDisallowed = new ProviderConfigProperty();
        identityConfirmationPageDisallowed.setType(BOOLEAN_TYPE);
        identityConfirmationPageDisallowed.setName(CONFIRMATION_PAGE_DISALLOWED);
        identityConfirmationPageDisallowed.setLabel("Bypass identity confirmation");
        identityConfirmationPageDisallowed.setHelpText("By default, the users are prompted to confirm their identity extracted from X509 client certificate. The identity confirmation prompt is skipped if the option is switched on.");

        configProperties = asList(mappingMethodList,
                canonicalDn,
                serialnumberHex,
                regExp,
                userMapperList,
                attributeOrPropertyValue,
                timestampValidationValue,
                crlCheckingEnabled,
                crlDPEnabled,
                cRLRelativePath,
                oCspCheckingEnabled,
                ocspResponderUri,
                ocspResponderCert,
                keyUsage,
                extendedKeyUsage,
                identityConfirmationPageDisallowed);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

}
