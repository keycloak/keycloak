package org.keycloak.testsuite.console;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.testsuite.console.page.Realm;
import static org.keycloak.testsuite.console.page.Realm.MASTER;
import org.keycloak.testsuite.console.page.RealmsRoot;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrl;

/**
 *
 * @author tkyjovsk
 */
public class RealmsTest extends AbstractAdminConsoleTest<RealmsRoot> {

    @Page
    private RealmsRoot realmsRoot;

    @Page
    private Realm realm;

    @Test
    public void testSelectMasterRealm() {

        realmsRoot.navigateTo();
        assertCurrentUrl(realmsRoot);

        realmsRoot.clickRealm(MASTER);

        realm.setAdminRealm(MASTER);
        assertCurrentUrl(realm);

    }

}
