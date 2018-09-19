package org.keycloak.performance.dataset.idm;

import java.util.stream.Stream;
import org.keycloak.performance.dataset.Dataset;
import org.keycloak.performance.dataset.EntityTest;

/**
 *
 * @author tkyjovsk
 */
public class ClientTest extends EntityTest<Client>{

    @Override
    public Stream<Client> entityStream(Dataset dataset) {
        return dataset.clients();
    }
    
}
