package org.keycloak.adapters;

import org.apache.commons.io.FileUtils;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.bouncycastle.util.encoders.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.keycloak.enums.SslRequired;
import org.keycloak.enums.TokenStore;
import org.keycloak.util.PemUtils;

import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.io.IOException;
import java.security.PublicKey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeycloakDeploymentBuilderTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void before() throws IOException {
        File dir = folder.newFolder();
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("/cacerts.jks"), new File(dir, "cacerts.jks"));
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("/keystore.jks"), new File(dir, "keystore.jks"));
        System.setProperty("testResources", dir.getAbsolutePath());
    }

    @After
    public void after() {
        System.getProperties().remove("testResources");
    }

    @Test
    public void load() throws Exception {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getClass().getResourceAsStream("/keycloak.json"));
        assertEquals("demo", deployment.getRealm());
        assertEquals("customer-portal", deployment.getResourceName());
        assertEquals(PemUtils.decodePublicKey("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB"), deployment.getRealmKey());
        assertEquals("https://localhost:8443/auth/realms/demo/protocol/openid-connect/login", deployment.getAuthUrl().build().toString());
        assertEquals(SslRequired.EXTERNAL, deployment.getSslRequired());
        assertTrue(deployment.isUseResourceRoleMappings());
        assertTrue(deployment.isCors());
        assertEquals(1000, deployment.getCorsMaxAge());
        assertEquals("POST, PUT, DELETE, GET", deployment.getCorsAllowedMethods());
        assertEquals("X-Custom, X-Custom2", deployment.getCorsAllowedHeaders());
        assertTrue(deployment.isBearerOnly());
        assertTrue(deployment.isPublicClient());
        assertTrue(deployment.isEnableBasicAuth());
        assertTrue(deployment.isExposeToken());
        assertEquals("234234-234234-234234", deployment.getResourceCredentials().get("secret"));
        assertEquals(20, ((ThreadSafeClientConnManager) deployment.getClient().getConnectionManager()).getMaxTotal());
        assertEquals("https://localhost:8443/auth/realms/demo/protocol/openid-connect/refresh", deployment.getRefreshUrl());
        assertTrue(deployment.isAlwaysRefreshToken());
        assertTrue(deployment.isRegisterNodeAtStartup());
        assertEquals(1000, deployment.getRegisterNodePeriod());
        assertEquals(TokenStore.COOKIE, deployment.getTokenStore());
        assertEquals("email", deployment.getPrincipalAttribute());
    }

}
