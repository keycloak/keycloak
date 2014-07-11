package org.keycloak.audit.jpa;

import org.junit.Ignore;
import org.keycloak.audit.tests.AbstractAuditProviderTest;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Ignore
public class JpaAuditProviderTest extends AbstractAuditProviderTest {

    @Override
    protected String getProviderId() {
        return JpaAuditProviderFactory.ID;
    }

}
