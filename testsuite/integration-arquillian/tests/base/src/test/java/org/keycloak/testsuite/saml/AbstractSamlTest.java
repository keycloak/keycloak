package org.keycloak.testsuite.saml;

import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.util.SamlClient;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import java.net.URI;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.Matchers.isSamlResponse;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_PORT;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SCHEME;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;

/**
 * @author mhajas
 */
public abstract class AbstractSamlTest extends AbstractAuthTest {

    public static final String REALM_NAME = "demo";
    public static final String REALM_PRIVATE_KEY = "MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=";
    public static final String REALM_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";
    public static final String REALM_SIGNING_CERTIFICATE = "MIIBkTCB+wIGAUkZB1wLMA0GCSqGSIb3DQEBCwUAMA8xDTALBgNVBAMTBGRlbW8wHhcNMTQxMDE2MTI1NDEzWhcNMjQxMDE2MTI1NTUzWjAPMQ0wCwYDVQQDEwRkZW1vMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABMA0GCSqGSIb3DQEBCwUAA4GBAI9moVwZxiEvzfvyL0zqyzRP4qnEdYQ/l/Nl78OAed25hdKpVpNv8i7DwM1QscWQhrtfGImD0480eoOUfe1rU9k6gNdNpR6kYAz17A/OsovpTFF0cIQE7HPqumpHfdbeW0jEjLNT2Od/PXdaIijVOdbJn8iF//nnItrwPbNUBU75";

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

    public static final String SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBANUbxrvEY3pkiQNt55zJLKBwN+zKmNQw08ThAmOKzwHfXoK+xlDSFxNMtTKJGkeUdnKzaTfESEcEfKYULUA41y/NnOlvjS0CEsc7Wq0Ce63TSSGMB2NHea4tV0aQz/MwLsbmz2IjAFWHA5CHL5WwacIf3UTOSNnhJUSvnkomjJAlAgMBAAECgYANpO2gb/5+g5lSIuNFYov86bJq8r2+ODIW1OE2Rljioc6HSHeiDRF1JuAjECwikRrUVTBTZbnK8jqY14neJsWAKBzGo+ToaQALsNZ9B91DxxL50K5oVOzw5shAS9TnRjN40+KIXFED4ydq4JRdoqb8+cN+N3i0+Cu7tdm+UaHTAQJBAOwFs3ZwqQEqmv9vmgmIFwFpJm1aIw25gEOf3Hy45GP4bL/j0FQgwcXYRbLE5bPqhw/liLKc1GQ97bVm6zs8SvUCQQDnJZA6TFRMiDjezinE1J4e0v4RupyDniVjbE5ArTK5/FRVkjw4Ny0AqZUEyIIqlTeZlCq45pCJy4a2hymDGVJxAj9gzfXNnmezEsZ//kYvoqHM8lPQhifaeTsigW7tuOf0GPCBw+6uksDnZM0xhZCxOoArBPoMSEbU1pGo1Y2lvhUCQF6E5sBgHAybm53Ich4Rz4LNRqWbSIstrR5F2I3sBRU2kInZXZSjQ1zE+7HUCB4/nFfJ1dp8NdiTCEg1Zw072pECQQDnxyQALmWhQbBTl0tq6CwYf9rZDwBzxuY+CXB8Ky1gOmXwan96KZvV4rK8MQQs6HIiYC/j+5lX3A3zlXTFldaz";
    public static final String SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDVG8a7xGN6ZIkDbeecySygcDfsypjUMNPE4QJjis8B316CvsZQ0hcTTLUyiRpHlHZys2k3xEhHBHymFC1AONcvzZzpb40tAhLHO1qtAnut00khjAdjR3muLVdGkM/zMC7G5s9iIwBVhwOQhy+VsGnCH91EzkjZ4SVEr55KJoyQJQIDAQAB";
    public static final PrivateKey SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY_PK;
    public static final PublicKey SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY_PK;

