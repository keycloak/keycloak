package org.keycloak.quarkus.runtime.configuration.mappers;

public enum ConfigCategory {
    // ordered by name asc
    CLUSTERING("Cluster", 10),
    DATABASE("Database", 20),
    TRANSACTION("Transaction",30),
    FEATURE("Feature", 40),
    HOSTNAME("Hostname", 50),
    HTTP("HTTP/TLS", 60),
    METRICS("Metrics", 70),
    PROXY("Proxy", 80),
    VAULT("Vault", 90),
    LOGGING("Logging", 100),
    GENERAL("General", 999);

    private final String heading;

    //Categories with a lower number are shown before groups with a higher number
    private final int order;

    ConfigCategory(String heading, int order) {
        this.heading = heading; this.order = order;
    }

    public String getHeading() {
        return this.heading;
    }

    public int getOrder() {
        return this.order;
    }

}
