package org.keycloak.federation.scim.jpa;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import java.util.Collections;
import java.util.List;

public class ScimResourceProvider implements JpaEntityProvider {

    @Override
    public List<Class<?>> getEntities() {
        return Collections.singletonList(ScimResourceMapping.class);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/scim-resource-changelog.xml";
    }

    @Override
    public void close() {
        // Nothing to close
    }

    @Override
    public String getFactoryId() {
        return ScimResourceProviderFactory.ID;
    }
}
