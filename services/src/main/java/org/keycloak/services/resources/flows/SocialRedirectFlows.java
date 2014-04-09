package org.keycloak.services.resources.flows;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.SocialRequestManager;
import org.keycloak.social.AuthRequest;
import org.keycloak.social.RequestDetails;
import org.keycloak.social.SocialProvider;
import org.keycloak.social.SocialProviderConfig;
import org.keycloak.social.SocialProviderException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SocialRedirectFlows {

    private final SocialRequestManager socialRequestManager;
    private final RealmModel realm;
    private final UriInfo uriInfo;
    private final SocialProvider socialProvider;
    private final RequestDetails.RequestDetailsBuilder socialRequestBuilder;

    SocialRedirectFlows(SocialRequestManager socialRequestManager, RealmModel realm, UriInfo uriInfo, SocialProvider provider) {
        this.socialRequestManager = socialRequestManager;
        this.realm = realm;
        this.uriInfo = uriInfo;
        this.socialRequestBuilder = RequestDetails.create(provider.getId());
        this.socialProvider = provider;
    }

    public SocialRedirectFlows putClientAttribute(String name, String value) {
        socialRequestBuilder.putClientAttribute(name, value);
        return this;
    }

    public Response redirectToSocialProvider() throws SocialProviderException {
        String socialProviderId = socialProvider.getId();

        String key = realm.getSocialConfig().get(socialProviderId + ".key");
        String secret = realm.getSocialConfig().get(socialProviderId + ".secret");
        String callbackUri = Urls.socialCallback(uriInfo.getBaseUri()).toString();
        SocialProviderConfig config = new SocialProviderConfig(key, secret, callbackUri);

        AuthRequest authRequest = socialProvider.getAuthUrl(config);
        RequestDetails socialRequest = socialRequestBuilder.putSocialAttributes(authRequest.getAttributes()).build();
        socialRequestManager.addRequest(authRequest.getId(), socialRequest);
        return Response.status(302).location(authRequest.getAuthUri()).build();
    }
}
