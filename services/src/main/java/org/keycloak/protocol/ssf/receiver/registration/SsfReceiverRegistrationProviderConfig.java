package org.keycloak.protocol.ssf.receiver.registration;

import java.util.Set;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;

/**
 * Holds the user configuration of an SSF Receiver.
 */
public class SsfReceiverRegistrationProviderConfig extends IdentityProviderModel {

    public static final String DESCRIPTION = "description";

    public static final String TRANSMITTER_METADATA_URL = "transmitterMetadataUrl";

    public static final String STREAM_ID = "streamId";

    public static final String STREAM_AUDIENCE = "streamAudience";

    public static final String TRANSMITTER_TOKEN = "transmitterToken";

    public static final String TRANSMITTER_TOKEN_TYPE = "transmitterTokenType";

    public static final String DELIVERY_METHOD = "deliveryMethod";

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

    public String getTransmitterToken() {
        return getConfig().get(TRANSMITTER_TOKEN);
    }

    public void setTransmitterToken(String transmitterToken) {
        getConfig().put(TRANSMITTER_TOKEN, transmitterToken);
    }

    public TransmitterTokenType getTransmitterTokenType() {
        String value = getConfig().get(TRANSMITTER_TOKEN_TYPE);
        if (value == null) {
            return TransmitterTokenType.ACCESS_TOKEN;
        }
        return TransmitterTokenType.valueOf(value);
    }

    public void setTransmitterTokenType(TransmitterTokenType transmitterTokenType) {
        getConfig().put(TRANSMITTER_TOKEN_TYPE, transmitterTokenType.name());
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

    public String getTransmitterMetadataUrl() {
        return getConfig().get(TRANSMITTER_METADATA_URL);
    }

    public void setTransmitterMetadataUrl(String transmitterMetadataUrl) {
        getConfig().put(TRANSMITTER_METADATA_URL, transmitterMetadataUrl);
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

    public DeliveryMethod getDeliveryMethod() {
        String value = getConfig().get(DELIVERY_METHOD);
        if (value == null) {
            return DeliveryMethod.PUSH;
        }
        return DeliveryMethod.valueOf(value);
    }

    public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
        getConfig().put(DELIVERY_METHOD, deliveryMethod.name());
    }

    @Override
    public void validate(RealmModel realm) {
        super.validate(realm);
    }

    public static enum TransmitterTokenType {
        ACCESS_TOKEN,
        // TODO add support for refresh token
        // REFRESH_TOKEN
    }

    public static enum DeliveryMethod {
        PUSH,
        // we might support polling in the future
        // POLL,
    }
}
