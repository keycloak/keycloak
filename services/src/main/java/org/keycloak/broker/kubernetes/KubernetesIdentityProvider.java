package org.keycloak.broker.kubernetes;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityProviderDataMarshaller;
import org.keycloak.crypto.KeyWrapper;
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
import org.keycloak.sessions.AuthenticationSessionModel;

public class KubernetesIdentityProvider extends OIDCIdentityProvider {

    private final String globalJwksUrl;

    public KubernetesIdentityProvider(KeycloakSession session, KubernetesIdentityProviderConfig config, String globalJwksUrl) {
        super(session, config);
        this.globalJwksUrl = globalJwksUrl;
    }

    protected KeyWrapper getIdentityProviderKeyWrapper(JWSInput jws) {
        JWSHeader header = jws.getHeader();
        String kid = header.getKeyId();
        String alg = header.getRawAlgorithm();

        String modelKey = PublicKeyStorageUtils.getIdpModelCacheKey(session.getContext().getRealm().getId(), getConfig().getInternalId());

        PublicKeyStorageProvider keyStorage = session.getProvider(PublicKeyStorageProvider.class);
        return keyStorage.getPublicKey(modelKey, kid, alg, new KubernetesJwksEndpointLoader(session, globalJwksUrl, getConfig().getJwksUrl()));
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
