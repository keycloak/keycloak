package org.keycloak.tests.saml;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
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
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakUrls;

import org.junit.jupiter.api.BeforeEach;

import static org.keycloak.tests.utils.matchers.Matchers.isSamlResponse;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author mhajas
 */
public abstract class AbstractSamlTest {

    @InjectRealm(fromJson = "testsaml.json")
    protected ManagedRealm samlRealm;

    @InjectKeycloakUrls
    protected KeycloakUrls keycloakUrls;

    public static final String REALM_NAME = "demo";

    // From testsaml.json
    public static final String REALM_PRIVATE_KEY = "MIIEpQIBAAKCAQEA3wAQl0VcOVlT7JIttt0cVpksLDjASjfI9zl0c7U5eMWAt0SCOT1EIMjPjtrjO8eyudi7ckwP3NcEHL3QKoNEzwxHpccW7Y2RwVfsFHXkSRvWaxFtxHGNd1NRF4RNMGsCdtCyaybhknItTnOWjRy4jsgHmxDN8rwOWCF0RfnNwXWGefUcF1fe5vpNj+1u2diIUgaR9GC4zpzaDNT68fhzSt92F6ZaU4/niRdfBOoBxHW25HSqqsDKS/xMhlBB19UFUsKTraPsJjQTEpi0vqdpx88a2NjzKRaShHa/p08SyY5cZtgU99TjW7+uvWD0ka4Wf+BziyJSU0xCyFxek5z95QIDAQABAoIBABDt66na8CdtFVFOalNe8eR5IxYFsO4cJ2ZCtwkvEY/jno6gkCpRm7cex53BbE2A2ZwA939ehY3EcmF5ijDQCmHq6BLjzGUjFupQscbT3w2AeYS4rAFP2ueGLGUr/BgtkjWm869CzQ6AcIQWLlsZemwMhNdMLUu85HHjCEq6WNko3fnZ3z0vigSeV7u5LpYVlSQ6dQnjBU51iL7lmeTRZjzIQ8RSpuwi/7K+JKeHFaUSatb40lQRSnAa/ZJgtIKgmVl21wPuCmQALSB/orY6jMuXFpyAOZE3CuNQr18E3o3hPyPiuAR9vq4DYQbRE0QmsLe/eFpl2lxay+EDb9KcxnkCgYEA9QcldhmzqKJMNOw8s/dwUIiJEWTpbi3WyMtY9vIDbBjVmeuX1YerBRfX3KhaHovgcw4Boc6LQ7Kuz7J/1OJ0PvMwF3y17ufq6V3WAXbzivTSCRgd1/53waPdrYiRAeAhTWVjL+8FvUbT1YlWSMYbXTdK8LZWm0WTMcNb9xuwIPMCgYEA6PxoETNRuJNaAKiVNBQr4p+goaUKC4m/a1iwff4Sk7B8eI/AsNWsowe9157QUOmdiVTwuIvkX8ymEsvgQxM7l5TVly6TuQNtf/oDMgj3h+23Wy50v4ErLTxYTnk4YGvAbhGEeRcxtVd3GP74avgID/pUiWyS8Ii052LR6l1PW8cCgYEAz987McFGQKdHvZI5QXiHKVtb5YzV2Go9EGYrWH0i2B8Nf6J2UmnhddWvhPyyT73dMd7NFaezUECTu5K0jjd75TfNMe/ULRVFnqvD9cQjg1yFn798+hRhJr9NPn5gftXViuKbzjuag+RFrJ/xupWO+3sAMcyPFvVkldAmAjLULm8CgYEAkDacW/k+HlfnH/05zbCmsXJJRYUYwKeU+uc859/6s7xMb3vbtBmu8IL8OZkuLMdOIhGXp0PAKqRML9pOiHZBLsSLqTbFbYH3p32juLbgMR0tn50T2u4jQa7WokxaXySTSg5Bx4pZ1Hu9VpWMQvogU3OKHD4+ffDAuXDrqnvzgUUCgYEAvoWI1az7E/LP59Fg6xPDSDnbl9PlQvHY8G7ppJXYzSvVWlk7Wm1VoTA4wFonD24okJ8jgRw6EBTRkM0Y8dg2dKvynJw3oUJdhmHL4mnb6bOhMbFU03cg9cm/YR1Vb/1eJXqrFYdnrMXx9T9udUT6OAKCkER+/uRv8gARRSzOYIE=";
    protected String realmPublicKey;
    protected String realmSigningCertificate;

