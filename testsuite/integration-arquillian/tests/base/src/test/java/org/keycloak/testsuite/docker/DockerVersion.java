package org.keycloak.testsuite.docker;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DockerVersion {

    public static final Integer MAJOR_VERSION_INDEX = 0;
    public static final Integer MINOR_VERSION_INDEX = 1;
    public static final Integer PATCH_VERSION_INDEX = 2;

    private final Integer major;
    private final Integer minor;
    private final Integer patch;

    public static final Comparator<DockerVersion> COMPARATOR = (lhs, rhs) -> Comparator.comparing(DockerVersion::getMajor)
            .thenComparing(Comparator.comparing(DockerVersion::getMinor)
            .thenComparing(Comparator.comparing(DockerVersion::getPatch)))
            .compare(lhs, rhs);

    /**
     * Major version is required.  minor and patch versions will be assumed '0' if not provided.
     */
    public DockerVersion(final Integer major, final Optional<Integer> minor, final Optional<Integer> patch) {
        Objects.requireNonNull(major, "Invalid docker version - no major release number given");

        this.major = major;
        this.minor = minor.orElse(0);
        this.patch = patch.orElse(0);
    }

    /**
     * @param versionString given in the form '1.12.6'
     */
    public static DockerVersion parseVersionString(final String versionString) {
        Objects.requireNonNull(versionString, "Cannot parse null docker version string");

        final List<Integer> versionNumberList = Arrays.stream(stripDashAndEdition(versionString).trim().split("\\."))
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        return new DockerVersion(versionNumberList.get(MAJOR_VERSION_INDEX),
                Optional.ofNullable(versionNumberList.get(MINOR_VERSION_INDEX)),
                Optional.ofNullable(versionNumberList.get(PATCH_VERSION_INDEX)));
    }

    private static String stripDashAndEdition(final String versionString) {
        if (versionString.contains("-")) {
            return versionString.substring(0, versionString.indexOf("-"));
        }

        return versionString;
    }

    public Integer getMajor() {
        return major;
    }

    public Integer getMinor() {
        return minor;
    }

    public Integer getPatch() {
        return patch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DockerVersion that = (DockerVersion) o;

        if (major != null ? !major.equals(that.major) : that.major != null) return false;
        if (minor != null ? !minor.equals(that.minor) : that.minor != null) return false;
        return patch != null ? patch.equals(that.patch) : that.patch == null;
    }

    @Override
    public int hashCode() {
        int result = major != null ? major.hashCode() : 0;
        result = 31 * result + (minor != null ? minor.hashCode() : 0);
        result = 31 * result + (patch != null ? patch.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DockerVersion{" +
                "major=" + major +
                ", minor=" + minor +
                ", patch=" + patch +
                '}';
    }
}
