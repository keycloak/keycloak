package org.keycloak.testsuite.saml;

import java.net.URI;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.saml.SamlConstants;

import static org.keycloak.testsuite.util.Matchers.isSamlResponse;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_PORT;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SCHEME;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author mhajas
 */
public abstract class AbstractSamlTest extends AbstractAuthTest {

    public static final String REALM_NAME = "demo";

    // From testsaml.json
    public static final String REALM_PRIVATE_KEY = "MIIEpQIBAAKCAQEA3wAQl0VcOVlT7JIttt0cVpksLDjASjfI9zl0c7U5eMWAt0SCOT1EIMjPjtrjO8eyudi7ckwP3NcEHL3QKoNEzwxHpccW7Y2RwVfsFHXkSRvWaxFtxHGNd1NRF4RNMGsCdtCyaybhknItTnOWjRy4jsgHmxDN8rwOWCF0RfnNwXWGefUcF1fe5vpNj+1u2diIUgaR9GC4zpzaDNT68fhzSt92F6ZaU4/niRdfBOoBxHW25HSqqsDKS/xMhlBB19UFUsKTraPsJjQTEpi0vqdpx88a2NjzKRaShHa/p08SyY5cZtgU99TjW7+uvWD0ka4Wf+BziyJSU0xCyFxek5z95QIDAQABAoIBABDt66na8CdtFVFOalNe8eR5IxYFsO4cJ2ZCtwkvEY/jno6gkCpRm7cex53BbE2A2ZwA939ehY3EcmF5ijDQCmHq6BLjzGUjFupQscbT3w2AeYS4rAFP2ueGLGUr/BgtkjWm869CzQ6AcIQWLlsZemwMhNdMLUu85HHjCEq6WNko3fnZ3z0vigSeV7u5LpYVlSQ6dQnjBU51iL7lmeTRZjzIQ8RSpuwi/7K+JKeHFaUSatb40lQRSnAa/ZJgtIKgmVl21wPuCmQALSB/orY6jMuXFpyAOZE3CuNQr18E3o3hPyPiuAR9vq4DYQbRE0QmsLe/eFpl2lxay+EDb9KcxnkCgYEA9QcldhmzqKJMNOw8s/dwUIiJEWTpbi3WyMtY9vIDbBjVmeuX1YerBRfX3KhaHovgcw4Boc6LQ7Kuz7J/1OJ0PvMwF3y17ufq6V3WAXbzivTSCRgd1/53waPdrYiRAeAhTWVjL+8FvUbT1YlWSMYbXTdK8LZWm0WTMcNb9xuwIPMCgYEA6PxoETNRuJNaAKiVNBQr4p+goaUKC4m/a1iwff4Sk7B8eI/AsNWsowe9157QUOmdiVTwuIvkX8ymEsvgQxM7l5TVly6TuQNtf/oDMgj3h+23Wy50v4ErLTxYTnk4YGvAbhGEeRcxtVd3GP74avgID/pUiWyS8Ii052LR6l1PW8cCgYEAz987McFGQKdHvZI5QXiHKVtb5YzV2Go9EGYrWH0i2B8Nf6J2UmnhddWvhPyyT73dMd7NFaezUECTu5K0jjd75TfNMe/ULRVFnqvD9cQjg1yFn798+hRhJr9NPn5gftXViuKbzjuag+RFrJ/xupWO+3sAMcyPFvVkldAmAjLULm8CgYEAkDacW/k+HlfnH/05zbCmsXJJRYUYwKeU+uc859/6s7xMb3vbtBmu8IL8OZkuLMdOIhGXp0PAKqRML9pOiHZBLsSLqTbFbYH3p32juLbgMR0tn50T2u4jQa7WokxaXySTSg5Bx4pZ1Hu9VpWMQvogU3OKHD4+ffDAuXDrqnvzgUUCgYEAvoWI1az7E/LP59Fg6xPDSDnbl9PlQvHY8G7ppJXYzSvVWlk7Wm1VoTA4wFonD24okJ8jgRw6EBTRkM0Y8dg2dKvynJw3oUJdhmHL4mnb6bOhMbFU03cg9cm/YR1Vb/1eJXqrFYdnrMXx9T9udUT6OAKCkER+/uRv8gARRSzOYIE=";
    public static final String REALM_PUBLIC_KEY;
    public static final String REALM_SIGNING_CERTIFICATE;

