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

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.function.Function;

import javax.ws.rs.core.Response;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;

/**
 * @author <a href="mailto:pnalyvayko@agi.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @date 7/31/2016
 */

public abstract class AbstractX509ClientCertificateAuthenticator implements Authenticator {

    public static final String DEFAULT_ATTRIBUTE_NAME = "usercertificate";
    protected static ServicesLogger logger = ServicesLogger.LOGGER;

    public static final String JAVAX_SERVLET_REQUEST_X509_CERTIFICATE = "javax.servlet.request.X509Certificate";

    public static final String REGULAR_EXPRESSION = "x509-cert-auth.regular-expression";
    public static final String ENABLE_CRL = "x509-cert-auth.crl-checking-enabled";
    public static final String ENABLE_OCSP = "x509-cert-auth.ocsp-checking-enabled";
    public static final String ENABLE_CRLDP = "x509-cert-auth.crldp-checking-enabled";
    public static final String CRL_RELATIVE_PATH = "x509-cert-auth.crl-relative-path";
    public static final String OCSPRESPONDER_URI = "x509-cert-auth.ocsp-responder-uri";
    public static final String MAPPING_SOURCE_SELECTION = "x509-cert-auth.mapping-source-selection";
    public static final String MAPPING_SOURCE_CERT_SUBJECTDN = "Match SubjectDN using regular expression";
    public static final String MAPPING_SOURCE_CERT_SUBJECTDN_EMAIL = "Subject's e-mail";
    public static final String MAPPING_SOURCE_CERT_SUBJECTDN_CN = "Subject's Common Name";
    public static final String MAPPING_SOURCE_CERT_ISSUERDN = "Match IssuerDN using regular expression";
    public static final String MAPPING_SOURCE_CERT_ISSUERDN_EMAIL = "Issuer's e-mail";
    public static final String MAPPING_SOURCE_CERT_ISSUERDN_CN = "Issuer's Common Name";
    public static final String MAPPING_SOURCE_CERT_SERIALNUMBER = "Certificate Serial Number";
    public static final String USER_MAPPER_SELECTION = "x509-cert-auth.mapper-selection";
    public static final String USER_ATTRIBUTE_MAPPER = "Custom Attribute Mapper";
    public static final String USERNAME_EMAIL_MAPPER = "Username or Email";
    public static final String CUSTOM_ATTRIBUTE_NAME = "x509-cert-auth.mapper-selection.user-attribute-name";
    public static final String CERTIFICATE_KEY_USAGE = "x509-cert-auth.keyusage";
    public static final String CERTIFICATE_EXTENDED_KEY_USAGE = "x509-cert-auth.extendedkeyusage";
    static final String DEFAULT_MATCH_ALL_EXPRESSION = "(.*?)(?:$)";
    public static final String CONFIRMATION_PAGE_DISALLOWED = "x509-cert-auth.confirmation-page-disallowed";


    protected Response createInfoResponse(AuthenticationFlowContext context, String infoMessage, Object ... parameters) {
        LoginFormsProvider form = context.form();
        return form.setInfo(infoMessage, parameters).createInfoPage();
    }

    protected static class CertificateValidatorConfigBuilder {

        static CertificateValidator.CertificateValidatorBuilder fromConfig(X509AuthenticatorConfigModel config) throws Exception {

            CertificateValidator.CertificateValidatorBuilder builder = new CertificateValidator.CertificateValidatorBuilder();
            return builder
                    .keyUsage()
                        .parse(config.getKeyUsage())
                    .extendedKeyUsage()
                        .parse(config.getExtendedKeyUsage())
                    .revocation()
                        .cRLEnabled(config.getCRLEnabled())
                        .cRLDPEnabled(config.getCRLDistributionPointEnabled())
                        .cRLrelativePath(config.getCRLRelativePath())
                        .oCSPEnabled(config.getOCSPEnabled())
                        .oCSPResponderURI(config.getOCSPResponder());
        }
    }

    // The method is purely for purposes of facilitating the unit testing
    public CertificateValidator.CertificateValidatorBuilder certificateValidationParameters(X509AuthenticatorConfigModel config) throws Exception {
        return CertificateValidatorConfigBuilder.fromConfig(config);
    }

    protected static class UserIdentityExtractorBuilder {

        private static final Function<X509Certificate[],X500Name> subject = certs -> {
            try {
                return new JcaX509CertificateHolder(certs[0]).getSubject();
            } catch (CertificateEncodingException e) {
                logger.warn("Unable to get certificate Subject", e);
            }
            return null;
        };

