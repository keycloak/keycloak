package org.keycloak.authentication.authenticators.client;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.authentication.authenticators.x509.CertificateValidator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.x509.X509ClientCertificateLookup;
import org.keycloak.utils.StringUtil;

public class X509ClientAuthenticator extends AbstractClientAuthenticator {

    public static final String PROVIDER_ID = "client-x509";
    public static final String ATTR_PREFIX = "x509";
    public static final String ATTR_SUBJECT_DN = ATTR_PREFIX + ".subjectdn";
    public static final String ATTR_CA_SUBJECT_DN = ATTR_PREFIX + ".casubjectdn";

    public static final String ATTR_ALLOW_REGEX_PATTERN_COMPARISON = ATTR_PREFIX + ".allow.regex.pattern.comparison";

    @Override
    public void authenticateClient(ClientAuthenticationFlowContext context) {

        X509ClientCertificateLookup provider = context.getSession().getProvider(X509ClientCertificateLookup.class);
        if (provider == null) {
            logger.errorv("\"{0}\" Spi is not available, did you forget to update the configuration?",
                    X509ClientCertificateLookup.class);
            return;
        }

        X509Certificate[] certs = null;
        ClientModel client = null;
        try {
            certs = provider.getCertificateChain(context.getHttpRequest());
            String client_id = null;
            MediaType mediaType = context.getHttpRequest().getHttpHeaders().getMediaType();
            boolean hasFormData = mediaType != null && mediaType.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE);

            MultivaluedMap<String, String> formData = hasFormData ? context.getHttpRequest().getDecodedFormParameters() : null;
            MultivaluedMap<String, String> queryParams = context.getSession().getContext().getUri().getQueryParameters();

            if (formData != null) {
                client_id = formData.getFirst(OAuth2Constants.CLIENT_ID);
            }

            if (client_id == null && queryParams != null) {
                client_id = queryParams.getFirst(OAuth2Constants.CLIENT_ID);
            }

            if (client_id == null) {
                client_id = context.getSession().getAttribute("client_id", String.class);
            }

            if (client_id == null) {
                Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "Missing client_id parameter");
                context.challenge(challengeResponse);
                return;
            }

            client = context.getRealm().getClientByClientId(client_id);
            if (client == null) {
                context.failure(AuthenticationFlowError.CLIENT_NOT_FOUND, null);
                return;
            }
            context.getEvent().client(client_id);
            context.setClient(client);

