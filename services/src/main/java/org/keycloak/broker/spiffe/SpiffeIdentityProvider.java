package org.keycloak.broker.spiffe;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ClientAssertionContext;
import org.keycloak.broker.provider.ClientAssertionIdentityProvider;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderDataMarshaller;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.keys.PublicKeyStorageUtils;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.Urls;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Implementation for https://datatracker.ietf.org/doc/draft-schwenkschuster-oauth-spiffe-client-auth/
 *
 * Main differences for SPIFFE JWT SVIDs and regular client assertions:
 * <ul>
*  <li><code>jwt-spiffe</code> client assertion type</li>
 * <li><code>iss</code> claim is optional, uses SPIFFE IDs, which includes trust domain instead</li>
 * <li><code>jti</code> claim is optional, and SPIFFE vendors re-use/cache tokens</li>
 * <li><code>sub</code> is a SPIFFE ID with the syntax <code>spiffe://trust-domain/workload-identity</code></li>
 * <li>Keys are fetched from a SPIFFE bundle endpoint, where the JWKS has additional SPIFFE specific fields (<code>spiffe_sequence</code> and <code>spiffe_refresh_hint</code>, the JWK does not set the <code>alg></code></li>
 * </ul>
 */
public class SpiffeIdentityProvider implements IdentityProvider<SpiffeIdentityProviderConfig>, ClientAssertionIdentityProvider {

    private static final Logger LOGGER = Logger.getLogger(SpiffeIdentityProvider.class);

    private final KeycloakSession session;
    private final SpiffeIdentityProviderConfig config;

    public SpiffeIdentityProvider(KeycloakSession session, SpiffeIdentityProviderConfig config) {
        this.session = session;
        this.config = config;
    }

    @Override
    public SpiffeIdentityProviderConfig getConfig() {
        return config;
    }

    @Override
    public boolean verifyClientAssertion(ClientAssertionContext context) {
        if (!context.getAssertionType().equals(SpiffeConstants.CLIENT_ASSERTION_TYPE)) {
            return false;
        }

        String trustedDomain = config.getTrustDomain();

        if (!verifySignature(context)) {
            return context.failure("Invalid signature");
        }

        JsonWebToken token = context.getToken();

        URI uri = URI.create(token.getSubject());
        if (!uri.getScheme().equals("spiffe")) {
            return context.failure("Not a SPIFFE ID");
        }

        if (!uri.getRawAuthority().equals(trustedDomain)) {
            return context.failure("Invalid trust-domain");
        }

        String expectedAudience = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), session.getContext().getRealm().getName());
        int allowedClockSkew = config.getAllowedClockSkew();

        if (token.getExp() == null || token.getExp() <= 0) {
            return context.failure("Token does not contain an expiration");
        }

        if (!(token.getAudience().length == 1 && token.getAudience()[0].equals(expectedAudience))) {
            return context.failure("Invalid audience");
        }

        if (!token.isActive(allowedClockSkew)) {
            return context.failure("Token not active");
        }
        if (token.getIat() != null && token.getIat() > 0 && token.getIat() - allowedClockSkew > Time.currentTime()) {
            return context.failure("Token was issued in the future");
        }

        return true;
    }

    private boolean verifySignature(ClientAssertionContext context) {

        try {
            String bundleEndpoint = config.getBundleEndpoint();
            JWSInput jws = context.getJwsInput();
            JWSHeader header = jws.getHeader();
            String kid = header.getKeyId();
            String alg = header.getRawAlgorithm();

            String modelKey = PublicKeyStorageUtils.getIdpModelCacheKey(context.getRealm().getId(), config.getInternalId());

            PublicKeyStorageProvider keyStorage = session.getProvider(PublicKeyStorageProvider.class);
            KeyWrapper publicKey = keyStorage.getPublicKey(modelKey, kid, alg, new SpiffeBundleEndpointLoader(session, bundleEndpoint));

            SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, alg);
            if (signatureProvider == null) {
                LOGGER.debugf("Failed to verify token, signature provider not found for algorithm %s", alg);
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

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, BrokeredIdentityContext context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void authenticationFinished(AuthenticationSessionModel authSession, BrokeredIdentityContext context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, BrokeredIdentityContext context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, BrokeredIdentityContext context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Response performLogin(AuthenticationRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void backchannelLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Response keycloakInitiatedBrowserLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Response export(UriInfo uriInfo, RealmModel realm, String format) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdentityProviderDataMarshaller getMarshaller() {
        throw new UnsupportedOperationException();
    }
}