    public static final String SAML_ASSERTION_CONSUMER_URL_SALES_POST = AUTH_SERVER_SCHEME + "://localhost:" + (AUTH_SERVER_SSL_REQUIRED ? AUTH_SERVER_PORT : 8080) + "/sales-post/saml";
    public static final String SAML_CLIENT_ID_SALES_POST = "http://localhost:8280/sales-post/";

    public static final String SAML_CLIENT_ID_ECP_SP = "http://localhost:8280/ecp-sp/";
    public static final String SAML_ASSERTION_CONSUMER_URL_ECP_SP = AUTH_SERVER_SCHEME + "://localhost:" + (AUTH_SERVER_SSL_REQUIRED ? AUTH_SERVER_PORT : 8080) + "/ecp-sp/saml";

    public static final String SAML_ASSERTION_CONSUMER_URL_SALES_POST2 = AUTH_SERVER_SCHEME + "://localhost:" + (AUTH_SERVER_SSL_REQUIRED ? AUTH_SERVER_PORT : 8080) + "/sales-post2/saml";
    public static final String SAML_CLIENT_ID_SALES_POST2 = "http://localhost:8280/sales-post2/";

    public static final String SAML_URL_SALES_POST_SIG = "http://localhost:8080/sales-post-sig/";
    public static final String SAML_CLIENT_ID_SALES_POST_SIG = "http://localhost:8280/sales-post-sig/";
    public static final String SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG = AUTH_SERVER_SCHEME + "://localhost:" + (AUTH_SERVER_SSL_REQUIRED ? AUTH_SERVER_PORT : 8080) + "/sales-post-sig/";

    public static final String SAML_CLIENT_ID_SALES_POST_ASSERTION_AND_RESPONSE_SIG = "http://localhost:8280/sales-post-assertion-and-response-sig/";
    public static final String SAML_ASSERTION_CONSUMER_URL_SALES_POST_ASSERTION_AND_RESPONSE_SIG = AUTH_SERVER_SCHEME + "://localhost:" + (AUTH_SERVER_SSL_REQUIRED ? AUTH_SERVER_PORT : 8080) + "/sales-post-assertion-and-response-sig/";

