package org.keycloak.protocol.ssf.receiver.registration;

import java.util.Set;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;

/**
 * Holds the user configuration of an SSF Receiver.
 */
public class SsfReceiverRegistrationProviderConfig extends IdentityProviderModel {

    public static final String DESCRIPTION = "description";

    public static final String STREAM_ID = "streamId";

    public static final String STREAM_AUDIENCE = "streamAudience";

    public static final String TRANSMITTER_ACCESS_TOKEN = "transmitterAccessToken";

    public static final String PUSH_AUTHORIZATION_HEADER = "pushAuthorizationHeader";

    public SsfReceiverRegistrationProviderConfig() {
    }

    public SsfReceiverRegistrationProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public String getIssuer() {
        return getConfig().get(ISSUER);
    }

    public void setIssuer(String issuer) {
        getConfig().put(ISSUER, issuer);
    }

    public String getDescription() {
        return getConfig().get(DESCRIPTION);
    }

    public void setDescription(String description) {
        getConfig().put(DESCRIPTION, description);
    }

    public String getTransmitterAccessToken() {
        return getConfig().get(TRANSMITTER_ACCESS_TOKEN);
    }

    public void setTransmitterAccessToken(String transmitterAccessToken) {
        getConfig().put(TRANSMITTER_ACCESS_TOKEN, transmitterAccessToken);
    }

    public String getPushAuthorizationHeader() {
        return getConfig().get(PUSH_AUTHORIZATION_HEADER);
    }

    public void setPushAuthorizationHeader(String pushAuthorizationHeader) {
        getConfig().put(PUSH_AUTHORIZATION_HEADER, pushAuthorizationHeader);
    }

    public String getStreamId() {
        return getConfig().get(STREAM_ID);
    }

    public void setStreamId(String streamId) {
        getConfig().put(STREAM_ID, streamId);
    }

    public String getStreamAudience() {
        return getConfig().get(STREAM_AUDIENCE);
    }

    public void setStreamAudience(String streamAudience) {
        getConfig().put(STREAM_AUDIENCE, streamAudience);
    }

    public Set<String> streamAudience() {
        String streamAudience = getStreamAudience();
        if (streamAudience == null) {
            return null;
        }
        return Set.of(streamAudience.split(","));
    }

    @Override
    public void validate(RealmModel realm) {
        super.validate(realm);
    }
}