            if (!client.isEnabled()) {
                context.failure(AuthenticationFlowError.CLIENT_DISABLED, null);
                return;
            }
        } catch (GeneralSecurityException e) {
            logger.errorf("[X509ClientCertificateAuthenticator:authenticate] Exception: %s", e.getMessage());
            context.attempted();
            return;
        }

        if (certs == null || certs.length == 0) {
            // No x509 client cert, fall through and
            // continue processing the rest of the authentication flow
            logger.debug("[X509ClientCertificateAuthenticator:authenticate] x509 client certificate is not available for mutual SSL.");
            context.attempted();
            return;
        }

        OIDCAdvancedConfigWrapper clientCfg = OIDCAdvancedConfigWrapper.fromClientModel(client);
        String subjectDNRegexp = client.getAttribute(ATTR_SUBJECT_DN);
        if (StringUtil.isBlank(subjectDNRegexp)) {
            logger.errorf("[X509ClientCertificateAuthenticator:authenticate] %s is null or empty", ATTR_SUBJECT_DN);
            context.attempted();
            return;
        }

        // Testing only 1st certificate in the chain to match with configured subject
        X509Certificate certificate = certs[0];
        boolean matchedCertificate = checkSubjectDN(context, certificate, subjectDNRegexp, clientCfg.getAllowRegexPatternComparison());

        if (!matchedCertificate) {
            // We do quite expensive operation here, so better check the logging level beforehand.
            if (logger.isDebugEnabled()) {
                logger.debugf("[X509ClientCertificateAuthenticator:authenticate] Couldn't match any certificate for expected Subject DN '%s' with allow regex pattern '%s'.", subjectDNRegexp, clientCfg.getAllowRegexPatternComparison());
                logger.debugf("[X509ClientCertificateAuthenticator:authenticate] Checked Subject DN: %s", certificate.getSubjectDN().getName());
                logger.debugf("[X509ClientCertificateAuthenticator:authenticate] All SubjectDNs from the certificate chain: %s",
                        Arrays.stream(certs)
                                .map(cert -> cert.getSubjectDN().getName())
                                .collect(Collectors.toList()));
            }
            context.attempted();
            return;
        }

        // get the name of the CA to check
        String caSubjectDN = client.getAttribute(ATTR_CA_SUBJECT_DN);
        if (StringUtil.isBlank(caSubjectDN)) {
            // TODO: enforce CA subject for keycloak 27.0
            logger.warnf("[X509ClientCertificateAuthenticator:authenticate] option '%s' is null or empty, this configuration is deprecated, please configure it for better security for client '%s' in realm '%s'",
                    ATTR_CA_SUBJECT_DN, client.getClientId(), context.getRealm().getName());
            // if the attribute is not present, return success for backwards compatibility
            context.success();
            return;
        }

        // validate the certificate against the CA
        X509Certificate ca = validateCertificateChain(context.getSession(), caSubjectDN, certs);
        if (ca == null) {
            context.attempted();
            return;
        }

        logger.debugf("[X509ClientCertificateAuthenticator:authenticate] Matched %s certificate.", certificate.getSubjectDN().getName());
        context.success();
    }

    private boolean checkSubjectDN(ClientAuthenticationFlowContext context, X509Certificate certificate, String subjectDN, boolean isRegExp){
        if (isRegExp) {
            return checkSubjectDNRegex(context, certificate, subjectDN);
        } else {
            return CertificateValidator.checkSubjectDNExact(certificate, subjectDN);
        }
    }

    private boolean checkSubjectDNRegex(ClientAuthenticationFlowContext context, X509Certificate certificate, String subjectDN) {
        Pattern subjectDNPattern = Pattern.compile(subjectDN);

        // getSubjectDN is deprecated and says should not be relied upon by portable code, we are deprecating regex comparison
        // TODO: Remove this option in keycloak 27.0
        logger.warnf("Regex comparison is deprecated. Please configure the X.509 client authenticator to use exact Subject DN for client '%s' in realm '%s'.",
                context.getRealm().getName(), context.getClient().getClientId());
        String subjectdn = certificate.getSubjectDN().getName();
        return subjectDNPattern.matcher(subjectdn).matches();
    }

    private X509Certificate validateCertificateChain(KeycloakSession session, String caSubjectDN, X509Certificate[] certs) {
        try {
            CertificateValidator validator = new CertificateValidator.CertificateValidatorBuilder()
                    .session(session)
                    .trustValidation()
                        .caSubjectDN(Collections.singletonList(caSubjectDN))
                        .enabled(true)
                    .timestampValidation()
                        .enabled(true)
                    .build(certs);
            validator.checkRevocationStatus()
                    .validateTimestamps()
                    .validateTrust()
                    .validateCASubjectDN();
            return validator.getCertPathBuilderResult().getTrustAnchor().getTrustedCert();
        } catch (GeneralSecurityException e) {
            logger.warnf(e, "Invalid certificate %s", CertificateValidator.getSubjectName(certs[0]));
            return null;
        }
    }

    public String getDisplayType() {
        return "X509 Certificate";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public List<ProviderConfigProperty> getConfigPropertiesPerClient() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getAdapterConfiguration(KeycloakSession session, ClientModel client) {
        return Collections.emptyMap();
    }

   @Override
    public Set<String> getProtocolAuthenticatorMethods(String loginProtocol) {
        if (loginProtocol.equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
            Set<String> results = new HashSet<>();
            results.add(OIDCLoginProtocol.TLS_CLIENT_AUTH);
            return results;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public String getHelpText() {
        return "Validates client based on a X509 Certificate";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}
