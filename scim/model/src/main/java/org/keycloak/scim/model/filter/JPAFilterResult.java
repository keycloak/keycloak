package org.keycloak.scim.model.filter;

import jakarta.persistence.criteria.Predicate;

/**
 * A record that encapsulates the result of evaluating a filter expression, including the generated JPA Predicate and a flag
 * indicating whether the filter is unsupported (e.g., due to unrecognized attributes). This allows the visitor to gracefully
 * handle unsupported filters.
 *
 * @param predicate the JPA Predicate generated from the filter expression
 * @param unsupported a flag indicating whether the filter is unsupported (true if unsupported, false if valid)
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public record JPAFilterResult(Predicate predicate, boolean unsupported) {

    public static JPAFilterResult valid(Predicate p) {
        return new JPAFilterResult(p, false);
    }

    public static JPAFilterResult unsupported(Predicate p) {
        return new JPAFilterResult(p, true);
    }
}
