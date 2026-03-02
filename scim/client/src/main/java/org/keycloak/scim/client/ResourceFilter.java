package org.keycloak.scim.client;

/**
 * Fluent builder for SCIM filter expressions. Supports all SCIM filter operators and logical combinations.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class ResourceFilter {

    public static ResourceFilter filter() {
        return new ResourceFilter();
    }

    private final StringBuilder filter = new StringBuilder();

    // Comparison operators

    public ResourceFilter eq(String property, String value) {
        append(property + " eq " + quote(value));
        return this;
    }

    public ResourceFilter ne(String property, String value) {
        append(property + " ne " + quote(value));
        return this;
    }

    public ResourceFilter co(String property, String value) {
        append(property + " co " + quote(value));
        return this;
    }

    public ResourceFilter sw(String property, String value) {
        append(property + " sw " + quote(value));
        return this;
    }

    public ResourceFilter ew(String property, String value) {
        append(property + " ew " + quote(value));
        return this;
    }

    public ResourceFilter gt(String property, Object value) {
        append(property + " gt " + value);
        return this;
    }

    public ResourceFilter ge(String property, Object value) {
        append(property + " ge " + value);
        return this;
    }

    public ResourceFilter lt(String property, Object value) {
        append(property + " lt " + value);
        return this;
    }

    public ResourceFilter le(String property, Object value) {
        append(property + " le " + value);
        return this;
    }

    public ResourceFilter pr(String property) {
        append(property + " pr");
        return this;
    }

    // Logical operators

    public ResourceFilter and() {
        filter.append(" and ");
        return this;
    }

    public ResourceFilter or() {
        filter.append(" or ");
        return this;
    }

    public ResourceFilter not() {
        append("not ");
        return this;
    }

    // Grouping

    public ResourceFilter lparen() {
        filter.append("(");
        return this;
    }

    public ResourceFilter rparen() {
        filter.append(")");
        return this;
    }

    public String build() {
        return filter.toString();
    }

    private void append(String s) {
        if (!filter.isEmpty() && !filter.toString().endsWith("(") && !filter.toString().endsWith("not ")) {
            filter.append(" ");
        }
        filter.append(s);
    }

    private String quote(String value) {
        // Escape backslashes and quotes
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
