package org.keycloak.tests.cluster;

import java.util.Objects;

import jakarta.ws.rs.core.UriBuilder;

/**
 * Lightweight node descriptor used by migrated cluster tests.
 */
public final class ContainerInfo {

    private final int index;
    private final String contextRoot;

    public ContainerInfo(int index, String contextRoot) {
        this.index = index;
        this.contextRoot = contextRoot;
    }

    public int getIndex() {
        return index;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public String getQualifier() {
        return "cluster-node-" + (index + 1);
    }

    public UriBuilder getUriBuilder() {
        return UriBuilder.fromUri(contextRoot);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContainerInfo that)) {
            return false;
        }
        return index == that.index && Objects.equals(contextRoot, that.contextRoot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, contextRoot);
    }

    @Override
    public String toString() {
        return getQualifier() + "(" + contextRoot + ")";
    }
}
