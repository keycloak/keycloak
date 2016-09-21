package org.keycloak.representations.docker;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * JSON Representation of a Docker Error in the following format:
 *
 *
 * {
 *  "code": "UNAUTHORIZED",
 *  "message": "access to the requested resource is not authorized",
 *  "detail": [
 *    {
 *      "Type": "repository",
 *      "Name": "samalba/my-app",
 *      "Action": "pull"
 *    },
 *    {
 *      "Type": "repository",
 *      "Name": "samalba/my-app",
 *      "Action": "push"
 *    }
 *  ]
 * }
 */
public class DockerError {


    @JsonProperty("code")
    private final String errorCode;
    @JsonProperty("message")
    private final String message;
    @JsonProperty("detail")
    private final List<DockerAccess> dockerErrorDetails;

    public DockerError(final String errorCode, final String message, final List<DockerAccess> dockerErrorDetails) {
        this.errorCode = errorCode;
        this.message = message;
        this.dockerErrorDetails = dockerErrorDetails;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public List<DockerAccess> getDockerErrorDetails() {
        return dockerErrorDetails;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof DockerError)) return false;

        final DockerError that = (DockerError) o;

        if (errorCode != that.errorCode) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        return dockerErrorDetails != null ? dockerErrorDetails.equals(that.dockerErrorDetails) : that.dockerErrorDetails == null;
    }

    @Override
    public int hashCode() {
        int result = errorCode != null ? errorCode.hashCode() : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (dockerErrorDetails != null ? dockerErrorDetails.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DockerError{" +
                "errorCode=" + errorCode +
                ", message='" + message + '\'' +
                ", dockerErrorDetails=" + dockerErrorDetails +
                '}';
    }
}
