package org.keycloak.policy;

/**
 * User attributes available for password-policy validation. Unset attributes are {@code null}.
 */
public final class PasswordPolicyContext {

    private final String username;
    private final String email;

    private PasswordPolicyContext(Builder builder) {
        this.username = builder.username;
        this.email = builder.email;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the effective username, or {@code null} if unavailable
     */
    public String username() {
        return username;
    }

    /**
     * @return the email address, or {@code null} if unavailable
     */
    public String email() {
        return email;
    }

    public static final class Builder {

        private String username;
        private String email;

        /**
         * @param username the effective username, or {@code null} if unavailable
         * @return this builder
         */
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * @param email the email address, or {@code null} if unavailable
         * @return this builder
         */
        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public PasswordPolicyContext build() {
            return new PasswordPolicyContext(this);
        }
    }
}
