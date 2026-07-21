package org.keycloak.tests.admin.client.v2;

import java.util.LinkedHashSet;
import java.util.Set;

import org.keycloak.representations.admin.v2.OIDCClientRepresentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepresentationDefensiveCopyTest {

    @Test
    void setPropertiesAreDefensivelyCopied() {
        OIDCClientRepresentation representation = new OIDCClientRepresentation();

        Set<String> redirectUris = new LinkedHashSet<>(Set.of("https://client.example/callback"));
        Set<String> roles = new LinkedHashSet<>(Set.of("client-role"));
        Set<OIDCClientRepresentation.Flow> loginFlows = new LinkedHashSet<>(Set.of(OIDCClientRepresentation.Flow.STANDARD));
        Set<String> webOrigins = new LinkedHashSet<>(Set.of("https://client.example"));
        Set<String> serviceAccountRoles = new LinkedHashSet<>(Set.of("service-role"));

        representation.setRedirectUris(redirectUris);
        representation.setRoles(roles);
        representation.setLoginFlows(loginFlows);
        representation.setWebOrigins(webOrigins);
        representation.setServiceAccountRoles(serviceAccountRoles);

        redirectUris.clear();
        roles.clear();
        loginFlows.clear();
        webOrigins.clear();
        serviceAccountRoles.clear();

        assertEquals(Set.of("https://client.example/callback"), representation.getRedirectUris());
        assertEquals(Set.of("client-role"), representation.getRoles());
        assertEquals(Set.of(OIDCClientRepresentation.Flow.STANDARD), representation.getLoginFlows());
        assertEquals(Set.of("https://client.example"), representation.getWebOrigins());
        assertEquals(Set.of("service-role"), representation.getServiceAccountRoles());
    }
}
