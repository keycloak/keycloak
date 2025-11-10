package org.keycloak.verifiableclaims.model;

public class VerifiableAttributeConfig {
    private final String name;
    private final boolean requireVerificationForUsers;
    private final boolean allowAdminBypass;
    private final boolean emitInTokensWhenVerified;
    private final String schema;

    public VerifiableAttributeConfig(String name, boolean requireVerificationForUsers,
                                     boolean allowAdminBypass, boolean emitInTokensWhenVerified,
                                     String schema) {
        this.name = name;
        this.requireVerificationForUsers = requireVerificationForUsers;
        this.allowAdminBypass = allowAdminBypass;
        this.emitInTokensWhenVerified = emitInTokensWhenVerified;
        this.schema = schema;
    }
    public String getName() { return name; }
    public boolean isRequireVerificationForUsers() { return requireVerificationForUsers; }
    public boolean isAllowAdminBypass() { return allowAdminBypass; }
    public boolean isEmitInTokensWhenVerified() { return emitInTokensWhenVerified; }
    public String getSchema() { return schema; }
}