    public static final String SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCay+v4QHb/lGOYCPrltH36W/newc3DvvsKHst6JGzM1yMiSuGM6UE7fuzWe6PHQ3IDuezz47iEGTAhRuyfcIQ9yt7iqsOSrEdXFx5xyoO6jt2Aty3FDLGbuLAR2HtYDe/sjyVqWZ0+KyRwuOqx5WcXVzpA6JRuTAZdXLDjBLqliOSFziVa6e0vml6EZrZyjHCvhjLASSxLmZ2Y/caXAggnQAeSOncCQWGT5Rlae68gMWUpGqq87bML0XjLUtHGVS6IUrD5mMaXeS8dvekqbs99ZA9YSlCH3ewMOiWamrv/HB+eENRt217vx/QOfzqXLF1md5vuahOdWA2bRSs0g+CFAgMBAAECggEADFHulfOLhQvqYU9VqLKU1Dg9ytzh45JvqH6K802y2xrEURZknTJzXqjbcLamclWL3YAOu9qD9P+MNRnP+2CZJdHfq2qr5iCQDP5qDeRwV4jHWFc77VL1T8+DM+hm6LszPUCnWa+INEp6u/37r+zyJ4lpuYDJT339g7C841SdBk+fGBXXTjsmhVjbG7uhLhj3QELFy43tnoP7BJCm0SV7T9PJk/LF5zCt1TP8gtQW2Dbh/Fo+zqRl7e7Kl7ivTmCXOC6sReBEBY4rUlitNs8+7JZ7c3oR7MBFT/Bg2I8xPgPxLqHcE9afLo4BJL6cVUG8qfrcwNIks/WsCEVC+xtgAQKBgQDXIWttO1/P0/lTbNYKrpWm/afjF4Z155QU7YKrxsTqmjQcj96bacMYBcWxrI6sR3DoEGdlwkK9m8n6MUuVP7udVlnFgegzwAUbuh5vj/wN7b+A52HE92osho6i/69R2lpf/qW0WbThV+w985S5bvgWRlZ+MPOFfNJnbCk2JnVjRQKBgQC4ND3XVZpfS/RCnxZRxEBXYH4108SeOMC/OEIBmKpxUJ/forv2l2KOXuq1FlaMc5AWEQhRG335P+VPKluziogEM0DyT89/tZQ6oK2hrXf4vCNgZ//WnbGvBWQC86dmWq+5/9ut7xwiP0KY6j2w3nukyJglzgIQLu7gZTrQ6gO8QQKBgCTI3BuTWXCp6CnxpC+zZndlS/2ymhUzAckKS5ufozASKfLwTrn4PZmv8vvMa6Ddnlqv414s90iAiOq03x2oBiaDC1qQOeoPXVA+ZNHiptCi5GngJoGNZKQ0ZdNNMOcoFOfxHNhXtmwJoqV9LuL+LIFyiXuOVzVuAHQatHlD3jKZAoGBALFuKM89dpgqmlp90MrsBankmU2R8UcSlZ7bOsE845iIt6Z7oyAwy88lYGET5kQdoIGQ4Hj3yU0IHgI2Y+Q6ITAiioGdHNr/9YrPcNBWPkSKPG1FX+rDNP7Ia4BoYCu4WKIJ8PnGY0wdsTGIF+pBM8oTKnLnz5b1DkV5XMEVWInBAoGAXr/RZ54nvWcvb/L995iy1uFaTooL5Rcb8XBIGaAeHbiIHy0AYH8lc7E2QW54dIvfaguWDDiebdfKHdjgdOrIYvlicGlCk9jFlsdzv/e28C37F67btX86XUzzM7uchU9kECLLM1igVvHPVQv3l3AIgEri2pcSOZlbE17zS+pKRlU=";
    public static final String SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmsvr+EB2/5RjmAj65bR9+lv53sHNw777Ch7LeiRszNcjIkrhjOlBO37s1nujx0NyA7ns8+O4hBkwIUbsn3CEPcre4qrDkqxHVxceccqDuo7dgLctxQyxm7iwEdh7WA3v7I8lalmdPiskcLjqseVnF1c6QOiUbkwGXVyw4wS6pYjkhc4lWuntL5pehGa2coxwr4YywEksS5mdmP3GlwIIJ0AHkjp3AkFhk+UZWnuvIDFlKRqqvO2zC9F4y1LRxlUuiFKw+ZjGl3kvHb3pKm7PfWQPWEpQh93sDDolmpq7/xwfnhDUbdte78f0Dn86lyxdZneb7moTnVgNm0UrNIPghQIDAQAB";
    public static final PrivateKey SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY_PK;
    public static final PublicKey SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY_PK;

