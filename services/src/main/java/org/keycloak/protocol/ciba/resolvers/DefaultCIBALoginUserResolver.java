package org.keycloak.protocol.ciba.resolvers;

import org.jboss.logging.Logger;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ciba.CIBAErrorCodes;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.LoginHintToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;

import javax.ws.rs.core.Response;

public class DefaultCIBALoginUserResolver implements CIBALoginUserResolver {

    private static final Logger logger = Logger.getLogger(DefaultCIBALoginUserResolver.class);

    private final KeycloakSession session;

    public DefaultCIBALoginUserResolver(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public UserModel getUserFromLoginHint(String loginHint) {
        UserModel userModel = KeycloakModelUtils.findUserByNameOrEmail(session, session.getContext().getRealm(), loginHint);
        if (userModel == null) {
            throw new ErrorResponseException(CIBAErrorCodes.UNKNOWN_USER_ID, "no user found", Response.Status.BAD_REQUEST);
        }
        return userModel;
    }

    @Override
    public UserModel getUserFromLoginHintToken(String loginHintToken) {
        try {
            TokenVerifier<LoginHintToken> verifier = TokenVerifier.create(loginHintToken, LoginHintToken.class)
                                                             .realmUrl(Urls.realmIssuer(session.getContext().getUri().getBaseUri(),
                                                                                        session.getContext().getRealm().getName()))
                                                             .audience(session.getContext().getClient().getClientId());

            SignatureVerifierContext verifierContext = session.getProvider(SignatureProvider.class, verifier.getHeader().getAlgorithm().name()).verifier(verifier.getHeader().getKeyId());
            verifier.verifierContext(verifierContext);

            LoginHintToken token = verifier.verify().getToken();

            UserModel userModel = getUserFromInfoUsedByAuthentication(token.getPreferredUsername());
            if (userModel == null) {
                throw new ErrorResponseException(CIBAErrorCodes.UNKNOWN_USER_ID, "no user found", Response.Status.BAD_REQUEST);
            }
            return userModel;
        } catch (VerificationException e) {
            logger.warn("Failed verify user hint token", e);
            throw new ErrorResponseException(CIBAErrorCodes.INVALID_REQUEST, "Token verification failed", Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public UserModel getUserFromIdTokenHint(String idTokenHint) {
        try {
            TokenVerifier<IDToken> verifier = TokenVerifier.create(idTokenHint, IDToken.class)
                                                             .realmUrl(Urls.realmIssuer(session.getContext().getUri().getBaseUri(),
                                                                                        session.getContext().getRealm().getName()))
                                                             .audience(session.getContext().getClient().getClientId());

            SignatureVerifierContext verifierContext = session.getProvider(SignatureProvider.class, verifier.getHeader().getAlgorithm().name()).verifier(verifier.getHeader().getKeyId());
            verifier.verifierContext(verifierContext);

            IDToken token = verifier.verify().getToken();

            UserModel userModel = getUserFromInfoUsedByAuthentication(token.getPreferredUsername());
            if (userModel == null) {
                throw new ErrorResponseException(CIBAErrorCodes.UNKNOWN_USER_ID, "no user found", Response.Status.BAD_REQUEST);
            }
            return userModel;
        } catch (VerificationException e) {
            logger.warn("Failed verify id token", e);
            throw new ErrorResponseException(CIBAErrorCodes.INVALID_REQUEST, "Token verification failed", Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public String getInfoUsedByAuthentication(UserModel user) {
        return user.getUsername();
    }

    @Override
    public UserModel getUserFromInfoUsedByAuthentication(String info) {
        return KeycloakModelUtils.findUserByNameOrEmail(session, session.getContext().getRealm(), info);
    }

    @Override
    public void close() {
    }

}