        private static final Function<X509Certificate[],X500Name> issuer = certs -> {
            try {
                return new JcaX509CertificateHolder(certs[0]).getIssuer();
            } catch (CertificateEncodingException e) {
                logger.warn("Unable to get certificate Issuer", e);
            }
            return null;
        };

        static UserIdentityExtractor fromConfig(X509AuthenticatorConfigModel config) {

            X509AuthenticatorConfigModel.MappingSourceType userIdentitySource = config.getMappingSourceType();
            String pattern = config.getRegularExpression();

            UserIdentityExtractor extractor = null;
            switch(userIdentitySource) {

                case SUBJECTDN:
                    extractor = UserIdentityExtractor.getPatternIdentityExtractor(pattern, certs -> certs[0].getSubjectDN().getName());
                    break;
                case ISSUERDN:
                    extractor = UserIdentityExtractor.getPatternIdentityExtractor(pattern, certs -> certs[0].getIssuerDN().getName());
                    break;
                case SERIALNUMBER:
                    extractor = UserIdentityExtractor.getPatternIdentityExtractor(DEFAULT_MATCH_ALL_EXPRESSION, certs -> certs[0].getSerialNumber().toString());
                    break;
                case SUBJECTDN_CN:
                    extractor = UserIdentityExtractor.getX500NameExtractor(BCStyle.CN, subject);
                    break;
                case SUBJECTDN_EMAIL:
                    extractor = UserIdentityExtractor
                            .either(UserIdentityExtractor.getX500NameExtractor(BCStyle.EmailAddress, subject))
                            .or(UserIdentityExtractor.getX500NameExtractor(BCStyle.E, subject));
                    break;
                case ISSUERDN_CN:
                    extractor = UserIdentityExtractor.getX500NameExtractor(BCStyle.CN, issuer);
                    break;
                case ISSUERDN_EMAIL:
                    extractor = UserIdentityExtractor
                            .either(UserIdentityExtractor.getX500NameExtractor(BCStyle.EmailAddress, issuer))
                            .or(UserIdentityExtractor.getX500NameExtractor(BCStyle.E, issuer));
                    break;
                default:
                    logger.warnf("[UserIdentityExtractorBuilder:fromConfig] Unknown or unsupported user identity source: \"%s\"", userIdentitySource.getName());
                    break;
            }
            return extractor;
        }
    }

    protected static class UserIdentityToModelMapperBuilder {

        static UserIdentityToModelMapper fromConfig(X509AuthenticatorConfigModel config) {

            X509AuthenticatorConfigModel.IdentityMapperType mapperType = config.getUserIdentityMapperType();
            String attributeName = config.getCustomAttributeName();

            UserIdentityToModelMapper mapper = null;
            switch (mapperType) {
                case USER_ATTRIBUTE:
                    mapper = UserIdentityToModelMapper.getUserIdentityToCustomAttributeMapper(attributeName);
                    break;
                case USERNAME_EMAIL:
                    mapper = UserIdentityToModelMapper.getUsernameOrEmailMapper();
                    break;
                default:
                    logger.warnf("[UserIdentityToModelMapperBuilder:fromConfig] Unknown or unsupported user identity mapper: \"%s\"", mapperType.getName());
            }
            return mapper;
        }
    }

    @Override
    public void close() {

    }

    protected X509Certificate[] getCertificateChain(AuthenticationFlowContext context) {
        // Get a x509 client certificate
        X509Certificate[] certs = (X509Certificate[]) context.getHttpRequest().getAttribute(JAVAX_SERVLET_REQUEST_X509_CERTIFICATE);

        if (certs != null) {
            for (X509Certificate cert : certs) {
                logger.tracef("[X509ClientCertificateAuthenticator:getCertificateChain] \"%s\"", cert.getSubjectDN().getName());
            }
        }

        return certs;
    }
    // Purely for unit testing
    public UserIdentityExtractor getUserIdentityExtractor(X509AuthenticatorConfigModel config) {
        return UserIdentityExtractorBuilder.fromConfig(config);
    }
    // Purely for unit testing
    public UserIdentityToModelMapper getUserIdentityToModelMapper(X509AuthenticatorConfigModel config) {
        return UserIdentityToModelMapperBuilder.fromConfig(config);
    }
    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }
}
