package org.keycloak.broker.jwtauthorizationgrant;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.JWTAuthorizationGrantProvider;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.keys.loader.PublicKeyStorageManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.JWTAuthorizationGrantValidationContext;
import org.keycloak.services.Urls;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

public class JWTAuthorizationGrantIdentityProvider implements JWTAuthorizationGrantProvider<JWTAuthorizationGrantIdentityProviderConfig> {
    private static final Logger LOGGER = Logger.getLogger(JWTAuthorizationGrantIdentityProvider.class);

    private final KeycloakSession session;
    private final JWTAuthorizationGrantConfig config;

    public JWTAuthorizationGrantIdentityProvider(KeycloakSession session, JWTAuthorizationGrantConfig config) {
        this.session = session;
        this.config = config;
    }

    @Override
    public BrokeredIdentityContext validateAuthorizationGrantAssertion(JWTAuthorizationGrantValidationContext context) throws IdentityBrokerException {
        // verify signature
        if (!verifySignature(context.getJws())) {
            throw new IdentityBrokerException("Invalid signature");
        }

        BrokeredIdentityContext user = new BrokeredIdentityContext(context.getJWT().getSubject(), getConfig());
        user.setUsername(context.getJWT().getSubject());
        return user;
    }

    @Override
    public int getAllowedClockSkew() {
        return config.getJWTAuthorizationGrantAllowedClockSkew();
    }

    @Override
    public boolean isAssertionReuseAllowed() {
        return config.isJWTAuthorizationGrantAssertionReuseAllowed();
    }

    @Override
    public List<String> getAllowedAudienceForJWTGrant() {
        RealmModel realm = session.getContext().getRealm();

        URI baseUri = session.getContext().getUri().getBaseUri();
        String issuer = Urls.realmIssuer(baseUri, realm.getName());
        String tokenEndpoint = Urls.tokenEndpoint(baseUri, realm.getName()).toString();
        return List.of(issuer, tokenEndpoint);
    }

    @Override
    public int getMaxAllowedExpiration() {
        return config.getJWTAuthorizationGrantMaxAllowedAssertionExpiration();
    }

    @Override
    public String getAssertionSignatureAlg() {
        String alg = config.getJWTAuthorizationGrantAssertionSignatureAlg();
        return StringUtil.isBlank(alg) ? null : alg;
    }

    @Override
    public boolean isLimitAccessTokenExpiration() {
        return getConfig().isJwtAuthorizationGrantLimitAccessTokenExp();
    }

    @Override
    public JWTAuthorizationGrantIdentityProviderConfig getConfig() {
        return this.config instanceof  JWTAuthorizationGrantIdentityProviderConfig ? (JWTAuthorizationGrantIdentityProviderConfig)this.config : null;
    }

    private boolean verifySignature(JWSInput jws) {
        try {
            JWSHeader header = jws.getHeader();
            String alg = header.getRawAlgorithm();

            KeyWrapper publicKey = PublicKeyStorageManager.getIdentityProviderKeyWrapper(session, session.getContext().getRealm(), getConfig(), jws);
            if (publicKey == null) {
                LOGGER.debugf("Failed to verify token, key not found for algorithm %s", alg);
                return false;
            }

            SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, alg);
            if (signatureProvider == null) {
                LOGGER.debugf("Failed to verify token signature, signature provider not found for algorithm %s", alg);
                return false;
            }

            return signatureProvider.verifier(publicKey).verify(jws.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8), jws.getSignature());
        } catch (Exception e) {
            LOGGER.debug("Failed to verify token signature", e);
            return false;
        }
    }

    @Override
    public void close() {

    }
}
