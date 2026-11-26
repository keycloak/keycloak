package org.keycloak.models.cache.infinispan.entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.UserVerifiableCredentialModel;

public class CachedUserVerifiableCredential {

    private final String id;
    private final String clientScopeId;
    private final String revision;
    private final Long createdDate;
    private final Long updatedDate;
    private final MultivaluedHashMap<String, String> userAttributes;

    public CachedUserVerifiableCredential(UserVerifiableCredentialModel credentialModel) {
        this.id = credentialModel.getId();
        this.clientScopeId = credentialModel.getClientScopeId();
        this.revision = credentialModel.getRevision();
        this.createdDate = credentialModel.getCreatedDate();
        this.updatedDate = credentialModel.getUpdatedDate();

        this.userAttributes = new MultivaluedHashMap<>();
        if (credentialModel.getUserAttributes() != null) {
            for (Map.Entry<String, List<String>> entry : credentialModel.getUserAttributes().entrySet()) {
                this.userAttributes.addAll(entry.getKey(), entry.getValue());
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getClientScopeId() {
        return clientScopeId;
    }

    public String getRevision() {
        return revision;
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public Long getUpdatedDate() {
        return updatedDate;
    }

    public Map<String, List<String>> getUserAttributes() {
        return new HashMap<>(userAttributes);
    }
}
