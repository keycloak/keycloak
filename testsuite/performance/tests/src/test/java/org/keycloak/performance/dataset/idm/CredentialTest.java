package org.keycloak.performance.dataset.idm;

import java.util.stream.Stream;
import org.keycloak.performance.dataset.Dataset;
import org.keycloak.performance.dataset.EntityTest;

/**
 *
 * @author tkyjovsk
 */
public class CredentialTest extends EntityTest<Credential> {

    @Override
    public Stream<Credential> entityStream(Dataset dataset) {
        return dataset.credentials();
    }

}
