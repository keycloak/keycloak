package org.keycloak.protocol.ssf.receiver.verification;

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.protocol.ssf.receiver.ReceiverModel;

import java.util.Map;

public class DefaultVerificationStore implements VerificationStore {

    private final KeycloakSession session;

    public DefaultVerificationStore(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setVerificationState(RealmModel realm, ReceiverModel model, String state) {
        // TODO check for pending verifications

        var singleUseObject = session.getProvider(SingleUseObjectProvider.class);

        String key = createVerificationKey(model.getStreamId());
        int lifespanSeconds = 300;
        Map<String, String> verificationData = Map.of("state", state, "timestamp", String.valueOf(Time.currentTime()));
        singleUseObject.put(key, lifespanSeconds, verificationData);
    }

    protected String createVerificationKey(String streamId) {
        return "ssf.verification." + streamId;
    }

    @Override
    public VerificationState getVerificationState(RealmModel realm, ReceiverModel model) {

        var singleUseObject = session.getProvider(SingleUseObjectProvider.class);
        String key = createVerificationKey(model.getStreamId());
        Map<String, String> verificationData = singleUseObject.get(key);

        if (verificationData == null) {
            return null;
        }

        String state = verificationData.get("state");
        long timestamp = Long.parseLong(verificationData.get("timestamp"));

        VerificationState verificationState = new VerificationState();
        verificationState.setTimestamp(timestamp);
        verificationState.setState(state);
        verificationState.setStreamId(model.getStreamId());

        return verificationState;
    }

    @Override
    public void clearVerificationState(RealmModel realm, ReceiverModel model) {
        var singleUseObject = session.getProvider(SingleUseObjectProvider.class);
        String key = createVerificationKey(model.getStreamId());
        singleUseObject.remove(key);
    }

}
