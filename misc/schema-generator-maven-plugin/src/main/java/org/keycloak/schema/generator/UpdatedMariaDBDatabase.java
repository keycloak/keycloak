package org.keycloak.schema.generator;

import java.util.HashSet;
import java.util.Set;

import liquibase.database.core.MariaDBDatabase;

/**
 * MariaDB database that treats additional words as reserved so that they are quoted in the generated SQL.
 */
public class UpdatedMariaDBDatabase extends MariaDBDatabase {

    private static final Set<String> RESERVED_WORDS = new HashSet<>();

    static {
        RESERVED_WORDS.add("PERIOD");
    }

    @Override
    public boolean isReservedWord(String string) {
        return super.isReservedWord(string) || RESERVED_WORDS.contains(string.toUpperCase());
    }

    @Override
    public int getPriority() {
        return super.getPriority() + 1; // Always take precedence over factory MariaDBDatabase
    }
}
