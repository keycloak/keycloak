/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0.
 */
package org.keycloak.tests.authz.services.uma;

import java.time.Duration;

import org.keycloak.representations.IDToken;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.tests.authz.services.Permission;
import org.keycloak.tests.authz.services.config.DefaultAuthzServicesServerConfig;
import org.keycloak.tests.authz.services.config.DefaultResourceServerConfig;
import org.keycloak.tests.common.BasicUserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = DefaultAuthzServicesServerConfig.class)
public class UmaStaleIdTokenRevocationTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectUser(config = BasicUserConfig.class)
    ManagedUser user;

    @InjectClient(config = DefaultResourceServerConfig.class, ref = "resource-server-foo")
    ManagedClient resourceServer;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @Test
    public void staleIdTokenRejectedAfterRealmNotBefore() {
        Permission.create(resourceServer)
                .resource("stale-id-token-resource")
                .grant();

        AccessTokenResponse tokenResponse = oauth
                .client(resourceServer.getClientId(), resourceServer.getSecret())
                .doPasswordGrantRequest(user.getUsername(), user.getPassword());

        String idTokenString = tokenResponse.getIdToken();
        assertNotNull(idTokenString);

        IDToken idToken = oauth.parseToken(idTokenString, IDToken.class);
        int notBefore = idToken.getIat().intValue() + 1;

        realm.updateWithCleanup(representation -> representation.notBefore(notBefore));
        timeOffSet.set(Duration.ofSeconds(2));

        assertTrue(idToken.getIat() < notBefore);

        tokenResponse = oauth
                .client(resourceServer.getClientId(), resourceServer.getSecret())
                .permissionGrantRequest()
                .claimToken(idTokenString)
                .send();

        assertEquals(400, tokenResponse.getStatusCode());
        assertEquals("unauthorized_client", tokenResponse.getError());
        assertEquals("Invalid identity", tokenResponse.getErrorDescription());
        assertNull(tokenResponse.getAccessToken());
    }
}
