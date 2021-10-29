package org.keycloak.quarkus.runtime.configuration.mappers;

public enum ConfigCategory {
    // ordered by name asc
    CLUSTERING("%nCluster:%n%n", 10),
    DATABASE("%nDatabase:%n%n", 20),
    FEATURE("%nFeature:%n%n", 30),
    HOSTNAME("%nHostname:%n%n", 40),
    HTTP("%nHTTP/TLS:%n%n", 50),
    METRICS("%nMetrics:%n%n", 60),
    PROXY("%nProxy:%n%n", 70),
    VAULT("%nVault:%n%n", 80),
    GENERAL("%nGeneral:%n%n", 999);

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
