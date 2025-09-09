package org.keycloak.models.policy;

final class AdhocResourcePolicyEvent extends ResourcePolicyEvent {

    AdhocResourcePolicyEvent(ResourceType type, String resourceId) {
        super(type, ResourceOperationType.AD_HOC, resourceId, null);
    }
}
