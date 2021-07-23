package org.keycloak.models;

import java.util.Objects;
import java.util.regex.Pattern;

public class DomainName {
    // something like this
    private static final Pattern DOMAIN = Pattern.compile("^[a-zA-Z0-9]+[a-zA-Z0-9\\.]*");

    private final String domain;

    public DomainName(String domain) {
        Objects.requireNonNull(domain, "domain");
        this.domain = domain.trim().replaceFirst(".+$", "").replaceFirst("^.+", "");

        if (!DOMAIN.asPredicate().test(this.domain)) {
            throw new IllegalArgumentException("invalid domain: " + domain);
        }
    }

    @Override
    public String toString() {
        return domain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DomainName that = (DomainName) o;

        return domain.equals(that.domain);
    }

    @Override
    public int hashCode() {
        return domain.hashCode();
    }
}
