package org.keycloak.representations.docker;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DockerErrorResponseToken {


    @JsonProperty("errors")
    private final List<DockerError> errorList;

    public DockerErrorResponseToken(final List<DockerError> errorList) {
        this.errorList = errorList;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof DockerErrorResponseToken)) return false;

        final DockerErrorResponseToken that = (DockerErrorResponseToken) o;

        return errorList != null ? errorList.equals(that.errorList) : that.errorList == null;
    }

    @Override
    public int hashCode() {
        return errorList != null ? errorList.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "DockerErrorResponseToken{" +
                "errorList=" + errorList +
                '}';
    }
}
