package org.keycloak.testframework.ui.webdriver;

import java.util.List;

import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;

import org.openqa.selenium.WebDriver;

public class ChromeWebDriverSupplier extends AbstractWebDriverSupplier {

    @Override
    public List<Dependency> getDependencies(RequestedInstance<ManagedWebDriver, InjectWebDriver> instanceContext) {
        return DependenciesBuilder.create(ManagedCertificates.class).build();
    }

    @Override
    public ManagedWebDriver getValue(InstanceContext<ManagedWebDriver, InjectWebDriver> instanceContext) {
        ManagedCertificates managedCerts = instanceContext.getDependency(ManagedCertificates.class);
        return new ManagedWebDriver(DriverUtils.createChromeDriver(false, DriverUtils.chromeTlsArguments(managedCerts)));
    }

    @Override
    public String getAlias() {
        return "chrome";
    }

    @Override
    public WebDriver getWebDriver() {
        return DriverUtils.createChromeDriver(false);
    }
}
