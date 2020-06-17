package org.keycloak.models;

public class ContentSecurityPolicyBuilder {

    private String frameSrc = "'self'";
    private String frameAncestors = "'self'";
    private String objectSrc = "'none'";

    private boolean first;
    private StringBuilder sb;

    public static ContentSecurityPolicyBuilder create() {
        return new ContentSecurityPolicyBuilder();
    }

    public ContentSecurityPolicyBuilder frameSrc(String frameSrc) {
        this.frameSrc = frameSrc;
        return this;
    }

    public ContentSecurityPolicyBuilder frameAncestors(String frameancestors) {
        this.frameAncestors = frameancestors;
        return this;
    }

    public String build() {
        sb = new StringBuilder();
        first = true;

        build("frame-src", frameSrc);
        build("frame-ancestors", frameAncestors);
        build("object-src", objectSrc);

        return sb.toString();
    }

    private void build(String k, String v) {
        if (v != null) {
            if (!first) {
                sb.append(" ");
            }
            first = false;

            sb.append(k).append(" ").append(v).append(";");
        }
    }

}
