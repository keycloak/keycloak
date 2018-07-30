package org.keycloak.performance.dataset.idm.authorization;

import java.util.stream.Stream;
import org.keycloak.performance.dataset.Dataset;
import org.keycloak.performance.dataset.EntityTest;

/**
 *
 * @author tkyjovsk
 */
public class ResourceTest extends EntityTest<Resource> {

    @Override
    public Stream<Resource> entityStream(Dataset dataset) {
        return dataset.resources();
    }

}
