package org.keycloak.performance.templates.idm.authorization;

import org.keycloak.performance.dataset.idm.authorization.ResourceServer;
import org.keycloak.performance.dataset.idm.authorization.Scope;
import org.keycloak.performance.templates.NestedEntityTemplate;
import org.keycloak.performance.util.ValidateNumber;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class ScopeTemplate extends NestedEntityTemplate<ResourceServer, Scope, ScopeRepresentation> {

    public static final String SCOPES_PER_RESOURCE_SERVER = "scopesPerResourceServer";
    
    public final int scopesPerResourceServer;

    public ScopeTemplate(ResourceServerTemplate resourceServerTemplate) {
        super(resourceServerTemplate);
        this.scopesPerResourceServer = getConfiguration().getInt(SCOPES_PER_RESOURCE_SERVER, 0);
    }

    @Override
    public int getEntityCountPerParent() {
        return scopesPerResourceServer;
    }

    @Override
    public void validateConfiguration() {
        logger().info(String.format("%s: %s", SCOPES_PER_RESOURCE_SERVER, scopesPerResourceServer));
        ValidateNumber.minValue(scopesPerResourceServer, 0);
    }

    @Override
    public Scope newEntity(ResourceServer parentEntity, int index) {
        return new Scope(parentEntity, index);
    }

    @Override
    public void processMappings(Scope entity) {
    }

}
