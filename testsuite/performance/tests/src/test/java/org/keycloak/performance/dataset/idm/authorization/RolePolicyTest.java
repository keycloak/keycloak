package org.keycloak.performance.dataset.idm.authorization;

import java.util.stream.Stream;
import org.junit.Test;
import org.keycloak.performance.dataset.Dataset;
import org.keycloak.performance.dataset.EntityTest;

/**
 *
 * @author tkyjovsk
 */
public class RolePolicyTest extends EntityTest<RolePolicy> {

    @Test
    public void testRoles() {
    }
    
    @Override
    public Stream<RolePolicy> entityStream(Dataset dataset) {
        return dataset.rolePolicies();
    }

}
