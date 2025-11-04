package org.keycloak.protocol.ssf.receiver.verification;

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.protocol.ssf.receiver.SsfReceiverModel;

import java.util.Map;

public class DefaultSsfStreamSsfStreamVerificationStore implements SsfStreamVerificationStore {

    protected int verificationStateLifespanSeconds = 300;

    protected final KeycloakSession session;

    public DefaultSsfStreamSsfStreamVerificationStore(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setVerificationState(RealmModel realm, SsfReceiverModel model, String state) {
        // TODO check for pending verifications

        var singleUseObject = session.getProvider(SingleUseObjectProvider.class);

        String key = createVerificationKey(model.getStreamId());
        Map<String, String> verificationData = Map.of("state", state, "timestamp", String.valueOf(Time.currentTime()));
        singleUseObject.put(key, verificationStateLifespanSeconds, verificationData);
    }

    protected String createVerificationKey(String streamId) {
        return "ssf.verification." + streamId;
    }

    @Override
    public SsfStreamVerificationState getVerificationState(RealmModel realm, SsfReceiverModel model) {

        var singleUseObject = session.getProvider(SingleUseObjectProvider.class);
        String key = createVerificationKey(model.getStreamId());
        Map<String, String> verificationData = singleUseObject.get(key);

        if (verificationData == null) {
            return null;
        }

        String state = verificationData.get("state");
        long timestamp = Long.parseLong(verificationData.get("timestamp"));

        SsfStreamVerificationState verificationState = new SsfStreamVerificationState();
        verificationState.setTimestamp(timestamp);
        verificationState.setState(state);
        verificationState.setStreamId(model.getStreamId());

        return verificationState;
    }

    @Override
    public void clearVerificationState(RealmModel realm, SsfReceiverModel model) {
        var singleUseObject = session.getProvider(SingleUseObjectProvider.class);
        String key = createVerificationKey(model.getStreamId());
        singleUseObject.remove(key);
    }

}
