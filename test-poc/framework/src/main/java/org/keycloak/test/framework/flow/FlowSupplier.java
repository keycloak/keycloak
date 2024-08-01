package org.keycloak.test.framework.flow;

import org.keycloak.test.framework.annotations.InjectFlow;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.page.AbstractPage;
import org.keycloak.test.framework.page.LoginPage;
import org.openqa.selenium.WebDriver;

public class FlowSupplier implements Supplier<ManagedFlow, InjectFlow> {

    @Override
    public Class<InjectFlow> getAnnotationClass() {
        return InjectFlow.class;
    }

    @Override
    public Class<ManagedFlow> getValueType() {
        return ManagedFlow.class;
    }

    @Override
    public ManagedFlow getValue(InstanceContext<ManagedFlow, InjectFlow> instanceContext) {
        WebDriver driver = instanceContext.getDependency(WebDriver.class);
        LoginPage page = (LoginPage) instanceContext.getDependency(AbstractPage.class);
        return new ManagedLoginFlow(driver, page);
    }

    @Override
    public boolean compatible(InstanceContext<ManagedFlow, InjectFlow> a, RequestedInstance<ManagedFlow, InjectFlow> b) {
        return true;
    }

    @Override
    public void close(InstanceContext<ManagedFlow, InjectFlow> instanceContext) {
        instanceContext.getValue().close();
    }
}
