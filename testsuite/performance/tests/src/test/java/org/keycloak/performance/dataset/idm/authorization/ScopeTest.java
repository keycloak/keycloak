package org.keycloak.performance.dataset.idm.authorization;

import java.util.stream.Stream;
import org.keycloak.performance.dataset.Dataset;
import org.keycloak.performance.dataset.EntityTest;

/**
 *
 * @author tkyjovsk
 */
public class ScopeTest extends EntityTest<Scope> {

    @Override
    public Stream<Scope> entityStream(Dataset dataset) {
        return dataset.scopes();
    }

}
