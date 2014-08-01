package org.keycloak.services.resources.flows;

import org.keycloak.ClientConnection;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.SocialResource;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.social.AuthRequest;
import org.keycloak.social.SocialProvider;
import org.keycloak.social.SocialProviderConfig;
import org.keycloak.social.SocialProviderException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SocialRedirectFlows {

    private final RealmModel realm;
    private final UriInfo uriInfo;
    private ClientConnection clientConnection;
    private final SocialProvider socialProvider;
    private final SocialResource.State state;

    SocialRedirectFlows(RealmModel realm, UriInfo uriInfo, ClientConnection clientConnection, SocialProvider provider) {
        this.realm = realm;
        this.uriInfo = uriInfo;
        this.clientConnection = clientConnection;
        this.socialProvider = provider;

        state = new SocialResource.State();
        state.setRealm(realm.getName());
        state.setProvider(provider.getId());
    }

    public SocialRedirectFlows putClientAttribute(String key, String value) {
        state.set(key, value);
        return this;
    }

    public SocialRedirectFlows user(UserModel user) {
        state.setUser(user.getId());
        return this;
    }

    public Response redirectToSocialProvider() throws SocialProviderException {
        String socialProviderId = socialProvider.getId();

        String key = realm.getSocialConfig().get(socialProviderId + ".key");
        String secret = realm.getSocialConfig().get(socialProviderId + ".secret");
        String callbackUri = Urls.socialCallback(uriInfo.getBaseUri()).toString();
        SocialProviderConfig config = new SocialProviderConfig(key, secret, callbackUri);

        String encodedState = new JWSBuilder().jsonContent(state).rsa256(realm.getPrivateKey());

        AuthRequest authRequest = socialProvider.getAuthUrl(config, encodedState);

        if (authRequest.getAttributes() != null) {
            String cookiePath = Urls.socialBase(uriInfo.getBaseUri()).build().getRawPath().toString();

            String encoded = new JWSBuilder()
                    .jsonContent(authRequest.getAttributes())
                    .rsa256(realm.getPrivateKey());

            CookieHelper.addCookie("KEYCLOAK_SOCIAL", encoded, cookiePath, null, null, -1, realm.getSslRequired().isRequired(clientConnection), true);
        }

        return Response.status(302).location(authRequest.getAuthUri()).build();
    }

}
