package org.keycloak.performance.dataset.idm;

import java.util.stream.Stream;
import org.keycloak.performance.dataset.Dataset;
import org.keycloak.performance.dataset.EntityTest;

/**
 *
 * @author tkyjovsk
 */
public class RealmTest extends EntityTest<Realm> {

    @Override
    public Stream<Realm> entityStream(Dataset dataset) {
        return dataset.realms();
    }

}
