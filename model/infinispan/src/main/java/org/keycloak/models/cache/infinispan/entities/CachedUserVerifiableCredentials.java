
package org.keycloak.models.cache.infinispan.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserVerifiableCredentialModel;

public class CachedUserVerifiableCredentials extends AbstractRevisioned implements InRealm {
    private final List<CachedUserVerifiableCredential> credentials;
    private final String realmId;

    public CachedUserVerifiableCredentials(long revision, String id, RealmModel realm, List<UserVerifiableCredentialModel> credentials) {
        super(revision, id);
        this.realmId = realm.getId();
        this.credentials = credentials != null
            ? credentials.stream()
                .map(CachedUserVerifiableCredential::new)
                .collect(Collectors.toCollection(ArrayList::new))
            : new ArrayList<>();
    }

    public List<CachedUserVerifiableCredential> getCredentials() {
        return credentials;
    }

    @Override
    public String getRealm() {
        return realmId;
    }
}
