package org.keycloak.testsuite.console;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.testsuite.page.console.Realm;
import static org.keycloak.testsuite.page.console.Realm.MASTER;
import org.keycloak.testsuite.page.console.RealmsRoot;
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
        
        realmsRoot.navigateTo(); // 1st time possibly redirected from realms root page to master realm
        realmsRoot.navigateTo(); // 2nd time to be sure
        assertCurrentUrl(realmsRoot);

        realmsRoot.clickRealm(MASTER);
        
        realm.setTemplateValues(MASTER);
        assertCurrentUrl(realm);

    }

}
