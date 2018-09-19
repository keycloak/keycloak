package org.keycloak.performance.dataset.idm;

import java.util.stream.Stream;
import org.keycloak.performance.dataset.Dataset;
import org.keycloak.performance.dataset.EntityTest;

/**
 *
 * @author tkyjovsk
 */
public class RealmRoleMappingsTest extends EntityTest<RoleMappings<User>> {

    @Override
    public Stream<RoleMappings<User>> entityStream(Dataset dataset) {
        return dataset.userRealmRoleMappings();
    }
    
}
