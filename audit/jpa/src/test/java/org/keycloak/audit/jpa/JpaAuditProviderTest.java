package org.keycloak.audit.jpa;

import org.keycloak.audit.tests.AbstractAuditProviderTest;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JpaAuditProviderTest extends AbstractAuditProviderTest {

    @Override
    protected String getProviderId() {
        return JpaAuditProviderFactory.ID;
    }

}
