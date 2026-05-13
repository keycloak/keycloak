package org.keycloak.tests.oid4vc.presentation;

class TestIssuerMetadata {

    private final String json;

    TestIssuerMetadata(String json) {
        this.json = json;
    }

    String json() {
        return json;
    }
}
