package org.keycloak.protocol.ssf.receiver.verification;

import org.keycloak.models.RealmModel;

public interface SsfStreamVerificationStore {

    void setVerificationState(RealmModel realm, String receiverAlias, String streamId, String state);

    SsfStreamVerificationState getVerificationState(RealmModel realm, String receiverAlias, String streamId);

    void clearVerificationState(RealmModel realm, String receiverAlias, String streamId);
}
