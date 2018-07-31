package org.keycloak.performance.dataset.idm;

import java.util.stream.Stream;
import org.keycloak.performance.dataset.Dataset;
import org.keycloak.performance.dataset.EntityTest;

/**
 *
 * @author tkyjovsk
 */
public class ClientRoleTest extends EntityTest<ClientRole> {

    @Override
    public Stream<ClientRole> entityStream(Dataset dataset) {
        return dataset.clientRoles();
    }

}
