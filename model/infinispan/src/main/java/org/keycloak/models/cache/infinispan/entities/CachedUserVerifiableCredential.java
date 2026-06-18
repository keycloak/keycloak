package org.keycloak.models.cache.infinispan.entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.UserVerifiableCredentialModel;

public class CachedUserVerifiableCredential {

    private final String credentialScopeName;
    private final String revision;
    private final Long createdDate;
    private final MultivaluedHashMap<String, String> userAttributes;

    public CachedUserVerifiableCredential(UserVerifiableCredentialModel credentialModel) {
        this.credentialScopeName = credentialModel.getCredentialScopeName();
        this.revision = credentialModel.getRevision();
        this.createdDate = credentialModel.getCreatedDate();

        this.userAttributes = new MultivaluedHashMap<>();
        if (credentialModel.getUserAttributes() != null) {
            for (Map.Entry<String, List<String>> entry : credentialModel.getUserAttributes().entrySet()) {
                this.userAttributes.addAll(entry.getKey(), entry.getValue());
            }
        }
    }

    public String getCredentialScopeName() {
        return credentialScopeName;
    }

    public String getRevision() {
        return revision;
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public Map<String, List<String>> getUserAttributes() {
        return new HashMap<>(userAttributes);
    }
}
