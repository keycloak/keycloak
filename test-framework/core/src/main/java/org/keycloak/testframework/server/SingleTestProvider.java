package org.keycloak.testframework.server;

record SingleTestProvider(Class<?> providerFactory, Class<?> spi, String... resourceFiles) {
}
