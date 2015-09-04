package org.keycloak.testsuite.console.realm;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.realm.RealmSettings;
import org.keycloak.testsuite.console.page.realm.RealmSettings.RealmTabs;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractRealmTest extends AbstractConsoleTest {

    @Page
    protected RealmSettings realmSettingsPage;

    public RealmTabs tabs() {
        return realmSettingsPage.tabs();
    }
    
    @Before
    public void beforeRealmTest() {
//        configure().realmSettings();
    }

}
