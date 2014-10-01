package org.keycloak.services.resources.flows;

import org.keycloak.ClientConnection;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.ClientSessionCode;
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

    SocialRedirectFlows(RealmModel realm, UriInfo uriInfo, ClientConnection clientConnection, SocialProvider provider) {
        this.realm = realm;
        this.uriInfo = uriInfo;
        this.clientConnection = clientConnection;
        this.socialProvider = provider;
    }

    public Response redirectToSocialProvider(ClientSessionCode code) throws SocialProviderException {
        code.setAction(ClientSessionModel.Action.SOCIAL_CALLBACK);
        code.getClientSession().setNote("social_provider", socialProvider.getId());
        String socialProviderId = socialProvider.getId();

        String key = realm.getSocialConfig().get(socialProviderId + ".key");
        String secret = realm.getSocialConfig().get(socialProviderId + ".secret");
        String callbackUri = Urls.socialCallback(uriInfo.getBaseUri()).toString();
        SocialProviderConfig config = new SocialProviderConfig(key, secret, callbackUri);


        AuthRequest authRequest = socialProvider.getAuthUrl(code.getClientSession(), config, code.getCode());
        return Response.status(302).location(authRequest.getAuthUri()).build();
    }

}