    static {
        try {
            KeyFactory kfRsa = KeyFactory.getInstance("RSA");
            SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY_PK = kfRsa.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY)));
            SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY_PK = kfRsa.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    // Set date to past; then: openssl req -x509 -newkey rsa:1024 -keyout key.pem -out cert.pem -days 1 -nodes -subj '/CN=http:\/\/localhost:8080\/sales-post-sig\/'
    public static final String SAML_CLIENT_SALES_POST_SIG_EXPIRED_PRIVATE_KEY = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAMrGzRp3HVf6Ti75rl5mPAPXua8APCCLANikzOd82VI0R8Ml0UAchkfRUBvBedobJIn9r8wwxMeXLmKsMynW52SYeC/Zx5b5K6ayMS3GWJIgqLpp/n1piUeI4sbJXlUj9UtW+QTpGhrHt9n7s7znwoNqGDUkjmyZiekEspjdfzzlAgMBAAECgYBJvPFo5lftXkCAJJucCGFapGAJm3RCAUpVfdhldakxk4FlHaNyRO0vwJX5AeplvekTpQUAo9trGTbs+uHAHT4XWOnwhHHyBRkWdiwXX9bzNdHnIwf/0SLIBBYUk0hoWEDvpklBPqllM215a0sEnB2ykYSsMDBSkFB7Ah+RK7zTAQJBAOw9v7SsfIhOXci9vnkQPuQpL8T4kwj7nWi+YtRGrXbF/bJGwjsgXN5i7otwBV/W+TNzI5H7s2opPUXdIxfP9C0CQQDbvIcxXjwjO1hjXXY4axiT1sxU8Oq1bds033atMoN9pib7IxkWh6ouOQZT8bxwQ2ElH0rswZ0/2CusrIUIekaZAkEAk9UUSQiDKXz4vSzXq8SZxodriDQRNtbVqv0wtSvBUwkU9+HFm+BlnRiFtCYWhuHsseCESs8ad/10hWqbkkQkxQJAZOvN2+rADB5xlhGS/o6RlzUMW+bapcFy8HHB/AI7SjZJqQaRuztL+jbOpTddqOIJeBdLPjoekvgh9wi1gRNH4QJBAMjfB1xYxmztfbUcUuOsATz3s7StprOAukd+hhBiMukxcKhi1IQp7tFhfFe/+xUY3fSh1a3KlyItFKxp68EdDRk=";
    public static final String SAML_CLIENT_SALES_POST_SIG_EXPIRED_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDKxs0adx1X+k4u+a5eZjwD17mvADwgiwDYpMznfNlSNEfDJdFAHIZH0VAbwXnaGySJ/a/MMMTHly5irDMp1udkmHgv2ceW+SumsjEtxliSIKi6af59aYlHiOLGyV5VI/VLVvkE6Roax7fZ+7O858KDahg1JI5smYnpBLKY3X885QIDAQAB";
    public static final String SAML_CLIENT_SALES_POST_SIG_EXPIRED_CERTIFICATE = "MIICMTCCAZqgAwIBAgIJAPlizW20Nhe6MA0GCSqGSIb3DQEBCwUAMDAxLjAsBgNVBAMMJWh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9zYWxlcy1wb3N0LXNpZy8wHhcNMTYwODI5MDg1MjMzWhcNMTYwODMwMDg1MjMzWjAwMS4wLAYDVQQDDCVodHRwOi8vbG9jYWxob3N0OjgwODAvc2FsZXMtcG9zdC1zaWcvMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDKxs0adx1X+k4u+a5eZjwD17mvADwgiwDYpMznfNlSNEfDJdFAHIZH0VAbwXnaGySJ/a/MMMTHly5irDMp1udkmHgv2ceW+SumsjEtxliSIKi6af59aYlHiOLGyV5VI/VLVvkE6Roax7fZ+7O858KDahg1JI5smYnpBLKY3X885QIDAQABo1MwUTAdBgNVHQ4EFgQUE9C6Ck0jsdY+sjN064ZYwYkZJr4wHwYDVR0jBBgwFoAUE9C6Ck0jsdY+sjN064ZYwYkZJr4wDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOBgQBuypHw5DMDBgfI6LcXBiCjpiQP3DLRLdwthh/RfCnZT7PrhXRJV8RMm8EqxqtEgfg2SKqMyA02uxMKH0p277U2iQveSDAaICTJRxtyFm6FERtgLNlsekusC2I14gZpLe84oHDf6L1w3dKFzzLEC9+bHg/XCg/KthWxW8iuVct5qg==";

    public static final String SAML_ASSERTION_CONSUMER_URL_SALES_POST_ENC =  AUTH_SERVER_SCHEME + "://localhost:" + (AUTH_SERVER_SSL_REQUIRED ? AUTH_SERVER_PORT : 8080) + "/sales-post-enc/saml";
    public static final String SAML_CLIENT_ID_SALES_POST_ENC = "http://localhost:8280/sales-post-enc/";
    public static final String SAML_CLIENT_SALES_POST_ENC_PRIVATE_KEY = "MIICXQIBAAKBgQDb7kwJPkGdU34hicplwfp6/WmNcaLh94TSc7Jyr9Undp5pkyLgb0DE7EIE+6kSs4LsqCb8HDkB0nLD5DXbBJFd8n0WGoKstelvtg6FtVJMnwN7k7yZbfkPECWH9zF70VeOo9vbzrApNRnct8ZhH5fbflRB4JMA9L9R+LbURdoSKQIDAQABAoGBANtbZG9bruoSGp2s5zhzLzd4hczT6Jfk3o9hYjzNb5Z60ymN3Z1omXtQAdEiiNHkRdNxK+EM7TcKBfmoJqcaeTkW8cksVEAW23ip8W9/XsLqmbU2mRrJiKa+KQNDSHqJi1VGyimi4DDApcaqRZcaKDFXg2KDr/Qt5JFD/o9IIIPZAkEA+ZENdBIlpbUfkJh6Ln+bUTss/FZ1FsrcPZWu13rChRMrsmXsfzu9kZUWdUeQ2Dj5AoW2Q7L/cqdGXS7Mm5XhcwJBAOGZq9axJY5YhKrsksvYRLhQbStmGu5LG75suF+rc/44sFq+aQM7+oeRr4VY88Mvz7mk4esdfnk7ae+cCazqJvMCQQCx1L1cZw3yfRSn6S6u8XjQMjWE/WpjulujeoRiwPPY9WcesOgLZZtYIH8nRL6ehEJTnMnahbLmlPFbttxPRUanAkA11MtSIVcKzkhp2KV2ipZrPJWwI18NuVJXb+3WtjypTrGWFZVNNkSjkLnHIeCYlJIGhDd8OL9zAiBXEm6kmgLNAkBWAg0tK2hCjvzsaA505gWQb4X56uKWdb0IzN+fOLB3Qt7+fLqbVQNQoNGzqey6B4MoS1fUKAStqdGTFYPG/+9t";
    public static final String SAML_CLIENT_SALES_POST_ENC_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDb7kwJPkGdU34hicplwfp6/WmNcaLh94TSc7Jyr9Undp5pkyLgb0DE7EIE+6kSs4LsqCb8HDkB0nLD5DXbBJFd8n0WGoKstelvtg6FtVJMnwN7k7yZbfkPECWH9zF70VeOo9vbzrApNRnct8ZhH5fbflRB4JMA9L9R+LbURdoSKQIDAQAB";

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
