package org.keycloak.performance.templates.idm.authorization;

import org.keycloak.performance.dataset.idm.authorization.Policy;
import org.keycloak.performance.dataset.idm.authorization.ResourceServer;
import org.keycloak.performance.templates.NestedEntityTemplate;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;

/**
 *
 * @author tkyjovsk
 */
public abstract class PolicyTemplate<NIE extends Policy<R>, R extends AbstractPolicyRepresentation>
        extends NestedEntityTemplate<ResourceServer, NIE, R> {

    public PolicyTemplate(ResourceServerTemplate resourceServerTemplate) {
        super(resourceServerTemplate);
    }
    
    public ResourceServerTemplate resourceServerTemplate() {
        return (ResourceServerTemplate) getParentEntityTemplate();
    }

}
