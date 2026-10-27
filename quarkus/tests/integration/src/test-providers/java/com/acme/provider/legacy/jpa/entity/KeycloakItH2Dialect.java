package com.acme.provider.legacy.jpa.entity;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * A trivial H2 dialect subclass, behaving exactly like {@link H2Dialect}, used only in tests to prove that a dialect
 * declared in a user's {@code persistence.xml} is honored rather than silently replaced by the auto-detected
 * {@code org.hibernate.dialect.H2Dialect}. Without this, a dialect assertion of {@code H2Dialect} would be a tautology
 * (auto-detection yields the same class), so it could not tell "honored" from "ignored".
 */
public class KeycloakItH2Dialect extends H2Dialect {

    public KeycloakItH2Dialect() {
        super();
    }

    public KeycloakItH2Dialect(DialectResolutionInfo info) {
        super(info);
    }
}
