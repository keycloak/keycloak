package org.keycloak.protocol.ssf.receiver.verification;

import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.receiver.ReceiverModel;

public interface VerificationStore {

    void setVerificationState(RealmModel realm, ReceiverModel model, String state);

    VerificationState getVerificationState(RealmModel realm, ReceiverModel model);

    void clearVerificationState(RealmModel realm, ReceiverModel model);
}
