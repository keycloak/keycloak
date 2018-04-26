package org.keycloak.performance.dataset.idm;

import java.util.stream.Stream;
import org.keycloak.performance.dataset.Dataset;
import org.keycloak.performance.dataset.EntityTest;

/**
 *
 * @author tkyjovsk
 */
public class UserTest extends EntityTest<User> {

    @Override
    public Stream<User> entityStream(Dataset dataset) {
        return dataset.users();
    }

}
