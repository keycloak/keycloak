package org.keycloak.testsuite.console.page.realm;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;

/**
 * @author tkyjovsk
 * @author mhajas
 */
public class CacheSettings extends RealmSettings {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/cache-settings";
    }

    @Page
    private CacheSettingsForm form;

    public CacheSettingsForm form() {
        return form;
    }

    public class CacheSettingsForm extends Form {
        @FindByJQuery("div[class='onoffswitch']:eq(0)")
        private OnOffSwitch realmCacheEnabled;

        @FindByJQuery("div[class='onoffswitch']:eq(1)")
        private OnOffSwitch userCacheEnabled;

        public void setRealmCacheEnabled(boolean value) {
            realmCacheEnabled.setOn(value);
        }

        public void setUserCacheEnabled(boolean value) {
            userCacheEnabled.setOn(value);
        }
    }

}
