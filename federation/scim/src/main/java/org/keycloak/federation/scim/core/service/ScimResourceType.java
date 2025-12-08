package org.keycloak.federation.scim.core.service;

import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.User;

public enum ScimResourceType {

    USER("/Users", User.class),

    GROUP("/Groups", Group.class);

    private final String endpoint;

    private final Class<? extends ResourceNode> resourceClass;

    ScimResourceType(String endpoint, Class<? extends ResourceNode> resourceClass) {
        this.endpoint = endpoint;
        this.resourceClass = resourceClass;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public <T extends ResourceNode> Class<T> getResourceClass() {
        return (Class<T>) resourceClass;
    }
}
