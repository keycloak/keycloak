package org.keycloak.performance.dataset.idm;

import java.util.stream.Stream;
import org.keycloak.performance.dataset.Dataset;
import org.keycloak.performance.dataset.EntityTest;

/**
 *
 * @author tkyjovsk
 */
public class RealmRoleTest extends EntityTest<RealmRole> {

    @Override
    public Stream<RealmRole> entityStream(Dataset dataset) {
        return dataset.realmRoles();
    }

}
