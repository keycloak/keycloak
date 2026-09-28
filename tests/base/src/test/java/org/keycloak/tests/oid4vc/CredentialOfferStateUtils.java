package org.keycloak.tests.oid4vc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferState;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.util.JsonSerialization;

public final class CredentialOfferStateUtils {

    private CredentialOfferStateUtils() {
    }

    public record CredentialOfferStateRecord(
            String credentialsOfferId,
            String targetUsername,
            String targetClientId,
            String txCode,
            List<OID4VCAuthorizationDetail> authDetails
    ) {}

    public static CredentialOfferStateRecord getCredentialOfferStateRecord(RunOnServerClient runOnServer, String offerNonce) {
        var runtimeOfferState = runOnServer.fetchString(session -> {

            RealmModel realm = session.getContext().getRealm();
            CredentialOfferStorage offerStorage = session.getProvider(CredentialOfferStorage.class);
            CredentialOfferState offerState = Optional.ofNullable(offerStorage.getOfferStateByNonce(offerNonce))
                    .orElseThrow(() -> new IllegalStateException("No CredentialOfferState for nonce: " + offerNonce));

            UserModel userModel = null;
            if (offerState.getTargetUserId() != null) {
                String targetUserId = offerState.getTargetUserId();
                userModel = session.users().getUserById(realm, targetUserId);
                if (userModel == null) {
                    throw new IllegalStateException("No User for id: " + targetUserId);
                }
            }

            Map<String, Object> valueMap = new LinkedHashMap<>();
            valueMap.put("credentialsOfferId", offerState.getCredentialsOfferId());

            Optional.ofNullable(userModel)
                    .ifPresent(it -> valueMap.put("targetUsername", it.getUsername()));

            Optional.ofNullable(offerState.getTargetClientId())
                    .ifPresent(it -> valueMap.put("targetClientId", it));

            Optional.ofNullable(offerState.getAuthorizationDetails())
                    .ifPresent(it -> valueMap.put("authDetails", it));

            Optional.ofNullable(offerState.getTxCode())
                    .ifPresent(it -> valueMap.put("txCode", it));

            return JsonSerialization.valueAsString(valueMap);
        });
        return JsonSerialization.valueFromString(runtimeOfferState, CredentialOfferStateRecord.class);
    }
}
