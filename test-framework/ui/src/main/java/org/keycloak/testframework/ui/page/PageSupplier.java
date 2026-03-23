package org.keycloak.testframework.ui.page;

import java.util.List;

import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

public class PageSupplier  implements Supplier<AbstractPage, InjectPage> {

    @Override
    public List<Dependency> getDependencies(RequestedInstance<AbstractPage, InjectPage> instanceContext) {
        return DependenciesBuilder.create(ManagedWebDriver.class, instanceContext.getAnnotation().webDriverRef()).build();
    }

    @Override
    public AbstractPage getValue(InstanceContext<AbstractPage, InjectPage> instanceContext) {
        ManagedWebDriver webDriver = instanceContext.getDependency(ManagedWebDriver.class, instanceContext.getAnnotation().webDriverRef());
        return webDriver.page().createPage(instanceContext.getRequestedValueType());
    }

    @Override
    public boolean compatible(InstanceContext<AbstractPage, InjectPage> a, RequestedInstance<AbstractPage, InjectPage> b) {
        return a.getAnnotation().ref().equals(b.getAnnotation().ref());
    }

}
