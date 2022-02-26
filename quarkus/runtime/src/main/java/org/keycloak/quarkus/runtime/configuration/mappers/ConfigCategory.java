package org.keycloak.quarkus.runtime.configuration.mappers;

public enum ConfigCategory {
    // ordered by name asc
    CLUSTERING("Cluster", 10),
    DATABASE("Database", 20),
    TRANSACTION("Transaction",30),
    FEATURE("Feature", 40),
    HOSTNAME("Hostname", 50),
    HTTP("HTTP/TLS", 60),
    HEALTH("Health", 70),
    METRICS("Metrics", 80),
    PROXY("Proxy", 90),
    VAULT("Vault", 100),
    LOGGING("Logging", 110),
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
