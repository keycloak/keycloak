package org.keycloak.protocol.ssf.receiver.verification;

import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.receiver.SsfReceiverModel;

public interface SsfStreamVerificationStore {

    void setVerificationState(RealmModel realm, SsfReceiverModel model, String state);

    SsfStreamVerificationState getVerificationState(RealmModel realm, SsfReceiverModel model);

    void clearVerificationState(RealmModel realm, SsfReceiverModel model);
}
