package org.keycloak.scim.model.filter;

import jakarta.persistence.criteria.Predicate;

/**
 * A record that encapsulates the result of evaluating a filter expression, including the generated JPA Predicate and a flag
 * indicating whether the filter is unsupported (e.g., due to unrecognized attributes). This allows the visitor to gracefully
 * handle unsupported filters.
 *
 * @param predicate the JPA Predicate generated from the filter expression
 * @param unsupported a flag indicating whether the filter is unsupported (true if unsupported, false if valid)
 * @param authzProtected whether the predicate involves an authorization-checked path (e.g. {@code groups.value eq "id"} that
 *        passed the FGAP permission check). When true, wrapping this predicate in a {@code not} expression is blocked because
 *        negating an authorized {@code eq} would produce the equivalent of {@code ne}, turning a targeted membership check
 *        into a broad membership oracle that leaks hidden relationships.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public record JPAFilterResult(Predicate predicate, boolean unsupported, boolean authzProtected) {

    public static JPAFilterResult valid(Predicate p) {
        return new JPAFilterResult(p, false, false);
    }

    public static JPAFilterResult valid(Predicate p, boolean authzProtected) {
        return new JPAFilterResult(p, false, authzProtected);
    }

    public static JPAFilterResult unsupported(Predicate p) {
        return new JPAFilterResult(p, true, false);
    }
}
