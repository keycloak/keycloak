package org.keycloak.performance.templates.idm.authorization;

import org.keycloak.performance.dataset.idm.authorization.JsPolicy;
import org.keycloak.performance.dataset.idm.authorization.ResourceServer;
import org.keycloak.performance.util.ValidateNumber;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class JsPolicyTemplate extends PolicyTemplate<JsPolicy, JSPolicyRepresentation> {

    public static final String JS_POLICIES_PER_RESOURCE_SERVER = "jsPoliciesPerResourceServer";
    
    public final int jsPoliciesPerResourceServer;

    public JsPolicyTemplate(ResourceServerTemplate resourceServerTemplate) {
        super(resourceServerTemplate);
        this.jsPoliciesPerResourceServer = getConfiguration().getInt(JS_POLICIES_PER_RESOURCE_SERVER, 0);
    }

    public int getJsPoliciesPerResourceServer() {
        return jsPoliciesPerResourceServer;
    }

    @Override
    public int getEntityCountPerParent() {
        return jsPoliciesPerResourceServer;
    }

    @Override
    public void validateConfiguration() {
        logger().info(String.format("%s: %s", JS_POLICIES_PER_RESOURCE_SERVER, jsPoliciesPerResourceServer));
        ValidateNumber.minValue(jsPoliciesPerResourceServer, 0);
    }

    @Override
    public JsPolicy newEntity(ResourceServer parentEntity, int index) {
        return new JsPolicy(parentEntity, index);
    }

    @Override
    public void processMappings(JsPolicy policy) {
    }

}
