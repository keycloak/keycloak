package org.keycloak.performance.dataset.idm.authorization;

import java.util.stream.Stream;
import org.keycloak.performance.dataset.Dataset;
import org.keycloak.performance.dataset.EntityTest;

/**
 *
 * @author tkyjovsk
 */
public class ResourceServerTest extends EntityTest<ResourceServer> {

    @Override
    public Stream<ResourceServer> entityStream(Dataset dataset) {
        return dataset.resourceServers();
    }

}
