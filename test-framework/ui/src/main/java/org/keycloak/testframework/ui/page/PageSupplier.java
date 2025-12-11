package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

public class PageSupplier  implements Supplier<AbstractPage, InjectPage> {

    @Override
    public AbstractPage getValue(InstanceContext<AbstractPage, InjectPage> instanceContext) {
        ManagedWebDriver webDriver = instanceContext.getDependency(ManagedWebDriver.class);
        return webDriver.page().createPage(instanceContext.getRequestedValueType());
    }

    @Override
    public boolean compatible(InstanceContext<AbstractPage, InjectPage> a, RequestedInstance<AbstractPage, InjectPage> b) {
        return true;
    }

}
