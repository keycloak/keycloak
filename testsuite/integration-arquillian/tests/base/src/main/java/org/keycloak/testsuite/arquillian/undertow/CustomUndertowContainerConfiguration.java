package org.keycloak.testsuite.arquillian.undertow;

import org.arquillian.undertow.UndertowContainerConfiguration;
import org.jboss.arquillian.container.spi.ConfigurationException;

public class CustomUndertowContainerConfiguration extends UndertowContainerConfiguration {

    private int workerThreads = Math.max(Runtime.getRuntime().availableProcessors(), 2) * 8;
    private String resourcesHome;

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public String getResourcesHome() {
        return resourcesHome;
    }

    public void setResourcesHome(String resourcesHome) {
        this.resourcesHome = resourcesHome;
    }

    @Override
    public void validate() throws ConfigurationException {
        super.validate();
        
        // TODO validate workerThreads
        
    }
    
}
