package org.keycloak.testsuite.arquillian;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.Container.State;
import org.keycloak.common.util.KeycloakUriBuilder;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author tkyjovsk
 */
public class ContainerInfo implements Comparable<ContainerInfo> {

    private URL contextRoot;
    private URL browserContextRoot;
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

    public KeycloakUriBuilder getUriBuilder() {
        try {
            return KeycloakUriBuilder.fromUri(getContextRoot().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void setContextRoot(URL contextRoot) {
        this.contextRoot = contextRoot;
    }

    public void setBrowserContextRoot(URL browserContextRoot) {
        this.browserContextRoot = browserContextRoot;
    }

    public URL getBrowserContextRoot() {
        return browserContextRoot;
    }

    public boolean isUndertow() {
        return getQualifier().toLowerCase().contains("undertow");
    }

    public boolean isQuarkus() {
        return getQualifier().toLowerCase().contains("quarkus");
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
        return isAS7() || isWildfly() || isEAP() || getQualifier().toLowerCase().contains("jboss");
    }

    @Override
    public String toString() {
        return getQualifier();
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

    public boolean isStarted() {
        return arquillianContainer.getState() == State.STARTED;
    }

    public boolean isManual() {
        return Objects.equals(arquillianContainer.getContainerConfiguration().getMode(), "manual");
    }

    @Override
    public int compareTo(ContainerInfo o) {
        return this.getQualifier().compareTo(o.getQualifier());
    }

}
