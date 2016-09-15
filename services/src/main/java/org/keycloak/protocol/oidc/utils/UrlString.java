package org.keycloak.protocol.oidc.utils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Url String representation
 */
public class UrlString {
    // Per RFC 3986, Appendix B
    public final static String URL_REGEX = "^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?";

    private String protocol;
    private String host;
    private Optional<String> port;
    private Optional<String> path;
    private Optional<String> queryParams;
    private Optional<String> fragment;

    public UrlString(final String urlString) {
        final Pattern pattern = Pattern.compile(URL_REGEX);
        final Matcher matcher = pattern.matcher(urlString);
        if (matcher.find()) {
            protocol = matcher.group(2);
            if (protocol == null || protocol.isEmpty()) {
                throw new IllegalArgumentException("Cannot create UrlString with empty or null protocol: " + urlString);
            }

            final String authority = matcher.group(4);
            final String[] authorityParts = authority.split(":");
            host = authorityParts[0];
            if (host == null || host.isEmpty()) {
                throw new IllegalArgumentException("Cannot create UrlString with empty or null host: " + urlString);
            }
            if (authorityParts.length > 1 && authorityParts[1] != null) {
                port = Optional.of(authorityParts[1]);
            } else {
                port = Optional.empty();
            }

            final String path = matcher.group(5);
            this.path = path != null && !path.isEmpty() ? Optional.of(path) : Optional.empty();

            final String queryParams = matcher.group(7);
            this.queryParams = queryParams != null && !queryParams.isEmpty() ? Optional.of(queryParams) : Optional.empty();

            final String fragment = matcher.group(9);
            this.fragment = fragment != null && !fragment.isEmpty() ? Optional.of(fragment) : Optional.empty();
        }
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public Optional<String> getPort() {
        return port;
    }

    public Optional<String> getPath() {
        return path;
    }

    public Optional<String> getQueryParams() {
        return queryParams;
    }

    public Optional<String> getFragment() {
        return fragment;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof UrlString)) return false;

        final UrlString urlString = (UrlString) o;

        if (protocol != null ? !protocol.equals(urlString.protocol) : urlString.protocol != null) return false;
        if (host != null ? !host.equals(urlString.host) : urlString.host != null) return false;
        if (port != null ? !port.equals(urlString.port) : urlString.port != null) return false;
        if (path != null ? !path.equals(urlString.path) : urlString.path != null) return false;
        if (queryParams != null ? !queryParams.equals(urlString.queryParams) : urlString.queryParams != null)
            return false;
        return fragment != null ? fragment.equals(urlString.fragment) : urlString.fragment == null;

    }

    @Override
    public int hashCode() {
        int result = protocol != null ? protocol.hashCode() : 0;
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + (port != null ? port.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (queryParams != null ? queryParams.hashCode() : 0);
        result = 31 * result + (fragment != null ? fragment.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UrlString{" +
                "protocol='" + protocol + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", path=" + path +
                ", queryParams=" + queryParams +
                ", fragment=" + fragment +
                '}';
    }
}
