package org.keycloak.testsuite.adapter;

import java.io.File;
import java.util.List;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.TestRealms.loadRealm;
import static org.keycloak.testsuite.adapter.AbstractExampleAdapterTest.EXAMPLES_HOME_DIR;
import static org.keycloak.testsuite.page.console.Realm.DEMO;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractFuseExampleAdapterTest extends AbstractExampleAdapterTest {

    @Override
    public void loadAdapterTestRealmsTo(List<RealmRepresentation> testRealms) {
        RealmRepresentation fureRealm = loadRealm(new File(EXAMPLES_HOME_DIR + "/fuse/testrealm.json"));
        testRealms.add(fureRealm);
    }

    @Override
    public void setPageUriTemplateValues() {
        super.setPageUriTemplateValues();
        testRealm.setTemplateValues(DEMO);
    }
    
    @Test
    public void testAppServerAvailable() {
        appServerContextRoot.navigateTo();
        assertCurrentUrlStartsWith(appServerContextRoot);
    }

}
