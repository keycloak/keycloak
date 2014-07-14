package org.keycloak.audit.mongo;

import org.junit.Ignore;
import org.keycloak.audit.tests.AbstractAuditProviderTest;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Ignore
public class MongoAuditProviderTest extends AbstractAuditProviderTest {

    @Override
    protected String getProviderId() {
        return MongoAuditProviderFactory.ID;
    }

}
