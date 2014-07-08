package org.keycloak.models.users.jpa;

import org.keycloak.model.test.AbstractUserProviderTest;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JpaUserProviderTest extends AbstractUserProviderTest {

    @Override
    protected String getProviderId() {
        return JpaUserProviderFactory.ID;
    }

}