    static {
        try {
            CryptoIntegration.init(Thread.currentThread().getContextClassLoader());
            KeyFactory kfRsa = KeyFactory.getInstance("RSA");
            SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY_PK = kfRsa.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY)));
            SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY_PK = kfRsa.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)));

            PrivateKey privateK = PemUtils.decodePrivateKey(REALM_PRIVATE_KEY);
            PublicKey publicK = KeyUtils.extractPublicKey(privateK);
            REALM_PUBLIC_KEY = PemUtils.encodeKey(publicK);
            Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(new KeyPair(publicK, privateK), "demo");
            REALM_SIGNING_CERTIFICATE = PemUtils.encodeCertificate(certificate);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    // Set date to past (For example with "faketime" utility); then: openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 1 -nodes -subj '/CN=http:\/\/localhost:8080\/sales-post-sig\/'
    public static final String SAML_CLIENT_SALES_POST_SIG_EXPIRED_PRIVATE_KEY = SamlConstants.SAML_CLIENT_SALES_POST_SIG_EXPIRED_PRIVATE_KEY;
    public static final String SAML_CLIENT_SALES_POST_SIG_EXPIRED_PUBLIC_KEY = SamlConstants.SAML_CLIENT_SALES_POST_SIG_EXPIRED_PUBLIC_KEY;
    public static final String SAML_CLIENT_SALES_POST_SIG_EXPIRED_CERTIFICATE = SamlConstants.SAML_CLIENT_SALES_POST_SIG_EXPIRED_CERTIFICATE;
    public static final String SAML_ASSERTION_CONSUMER_URL_SALES_POST_ENC =  AUTH_SERVER_SCHEME + "://localhost:" + (AUTH_SERVER_SSL_REQUIRED ? AUTH_SERVER_PORT : 8080) + "/sales-post-enc/saml";
    public static final String SAML_CLIENT_ID_SALES_POST_ENC = "http://localhost:8280/sales-post-enc/";
    public static final String SAML_CLIENT_SALES_POST_ENC_PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDE5iKDNNW5XxHAF0ITErZcHDYZI68z7u68n7o4dsiywkfOWf7jVnw7PJVnMeDEtLWtTO6f0tRTqJ4OV5HYdJ9+mhPJtn+2UuvrepyYa2IsC1eFPH98ZEtYapsE6ObvhKBQMcu5G/tQrxkCFY2ssDa99unwBH5STLyX78UvqKiYnkPCvIhkiPIHy8ab7DQowc+EE9XhlE3b63A65rp4G9R87rwgJX5VTM3h81WcDuWLPOg7YRYLZoorWz2p38/qL9gXY5NxIRK16EHGfw2W1dPrX3GyMOJbXVyrBNZ6m5IL9Wn7lBEJ/Dl7ZFMFB5W36QkJ+3aaNLT/Tu/Gz+7f24inAgMBAAECggEATiW0zvR6Ww9jgST6AY3suNQtmH60O915/X07sMtcTq6TR1AqvNoHho8+EO4X8ppyfOzKzL4lrWqACNsytIFdCCdo8ScwuxFgN167pjcAiNCblPL0+k7oJJhzHFi/x5KQ+iM5Yye68EP+nfgl+cMahvznzm5KIKn6NCdi0M6U07VRuPIep0v5geqwLOYRWMm8guis5V1p6tpPm6ejplea0QaNpkGxpNuzE2GDJotPRja1TNZUBDV0cKPVY+00BOeuqbiM90V+uk+zRMb9UeeRsuufx2fnLythff19NTgnukgzxWPfU9sSzHen1If1Ul5Xmv3VRG6XhwvOWsLm1TqVuQKBgQD4YgOkRMtpm6BFhOp6pjBcy/H1hN54cMqcTHtpL4w9X7bW+LoN9alfxZiHIRS8+HNATpRtjyKoo5yOQ09NH12/4lFpEIPdkQPzJQIb+kh//QMqqtGcRblCitNObHnlz/HhYDJ3C0nA9frfXhkv3doBAKEELytceGbS1fJ2PcIi2wKBgQDK7+9AmuWXe1qtDt/21j5ymsqhDFjuriPdT6LNvE9ep36h+XRHLe7XEKCKqyOsfYJvK7QI8QQbvB8Jto3pxJf41kBJxmzI9n4SnBKKhInoIICRXXQN4tTDoXVXQGun0idvyhrNEVL3ryW3XPX/UJHFy/Hfjab0sYJm6F50WcQtJQKBgGojUBURBK8zPnCWlLAmdgIhcFqPFZX39MyHbjELjWzoirQgAzlV4bO4Ny5/N2Js9KrlKU4L3S6dA5hTMP7uyVvmtQ0lboPupPZwuQ8Fi5eNoZ3I8ttJfBnwQs1/UzOeAWlidw4ht7mKI1Lx3edzcOX+w8+K7IeON7oejIZ0a5IDAoGAXDrpmIoNWGg2kLpW7V73aKyS9NigvnEkWZus2SYBSHqFIeY2g3cLunCTFhKrluQ/2HibTQkEnfpEfOyb2KeBjhUJiL4GiNsF9z05a/zKlFXZOLepW/pASlzh8HKVuuLXC4Zl4ddCxtCyKoC0SIH8jlGfLsO5IjJemph2/RgjAYUCgYEAkE98bIHsK9jPbt+wnPPs6kyDGHy1JrG9yBlcHOPxsnpxWLFXuxU+9D0qkpbfA28D4jAgehpePzlNPXkF4uIlgarYRDIKss/dX6QQXmmBKjY8UEu+doZYpJGO9SnSuUyih6eRlC/7x9zER/uPjJYia055u2VB0GqO51PKAgq/tqc=";
    public static final String SAML_CLIENT_SALES_POST_ENC_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxOYigzTVuV8RwBdCExK2XBw2GSOvM+7uvJ+6OHbIssJHzln+41Z8OzyVZzHgxLS1rUzun9LUU6ieDleR2HSffpoTybZ/tlLr63qcmGtiLAtXhTx/fGRLWGqbBOjm74SgUDHLuRv7UK8ZAhWNrLA2vfbp8AR+Uky8l+/FL6iomJ5DwryIZIjyB8vGm+w0KMHPhBPV4ZRN2+twOua6eBvUfO68ICV+VUzN4fNVnA7lizzoO2EWC2aKK1s9qd/P6i/YF2OTcSEStehBxn8NltXT619xsjDiW11cqwTWepuSC/Vp+5QRCfw5e2RTBQeVt+kJCft2mjS0/07vxs/u39uIpwIDAQAB";

    public static final String SAML_CLIENT_ID_EMPLOYEE_2 = "http://localhost:8280/employee2/";
    public static final String SAML_CLIENT_ID_EMPLOYEE_SIG = "http://localhost:8280/employee-sig/";

    public static final String SAML_BROKER_ALIAS = "saml-broker";

    protected final AtomicReference<NameIDType> nameIdRef = new AtomicReference<>();
    protected final AtomicReference<String> sessionIndexRef = new AtomicReference<>();

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/adapter-test/keycloak-saml/testsaml.json"));
    }

    @Override
    protected boolean modifyRealmForSSL() {
        return true;
    }

    protected AuthnRequestType createLoginRequestDocument(String issuer, String assertionConsumerURL, String realmName) {
        return SamlClient.createLoginRequestDocument(issuer, assertionConsumerURL, getAuthServerSamlEndpoint(realmName));
    }

    protected URI getAuthServerSamlEndpoint(String realm) throws IllegalArgumentException, UriBuilderException {
        return RealmsResource
                .protocolUrl(UriBuilder.fromUri(getAuthServerRoot()))
                .build(realm, SamlProtocol.LOGIN_PROTOCOL);
    }

    protected URI getAuthServerBrokerSamlEndpoint(String realm, String identityProviderAlias) throws IllegalArgumentException, UriBuilderException {
        return RealmsResource
                .realmBaseUrl(UriBuilder.fromUri(getAuthServerRoot()))
                .path("broker/{idp-name}/endpoint")
                .build(realm, identityProviderAlias);
    }

    protected URI getAuthServerRealmBase(String realm) throws IllegalArgumentException, UriBuilderException {
        return RealmsResource
                .realmBaseUrl(UriBuilder.fromUri(getAuthServerRoot()))
                .build(realm);
    }

    protected URI getSamlBrokerUrl(String realmName) {
        return URI.create(getAuthServerRealmBase(realmName).toString() + "/broker/" + SAML_BROKER_ALIAS + "/endpoint");
    }

    protected SAML2Object extractNameIdAndSessionIndexAndTerminate(SAML2Object so) {
        assertThat(so, isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        ResponseType loginResp1 = (ResponseType) so;
        final AssertionType firstAssertion = loginResp1.getAssertions().get(0).getAssertion();
        assertThat(firstAssertion, org.hamcrest.Matchers.notNullValue());
        assertThat(firstAssertion.getSubject().getSubType().getBaseID(), instanceOf(NameIDType.class));

        NameIDType nameId = (NameIDType) firstAssertion.getSubject().getSubType().getBaseID();
        AuthnStatementType firstAssertionStatement = (AuthnStatementType) firstAssertion.getStatements().iterator().next();

        nameIdRef.set(nameId);
        sessionIndexRef.set(firstAssertionStatement.getSessionIndex());

        return null;
    }
}
