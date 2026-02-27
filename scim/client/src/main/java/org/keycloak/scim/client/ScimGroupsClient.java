package org.keycloak.scim.client;

import org.keycloak.scim.resource.group.Group;

public class ScimGroupsClient extends AbstractScimResourceClient<Group> {

    public ScimGroupsClient(ScimClient client) {
        super(client, Group.class);
    }

    @Override
    public void close() {

    }
}
