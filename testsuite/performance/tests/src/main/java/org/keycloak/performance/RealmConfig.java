package org.keycloak.performance;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class RealmConfig {
    public int accessTokenLifeSpan = 60;
    public boolean registrationAllowed = true;
    public List<String> requiredCredentials = Collections.unmodifiableList(Arrays.asList("password"));
}