    public static final String SAML_CLIENT_ID_SALES_POST = "http://localhost:8280/sales-post/";
    public static final String SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCay+v4QHb/lGOYCPrltH36W/newc3DvvsKHst6JGzM1yMiSuGM6UE7fuzWe6PHQ3IDuezz47iEGTAhRuyfcIQ9yt7iqsOSrEdXFx5xyoO6jt2Aty3FDLGbuLAR2HtYDe/sjyVqWZ0+KyRwuOqx5WcXVzpA6JRuTAZdXLDjBLqliOSFziVa6e0vml6EZrZyjHCvhjLASSxLmZ2Y/caXAggnQAeSOncCQWGT5Rlae68gMWUpGqq87bML0XjLUtHGVS6IUrD5mMaXeS8dvekqbs99ZA9YSlCH3ewMOiWamrv/HB+eENRt217vx/QOfzqXLF1md5vuahOdWA2bRSs0g+CFAgMBAAECggEADFHulfOLhQvqYU9VqLKU1Dg9ytzh45JvqH6K802y2xrEURZknTJzXqjbcLamclWL3YAOu9qD9P+MNRnP+2CZJdHfq2qr5iCQDP5qDeRwV4jHWFc77VL1T8+DM+hm6LszPUCnWa+INEp6u/37r+zyJ4lpuYDJT339g7C841SdBk+fGBXXTjsmhVjbG7uhLhj3QELFy43tnoP7BJCm0SV7T9PJk/LF5zCt1TP8gtQW2Dbh/Fo+zqRl7e7Kl7ivTmCXOC6sReBEBY4rUlitNs8+7JZ7c3oR7MBFT/Bg2I8xPgPxLqHcE9afLo4BJL6cVUG8qfrcwNIks/WsCEVC+xtgAQKBgQDXIWttO1/P0/lTbNYKrpWm/afjF4Z155QU7YKrxsTqmjQcj96bacMYBcWxrI6sR3DoEGdlwkK9m8n6MUuVP7udVlnFgegzwAUbuh5vj/wN7b+A52HE92osho6i/69R2lpf/qW0WbThV+w985S5bvgWRlZ+MPOFfNJnbCk2JnVjRQKBgQC4ND3XVZpfS/RCnxZRxEBXYH4108SeOMC/OEIBmKpxUJ/forv2l2KOXuq1FlaMc5AWEQhRG335P+VPKluziogEM0DyT89/tZQ6oK2hrXf4vCNgZ//WnbGvBWQC86dmWq+5/9ut7xwiP0KY6j2w3nukyJglzgIQLu7gZTrQ6gO8QQKBgCTI3BuTWXCp6CnxpC+zZndlS/2ymhUzAckKS5ufozASKfLwTrn4PZmv8vvMa6Ddnlqv414s90iAiOq03x2oBiaDC1qQOeoPXVA+ZNHiptCi5GngJoGNZKQ0ZdNNMOcoFOfxHNhXtmwJoqV9LuL+LIFyiXuOVzVuAHQatHlD3jKZAoGBALFuKM89dpgqmlp90MrsBankmU2R8UcSlZ7bOsE845iIt6Z7oyAwy88lYGET5kQdoIGQ4Hj3yU0IHgI2Y+Q6ITAiioGdHNr/9YrPcNBWPkSKPG1FX+rDNP7Ia4BoYCu4WKIJ8PnGY0wdsTGIF+pBM8oTKnLnz5b1DkV5XMEVWInBAoGAXr/RZ54nvWcvb/L995iy1uFaTooL5Rcb8XBIGaAeHbiIHy0AYH8lc7E2QW54dIvfaguWDDiebdfKHdjgdOrIYvlicGlCk9jFlsdzv/e28C37F67btX86XUzzM7uchU9kECLLM1igVvHPVQv3l3AIgEri2pcSOZlbE17zS+pKRlU=";
    public static final String SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmsvr+EB2/5RjmAj65bR9+lv53sHNw777Ch7LeiRszNcjIkrhjOlBO37s1nujx0NyA7ns8+O4hBkwIUbsn3CEPcre4qrDkqxHVxceccqDuo7dgLctxQyxm7iwEdh7WA3v7I8lalmdPiskcLjqseVnF1c6QOiUbkwGXVyw4wS6pYjkhc4lWuntL5pehGa2coxwr4YywEksS5mdmP3GlwIIJ0AHkjp3AkFhk+UZWnuvIDFlKRqqvO2zC9F4y1LRxlUuiFKw+ZjGl3kvHb3pKm7PfWQPWEpQh93sDDolmpq7/xwfnhDUbdte78f0Dn86lyxdZneb7moTnVgNm0UrNIPghQIDAQAB";
    protected PrivateKey samlClientSalesPostSigPrivateKeyPk;
    protected PublicKey samlClientSalesPostSigPublicKeyPk;

