package org.keycloak.authentication.authenticators.client;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.x509.X509ClientCertificateLookup;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.*;

public class X509ClientAuthenticator extends AbstractClientAuthenticator {

    public static final String PROVIDER_ID = "client-x509";
    protected static ServicesLogger logger = ServicesLogger.LOGGER;

    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public void authenticateClient(ClientAuthenticationFlowContext context) {

        X509ClientCertificateLookup provider = context.getSession().getProvider(X509ClientCertificateLookup.class);
        if (provider == null) {
            logger.errorv("\"{0}\" Spi is not available, did you forget to update the configuration?",
                    X509ClientCertificateLookup.class);
            return;
        }

        X509Certificate[] certs = new X509Certificate[0];
        try {
            certs = provider.getCertificateChain(context.getHttpRequest());
            String client_id = null;
            MediaType mediaType = context.getHttpRequest().getHttpHeaders().getMediaType();
            boolean hasFormData = mediaType != null && mediaType.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE);

            MultivaluedMap<String, String> formData = hasFormData ? context.getHttpRequest().getDecodedFormParameters() : null;
            MultivaluedMap<String, String> queryParams = context.getHttpRequest().getUri().getQueryParameters();

            if (formData != null) {
                client_id = formData.getFirst(OAuth2Constants.CLIENT_ID);
            }

            if (client_id == null) {
                if (queryParams != null) {
                    client_id = queryParams.getFirst(OAuth2Constants.CLIENT_ID);
                } else {
                    Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "Missing client_id parameter");
                    context.challenge(challengeResponse);
                    return;
                }
            }

            ClientModel client = context.getRealm().getClientByClientId(client_id);
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
        }

        if (certs == null || certs.length == 0) {
            // No x509 client cert, fall through and
            // continue processing the rest of the authentication flow
            logger.debug("[X509ClientCertificateAuthenticator:authenticate] x509 client certificate is not available for mutual SSL.");
            context.attempted();
            return;
        }

        context.success();
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
    public Map<String, Object> getAdapterConfiguration(ClientModel client) {
        Map<String, Object> result = new HashMap<>();
        return result;
    }

    @Override
    public Set<String> getProtocolAuthenticatorMethods(String loginProtocol) {
        if (loginProtocol.equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
            Set<String> results = new HashSet<>();
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
