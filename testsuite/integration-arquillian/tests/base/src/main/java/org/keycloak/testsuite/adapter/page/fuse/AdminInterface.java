package org.keycloak.testsuite.adapter.page.fuse;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class AdminInterface extends CustomerPortalFuseExample {

    @Override
    public String getContext() {
        return super.getContext() + "/customers/camel.jsp";
    }

}
