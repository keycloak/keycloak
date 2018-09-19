package org.keycloak.performance.dataset.idm.authorization;

import javax.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.JSPoliciesResource;
import org.keycloak.admin.client.resource.JSPolicyResource;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class JsPolicy extends Policy<JSPolicyRepresentation> {

    public JsPolicy(ResourceServer resourceServer, int index) {
        super(resourceServer, index);
    }

    @Override
    public JSPolicyRepresentation newRepresentation() {
        return new JSPolicyRepresentation();
    }

    public JSPoliciesResource jsPoliciesResource(Keycloak adminClient) {
        return getResourceServer().resource(adminClient).policies().js();
    }

    public JSPolicyResource resource(Keycloak adminClient) {
        return jsPoliciesResource(adminClient).findById(getIdAndReadIfNull(adminClient));
    }

    @Override
    public JSPolicyRepresentation read(Keycloak adminClient) {
        return jsPoliciesResource(adminClient).findByName(getRepresentation().getName());
    }

    @Override
    public Response create(Keycloak adminClient) {
        return jsPoliciesResource(adminClient).create(getRepresentation());
    }

    @Override
    public void update(Keycloak adminClient) {
        resource(adminClient).update(getRepresentation());
    }

    @Override
    public void delete(Keycloak adminClient) {
        resource(adminClient).remove();
    }

}
