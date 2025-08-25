package org.keycloak.authentication.authenticators.client;

import jakarta.ws.rs.core.Response;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.crypto.ClientSignatureVerifierProvider;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.keys.loader.PublicKeyStorageManager;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ServicesLogger;

import java.security.PublicKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.keycloak.models.TokenManager.DEFAULT_VALIDATOR;

public abstract class AbstractJWTClientAuthenticator  extends AbstractClientAuthenticator {

    protected static final String ATTR_PREFIX = "jwt.credential";
    protected static final String CERTIFICATE_ATTR = "jwt.credential.certificate";

    @Override
    public void authenticateClient(ClientAuthenticationFlowContext context) {
        JWTClientValidator validator = getValidator(context);
        if (!validator.clientAssertionParametersValidation()) return;

        try {
            validator.readJws();
            if (!validator.validateClient()) return;
            if (!validator.validateSignatureAlgorithm()) return;

            RealmModel realm = validator.getRealm();
            ClientModel client = validator.getClient();
            JWSInput jws = validator.getJws();
            JsonWebToken token = validator.getToken();
            String clientAssertion = validator.getClientAssertion();

            // Get client key and validate signature
            PublicKey clientPublicKey = getSignatureValidationKey(client, context, jws);
            if (clientPublicKey == null) {
                // Error response already set to context
                return;
            }

            boolean signatureValid;
            try {
                JsonWebToken jwt = context.getSession().tokens().decodeClientJWT(clientAssertion, client, (jose, validatedClient) -> {
                    DEFAULT_VALIDATOR.accept(jose, validatedClient);
                    String signatureAlgorithm = jose.getHeader().getRawAlgorithm();
                    ClientSignatureVerifierProvider signatureProvider = context.getSession().getProvider(ClientSignatureVerifierProvider.class, signatureAlgorithm);
                    if (signatureProvider == null) {
                        throw new RuntimeException("Algorithm not supported");
                    }
                    if (!signatureProvider.isAsymmetricAlgorithm()) {
                        throw new RuntimeException("Algorithm is not asymmetric");
                    }
                }, JsonWebToken.class);
                signatureValid = jwt != null;
            } catch (RuntimeException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new RuntimeException("Signature on JWT token failed validation", cause);
            }
            if (!signatureValid) {
                throw new RuntimeException("Signature on JWT token failed validation");
            }

            validator.validateTokenAudience(context, realm, token);

            validator.validateToken();
            validator.validateTokenReuse();

            context.success();
        } catch (Exception e) {
            ServicesLogger.LOGGER.errorValidatingAssertion(e);
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), OAuthErrorException.INVALID_CLIENT, "Client authentication with signed JWT failed: " + e.getMessage());
            context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, challengeResponse);
        }
    }

    protected PublicKey getSignatureValidationKey(ClientModel client, ClientAuthenticationFlowContext context, JWSInput jws) {
        PublicKey publicKey = PublicKeyStorageManager.getClientPublicKey(context.getSession(), client, jws);
        if (publicKey == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), OAuthErrorException.INVALID_CLIENT, "Unable to load public key");
            context.failure(AuthenticationFlowError.CLIENT_CREDENTIALS_SETUP_REQUIRED, challengeResponse);
            return null;
        } else {
            return publicKey;
        }
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    public List<ProviderConfigProperty> getConfigPropertiesPerClient() {
        // This impl doesn't use generic screen in admin console, but has its own screen. So no need to return anything here
        return Collections.emptyList();
    }

    @Override
    public Set<String> getProtocolAuthenticatorMethods(String loginProtocol) {
        if (loginProtocol.equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
            Set<String> results = new HashSet<>();
            results.add(OIDCLoginProtocol.PRIVATE_KEY_JWT);
            return results;
        } else {
            return Collections.emptySet();
        }
    }

    protected abstract JWTClientValidator getValidator(ClientAuthenticationFlowContext context);

}
