package org.keycloak.testframework.database;

public record DatabaseConfig(String initScript, String database, boolean preventReuse) {
}
