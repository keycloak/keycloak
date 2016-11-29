package org.keycloak.testsuite.arquillian;

import org.jboss.arquillian.container.spi.Container;

import java.net.URL;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author tkyjovsk
 */
public class ContainerInfo {

    private URL contextRoot;
    private Container arquillianContainer;
    private boolean adapterLibsInstalled;

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

    public boolean isUndertow() {
        return getQualifier().toLowerCase().contains("undertow");
    }

    public boolean isAS7() {
        return getQualifier().toLowerCase().contains("as7");
    }

    public boolean isWildfly() {
        return getQualifier().toLowerCase().contains("wildfly");
    }

    public boolean isEAP() {
        return getQualifier().toLowerCase().contains("eap");
    }

    public boolean isJBossBased() {
        return isAS7() || isWildfly() || isEAP();
    }

    @Override
    public String toString() {
        return getQualifier();
    }

    public boolean isAdapterLibsInstalled() {
        return adapterLibsInstalled;
    }

    public void setAdapterLibsInstalled(boolean adapterLibsInstalled) {
        this.adapterLibsInstalled = adapterLibsInstalled;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.arquillianContainer);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ContainerInfo other = (ContainerInfo) obj;
        return Objects.equals(
                this.arquillianContainer.getContainerConfiguration().getContainerName(),
                other.arquillianContainer.getContainerConfiguration().getContainerName());
    }

}
