package org.keycloak.quarkus.runtime.configuration.mappers;

public enum ConfigCategory {
    // ordered by name asc
    CLUSTERING("Cluster", 10),
    DATABASE("Database", 20),
    FEATURE("Feature", 30),
    HOSTNAME("Hostname", 40),
    HTTP("HTTP/TLS", 50),
    METRICS("Metrics", 60),
    PROXY("Proxy", 70),
    VAULT("Vault", 80),
    LOGGING("Logging", 90),
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
