package org.keycloak.representations.docker;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;


/**
 * Per the docker auth v2 spec, access is defined like this:
 *
 *        {
 *        "type": "repository",
 *        "name": "samalba/my-app",
 *        "actions": [
 *           "push",
 *           "pull"
 *         ]
 *        }
 *
 */
public class DockerAccess {

    public static final int ACCESS_TYPE = 0;
    public static final int REPOSITORY_NAME = 1;
    public static final int PERMISSIONS = 2;
    public static final String DECODE_ENCODING = "UTF-8";

    @JsonProperty("type")
    protected String type;
    @JsonProperty("name")
    protected String name;
    @JsonProperty("actions")
    protected List<String> actions;

    public DockerAccess() {
    }

    public DockerAccess(final String scopeParam) {
        if (scopeParam != null) {
            try {
                final String unencoded = URLDecoder.decode(scopeParam, DECODE_ENCODING);
                final String[] parts = unencoded.split(":");
                if (parts.length != 3) {
                    throw new IllegalArgumentException(String.format("Expecting input string to have %d parts delineated by a ':' character.  " +
                            "Found %d parts: %s", 3, parts.length, unencoded));
                }

                type = parts[ACCESS_TYPE];
                name = parts[REPOSITORY_NAME];
                if (parts[PERMISSIONS] != null) {
                    actions = Arrays.asList(parts[PERMISSIONS].split(","));
                }
            } catch (final UnsupportedEncodingException e) {
                throw new IllegalStateException("Error attempting to decode scope parameter using encoding: " + DECODE_ENCODING);
            }
        }
    }

    public String getType() {
        return type;
    }

    public DockerAccess setType(final String type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public DockerAccess setName(final String name) {
        this.name = name;
        return this;
    }

    public List<String> getActions() {
        return actions;
    }

    public DockerAccess setActions(final List<String> actions) {
        this.actions = actions;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof DockerAccess)) return false;

        final DockerAccess that = (DockerAccess) o;

        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return actions != null ? actions.equals(that.actions) : that.actions == null;

    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (actions != null ? actions.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DockerAccess{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", actions=" + actions +
                '}';
    }
}
