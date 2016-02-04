package org.keycloak.testsuite.arquillian;

import java.net.URL;
import java.util.Map;
import org.jboss.arquillian.container.spi.Container;

/**
 *
 * @author tkyjovsk
 */
public class ContainerInfo {

    private URL contextRoot;
    private Container arquillianContainer;

    public ContainerInfo(Container arquillianContainer) {
        if (arquillianContainer == null) {
            throw new IllegalArgumentException();
        }
        this.arquillianContainer = arquillianContainer;
    }

    public Container getArquillianContainer() {
        return arquillianContainer;
    }

    public Map<String, String> getProperties() {
        return getArquillianContainer().getContainerConfiguration().getContainerProperties();
    }

    public String getQualifier() {
        return getArquillianContainer().getName();
    }

    public URL getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(URL contextRoot) {
        this.contextRoot = contextRoot;
    }

    public boolean isAS7() {
        return getQualifier().contains("as7");
    }

    public boolean isWildfly() {
        return getQualifier().contains("Wildfly");
    }

    public boolean isEAP() {
        return getQualifier().contains("eap");
    }

    public boolean isJBossBased() {
        return isAS7() || isWildfly() || isEAP();
    }

    @Override
    public String toString() {
        return getQualifier();
    }

}
