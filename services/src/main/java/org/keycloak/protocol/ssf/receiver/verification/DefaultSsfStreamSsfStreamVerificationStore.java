package org.keycloak.protocol.ssf.receiver.verification;

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;

import java.util.Map;

/**
 * Default {@link SsfStreamVerificationStore} implementation that uses the {@link SingleUseObjectProvider} to manage the
 * verification state of a stream associated with a SSF Receiver.
 */
public class DefaultSsfStreamSsfStreamVerificationStore implements SsfStreamVerificationStore {

    protected int verificationStateLifespanSeconds = 300;

    protected final KeycloakSession session;

    public DefaultSsfStreamSsfStreamVerificationStore(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setVerificationState(RealmModel realm, String receiverAlias, String streamId, String state) {
        // TODO check for pending verifications

        var singleUseObject = session.getProvider(SingleUseObjectProvider.class);

        String key = createVerificationKey(receiverAlias, streamId);
        Map<String, String> verificationData = Map.of("state", state, "timestamp", String.valueOf(Time.currentTime()));
        singleUseObject.put(key, verificationStateLifespanSeconds, verificationData);
    }

    protected String createVerificationKey(String receiverAlias, String streamId) {
        return "ssf.verification:" + receiverAlias + ":" + streamId;
    }

    @Override
    public SsfStreamVerificationState getVerificationState(RealmModel realm, String receiverAlias, String streamId) {

        var singleUseObject = session.getProvider(SingleUseObjectProvider.class);
        String key = createVerificationKey(receiverAlias, streamId);
        Map<String, String> verificationData = singleUseObject.get(key);

        if (verificationData == null) {
            return null;
        }

        String state = verificationData.get("state");
        long timestamp = Long.parseLong(verificationData.get("timestamp"));

        SsfStreamVerificationState verificationState = new SsfStreamVerificationState();
        verificationState.setTimestamp(timestamp);
        verificationState.setState(state);
        verificationState.setStreamId(streamId);

        return verificationState;
    }

    @Override
    public void clearVerificationState(RealmModel realm, String receiverAlias, String streamId) {
        var singleUseObject = session.getProvider(SingleUseObjectProvider.class);
        String key = createVerificationKey(receiverAlias, streamId);
        singleUseObject.remove(key);
    }

}