    @BeforeEach
    public void initCrypto() throws Exception {
        CryptoIntegration.init(this.getClass().getClassLoader());
        KeyFactory kfRsa = KeyFactory.getInstance("RSA");
        samlClientSalesPostSigPrivateKeyPk = kfRsa.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY)));
        samlClientSalesPostSigPublicKeyPk = kfRsa.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)));

        PrivateKey privateK = PemUtils.decodePrivateKey(REALM_PRIVATE_KEY);
        PublicKey publicK = KeyUtils.extractPublicKey(privateK);
        realmPublicKey = PemUtils.encodeKey(publicK);
        Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(new KeyPair(publicK, privateK), "demo");
        realmSigningCertificate = PemUtils.encodeCertificate(certificate);
    }


    // Set date to past (For example with "faketime" utility); then: openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 1 -nodes -subj '/CN=http:\/\/localhost:8080\/sales-post-sig\/'
    public static final String SAML_BROKER_ALIAS = "saml-broker";

    protected final AtomicReference<NameIDType> nameIdRef = new AtomicReference<>();
    protected final AtomicReference<String> sessionIndexRef = new AtomicReference<>();

    protected String getSamlAssertionConsumerUrl() {
        return keycloakUrls.getBaseUrl() + "/sales-post/saml";
    }

    protected AuthnRequestType createLoginRequestDocument(String issuer, String assertionConsumerURL, String realmName) throws URISyntaxException {
        return SamlClient.createLoginRequestDocument(issuer, assertionConsumerURL, getAuthServerSamlEndpoint(realmName));
    }

    protected URI getAuthServerSamlEndpoint(String realm) throws IllegalArgumentException, UriBuilderException, URISyntaxException {
        return RealmsResource
                .protocolUrl(UriBuilder.fromUri(keycloakUrls.getBaseUrl().toURI()))
                .build(realm, SamlProtocol.LOGIN_PROTOCOL);
    }

    protected URI getAuthServerBrokerSamlEndpoint(String realm, String identityProviderAlias) throws IllegalArgumentException, UriBuilderException, URISyntaxException {
        return RealmsResource
                .realmBaseUrl(UriBuilder.fromUri(keycloakUrls.getBaseUrl().toURI()))
                .path("broker/{idp-name}/endpoint")
                .build(realm, identityProviderAlias);
    }

    protected URI getAuthServerRealmBase(String realm) throws IllegalArgumentException, UriBuilderException, URISyntaxException {
        return RealmsResource
                .realmBaseUrl(UriBuilder.fromUri(keycloakUrls.getBaseUrl().toURI()))
                .build(realm);
    }

    protected URI getSamlBrokerUrl(String realmName) throws URISyntaxException {
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
