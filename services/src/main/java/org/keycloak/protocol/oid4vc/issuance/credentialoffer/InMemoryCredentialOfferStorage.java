package org.keycloak.protocol.oid4vc.issuance.credentialoffer;

import java.util.Map;
import java.util.Optional;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailsResponse;
import org.keycloak.util.JsonSerialization;

class InMemoryCredentialOfferStorage implements CredentialOfferStorage {

    private static final String ENTRY_KEY = "json";

    @Override
    public synchronized void putOfferState(KeycloakSession session, CredentialOfferState entry) {
        String entryJson = JsonSerialization.valueAsString(entry);
        session.singleUseObjects().put(entry.getNonce(), entry.getExpiration(), Map.of(ENTRY_KEY, entryJson));
        entry.getPreAuthorizedCode().ifPresent(it -> {
            session.singleUseObjects().put(it, entry.getExpiration(), Map.of(ENTRY_KEY, entryJson));
        });
        Optional.ofNullable(entry.getAuthorizationDetails()).ifPresent(it -> {
            ((OID4VCAuthorizationDetailsResponse) it).getCredentialIdentifiers().forEach( cid -> {
                session.singleUseObjects().put(cid, entry.getExpiration(), Map.of(ENTRY_KEY, entryJson));
            });
        });
    }

    @Override
    public synchronized CredentialOfferState findOfferStateByNonce(KeycloakSession session, String nonce) {
        if (session.singleUseObjects().contains(nonce)) {
            String entryJson = session.singleUseObjects().get(nonce).get(ENTRY_KEY);
            return JsonSerialization.valueFromString(entryJson, CredentialOfferState.class);
        }
        return null;
    }

    @Override
    public synchronized CredentialOfferState findOfferStateByCode(KeycloakSession session, String code) {
        if (session.singleUseObjects().contains(code)) {
            String entryJson = session.singleUseObjects().get(code).get(ENTRY_KEY);
            return JsonSerialization.valueFromString(entryJson, CredentialOfferState.class);
        }
        return null;
    }

    @Override
    public CredentialOfferState findOfferStateByCredentialId(KeycloakSession session, String credId) {
        if (session.singleUseObjects().contains(credId)) {
            String entryJson = session.singleUseObjects().get(credId).get(ENTRY_KEY);
            return JsonSerialization.valueFromString(entryJson, CredentialOfferState.class);
        }
        return null;
    }

    public synchronized void replaceOfferState(KeycloakSession session, CredentialOfferState entry) {
        String entryJson = JsonSerialization.valueAsString(entry);
        session.singleUseObjects().replace(entry.getNonce(), Map.of(ENTRY_KEY, entryJson));
        entry.getPreAuthorizedCode().ifPresent(it -> {
            session.singleUseObjects().replace(it, Map.of(ENTRY_KEY, entryJson));
        });
        Optional.ofNullable(entry.getAuthorizationDetails()).ifPresent(it -> {
            ((OID4VCAuthorizationDetailsResponse) it).getCredentialIdentifiers().forEach( cid -> {
                if (session.singleUseObjects().contains(cid)) {
                    session.singleUseObjects().replace(cid, Map.of(ENTRY_KEY, entryJson));
                } else {
                    session.singleUseObjects().put(cid, entry.getExpiration(), Map.of(ENTRY_KEY, entryJson));
                }
            });
        });
    }

    @Override
    public synchronized void removeOfferState(KeycloakSession session, CredentialOfferState entry) {
        session.singleUseObjects().remove(entry.getNonce());
        entry.getPreAuthorizedCode().ifPresent(it -> {
            session.singleUseObjects().remove(it);
        });
        Optional.ofNullable(entry.getAuthorizationDetails()).ifPresent(it -> {
            ((OID4VCAuthorizationDetailsResponse) it).getCredentialIdentifiers().forEach( cid -> {
                session.singleUseObjects().remove(cid);
            });
        });
    }
}
