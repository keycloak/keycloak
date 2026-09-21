package org.keycloak.operator.controllers;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.stream.Stream;

import org.keycloak.operator.crds.v2alpha1.client.KeycloakClientStatusCondition;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakOIDCClientBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KeycloakClientBaseControllerTest {

    /**
     * A self-signed end-entity (leaf) certificate with basicConstraints CA:FALSE.
     * Generated with: openssl req -x509 -newkey rsa:2048 -days 3650 -nodes
     *   -subj "/CN=leaf" -addext "basicConstraints=critical,CA:FALSE" -addext "keyUsage=digitalSignature"
     */
    private static final String LEAF_CERT_BASE64 =
            "MIIC6DCCAdCgAwIBAgIUJWec0PsSAPd04DjksQhHR8K6n4gwDQYJKoZIhvcNAQEL" +
            "BQAwDzENMAsGA1UEAwwEbGVhZjAeFw0yNjA5MjExNjIyMDFaFw0zNjA5MTgxNjIy" +
            "MDFaMA8xDTALBgNVBAMMBGxlYWYwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEK" +
            "AoIBAQCYDDpPkMwAekg+t1ABtI7mk+MD4IzVmJ95U7Ir9qkqdjEUul/VDyuV0iZb" +
            "5E1bybdsNJgnpgmCYLmx3krmvNwNonxvUsz9n72iRgW+zgRw5F3eAOHs2NqMy1ke" +
            "E5IZctRb3KzHrVed0E+y8THW3KpLH72enRbmqcVu4FHussBA2+r9ot6MGuPJF6sM" +
            "tXSAvsZ9LkdFJW1HWpQ2V0qGEJLeNrr29gsf0B84OVsecXe5m6AX1Mpmm+ESpxbB" +
            "PNabyDeID4P1zYZwnjqXeQ/4U+OTkb2t0ioKRwyGqcpH281P9mw7uxMSIBFIU6Z3" +
            "JOXjrRZH7s9WeFkl/hgz1ak4SG85AgMBAAGjPDA6MAwGA1UdEwEB/wQCMAAwCwYD" +
            "VR0PBAQDAgeAMB0GA1UdDgQWBBR/xpKVv1uNineTnZJh7wY/nwyE2zANBgkqhkiG" +
            "9w0BAQsFAAOCAQEABNeWOClA7w5F64ORcFt9pY147RHfRh7e9wy6+pb+0WgIuqqS" +
            "oXB+VbqMxhy29u6ki5vSh0fAPeiPXQD9338jELwU0Dai0/zforFMDwPTDFbfOIlt" +
            "CgjbTUl72msZFS+QXLPnW/CEhvliu2qQfRwZkpYvjIT/vEqqIC4W6nswX58JdaUI" +
            "Ph1E2gDarKgmt8EiyotOE6dAmzlUmvrlOhONG5xdEaUcNXEIye3ruBiAdcEk0kTd" +
            "Y9Kryj7xbY3j2lmhmQ4z8zJrxLWgQyKMdLwyaTt8wy3ORv+E16IrMaJoWdX1NR6" +
            "6hAgPYfwPYx4tm9Luu6o0pHj50p27UUTZuyUvEw==";

    /**
     * A self-signed CA certificate with basicConstraints CA:TRUE.
     * Generated with: openssl req -x509 -newkey rsa:2048 -days 3650 -nodes
     *   -subj "/CN=ca" -addext "basicConstraints=critical,CA:TRUE"
     */
    private static final String CA_CERT_BASE64 =
            "MIIC+zCCAeOgAwIBAgIUC/zKaYtAAVmEQ2Kf8wHqT5+W494wDQYJKoZIhvcNAQEL" +
            "BQAwDTELMAkGA1UEAwwCY2EwHhcNMjYwOTIxMTYyMjAxWhcNMzYwOTE4MTYyMjAx" +
            "WjANMQswCQYDVQQDDAJjYTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB" +
            "AL2/Mz6NmWoGJvgd3xcVEa5bQDZpS7mcel8ukikoVjwHu3ut6ahkZoTT8tnVD3Ry" +
            "FqHWlKKog24F7Q+/kHLdOAK7yrasNbANuOBDBwDoxdj8GiyGlpQXHS4NWRCIYVCg" +
            "7q0JJIZcDm4ckPheGXubaKhJ/8sdLBz2X6Anr86fxxDybZ7Ogg4Lej47j1ShMkzK" +
            "Zfap/Kp6oXWU4WYDKmdeX2XW2SOgbl2sCfLKX5htGf4gfEQrzA4GRxo3oEY0Gmm" +
            "vdCPrPqhGW31508pPgHFWejjgWDcvHJzciO+Fr0lCBpCAV7zCYIBduQrgQD1o387" +
            "YlWp5sx/wevesoctB5PS5/Y0CAwEAAaNTMFEwHQYDVR0OBBYEFAMOP3fzSikhV2QL" +
            "RieS7dD4TwJ7MB8GA1UdIwQYMBaAFAMOP3fzSikhV2QLRieS7dD4TwJ7MA8GA1Ud" +
            "EwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBAG+zmxsjCOlVS7LDM3Kcux1X" +
            "EfqE+ONd8svTn9mOCe3as8B+TG29n66yQK8nQ2IP9UVSbISMWy/oPyJ7aZfjwP7W" +
            "eLuxTgHmdoNxfNY+A1AyEtRq3+P8EoQCQX5pLKkMioSj/anihsN29FHuCKteD3i2" +
            "6BEJl2/gvK+TVBVSFZH3aTGksZqE/KhMKM36544dEHQ9ffJMU2UqFoNSKWO4DY3B" +
            "jl4SFAt7YWvKzIbdrPeD1rk+6nAgvcnEYUr+DOLoEZz3fubHo2WyTKKFAFiJU/86" +
            "lXIFEty6IQ2kHBQgId0Vc+u4oSziV+C2USk7jTeCExZgwEO5RVgvXAckk7qE1rU=";

    @Test
    public void testErrorStatus() {
        var client = new KeycloakOIDCClientBuilder().withNewMetadata().endMetadata().build();
        var agg = new KeycloakClientBaseController.KeycloakClientStatusAggregator(client);
        agg.setCondition(KeycloakClientStatusCondition.HAS_ERRORS, true, "some error");
        var status = agg.build();
        var condition = status.getConditions().get(0);
        assertEquals(KeycloakClientStatusCondition.HAS_ERRORS, condition.getType());
        assertEquals(true, condition.getStatus());
        assertEquals("some error", condition.getMessage());

        client.getMetadata().setGeneration(1L);
        client.setStatus(status);
        agg = new KeycloakClientBaseController.KeycloakClientStatusAggregator(client);
        agg.setCondition(KeycloakClientStatusCondition.HAS_ERRORS, false, "");
    }

    @Test
    public void testValidateLeafCertificateAcceptsEndEntityCert() throws Exception {
        X509Certificate cert = decodeCert(LEAF_CERT_BASE64);
        // must not throw
        KeycloakClientBaseController.validateLeafCertificate(cert, "my-tls-secret");
    }

    @Test
    public void testValidateLeafCertificateRejectsCACert() throws Exception {
        X509Certificate cert = decodeCert(CA_CERT_BASE64);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> KeycloakClientBaseController.validateLeafCertificate(cert, "my-tls-secret"));
        assertTrue(ex.getMessage().contains("my-tls-secret"));
        assertTrue(ex.getMessage().contains("CA certificate"));
    }

    /**
     * A leaf certificate with SAN DNS:keycloak.mynamespace.svc.
     * Generated with: openssl req -x509 -newkey rsa:2048 -days 3650 -nodes
     *   -subj "/CN=keycloak" -addext "basicConstraints=critical,CA:FALSE"
     *   -addext "keyUsage=digitalSignature" -addext "subjectAltName=DNS:keycloak.mynamespace.svc"
     */
    private static final String SVC_CERT_BASE64 =
            "MIIDODCCAiCgAwIBAgIUKuXG18JM94y08v3qa2GYnV9m5XEwDQYJKoZIhvcNAQEL" +
            "BQAwEzERMA8GA1UEAwwIa2V5Y2xvYWswHhcNMjYwOTIxMTYzNDQ1WhcNMzYwOTE4" +
            "MTYzNDQ1WjATMREwDwYDVQQDDAhrZXljbG9hazCCASIwDQYJKoZIhvcNAQEBBQAD" +
            "ggEPADCCAQoCggEBALGcQy1J3rnZQXsHhmEHTHbnAxglK29y19rHnw9RfU+26GknS" +
            "+siLhf3xrqyuWeGI14YS80P+z7IGzrQsIzWwjaLo1gO9ucVYzue2Bsdod6AfGtrT" +
            "Nd3wn+3iqSstfX7JQpAbMhz+axb4rg1whUXsrtZb6+eaNPKdNsmNIk8XNcJ+Qtni" +
            "PC5ELHbcutSim0ATpV18O/a7MbC52qPlvCQL5osHVSIycSE/iGH/UfRcd8/WlTyU" +
            "fdEGOIdnr2DLfd6JidfmVmSWTVeDghmRguVj8u2+tUJITCdQmOv/bx5Djo8Q5p7h" +
            "i0c4Q/VxMrDZh/GnsM1aKDhWhy+3mYPnNQOjUECAwEAAaOBgzCBgDAdBgNVHQ4E" +
            "FgQUamRT9eFoCzPIOPhECWQt33T06dwwHwYDVR0jBBgwFoAUamRT9eFoCzPIOPhE" +
            "CWQt33T06dwwDAYDVR0TAQH/BAIwADALBgNVHQ8EBAMCB4AwIwYDVR0RBBwwGoIY" +
            "a2V5Y2xvYWsubXluYW1lc3BhY2Uuc3ZjMA0GCSqGSIb3DQEBCwUAA4IBAQAoreF2" +
            "SAFv0Cf+NPyHSRoHJy8EeNkJxHtjzike1bbtefxJZIKfLnpAAoZg7eKSQ9NB7Alr" +
            "4+uM4DeHEod5VcrhOd+vCX6O9tQuAzk32mRZ/uHsMF0n8pPZFtx2mEspfUkFbHGO" +
            "PJekPOxJdpqjFDtpNofTAoMTflr9+Oqj/bkcp5EcqJ4JOlHeEMjt/V95q4ckAfj8" +
            "RT9U7/SROTTT0mv9U5V+zg61+rkKP0tJZdqE9rM0L34Md9tYoB/u9ZXwLGP6t2q2" +
            "h7AmR6gruisQgWSilRLB9VPumGzQN81kWKK1vDBaSn2uFKNJ6LOQBMYVxESPeQUI" +
            "q/I+XSmXTURR6dZv";

    static Stream<Arguments> certCoversHostnameCases() throws Exception {
        X509Certificate svcCert = decodeCert(SVC_CERT_BASE64);
        X509Certificate leafCert = decodeCert(LEAF_CERT_BASE64);
        return Stream.of(
                // cert with matching SAN → covers the hostname
                Arguments.of(svcCert, "keycloak.mynamespace.svc", true),
                // cert with matching SAN → does not cover a different service hostname
                Arguments.of(svcCert, "keycloak.othernamespace.svc", false),
                // cert with only CN=leaf, no SAN → does not cover a service hostname
                Arguments.of(leafCert, "keycloak.mynamespace.svc", false)
        );
    }

    @ParameterizedTest
    @MethodSource("certCoversHostnameCases")
    public void testCertCoversHostname(X509Certificate cert, String hostname, boolean expected) {
        assertEquals(expected, KeycloakClientBaseController.certCoversHostname(cert, hostname));
    }

    @Test
    public void testNoopHostnameVerifierUsedWhenCertDoesNotCoverServiceHostname() throws Exception {
        // LEAF_CERT_BASE64 has CN=leaf and no SANs — does not cover a Kubernetes service hostname
        assertFalse(KeycloakClientBaseController.certCoversHostname(decodeCert(LEAF_CERT_BASE64), "keycloak.mynamespace.svc"));
    }

    @Test
    public void testDefaultHostnameVerifierUsedWhenCertCoversServiceHostname() throws Exception {
        // SVC_CERT_BASE64 has SAN DNS:keycloak.mynamespace.svc
        assertTrue(KeycloakClientBaseController.certCoversHostname(decodeCert(SVC_CERT_BASE64), "keycloak.mynamespace.svc"));
    }

    private static X509Certificate decodeCert(String base64) throws Exception {
        byte[] der = Base64.getMimeDecoder().decode(base64);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(der));
    }

}
