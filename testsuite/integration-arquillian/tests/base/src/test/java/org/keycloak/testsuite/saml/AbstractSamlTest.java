package org.keycloak.testsuite.saml;

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
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.util.SamlClient;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
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

    public static final String SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBANUbxrvEY3pkiQNt55zJLKBwN+zKmNQw08ThAmOKzwHfXoK+xlDSFxNMtTKJGkeUdnKzaTfESEcEfKYULUA41y/NnOlvjS0CEsc7Wq0Ce63TSSGMB2NHea4tV0aQz/MwLsbmz2IjAFWHA5CHL5WwacIf3UTOSNnhJUSvnkomjJAlAgMBAAECgYANpO2gb/5+g5lSIuNFYov86bJq8r2+ODIW1OE2Rljioc6HSHeiDRF1JuAjECwikRrUVTBTZbnK8jqY14neJsWAKBzGo+ToaQALsNZ9B91DxxL50K5oVOzw5shAS9TnRjN40+KIXFED4ydq4JRdoqb8+cN+N3i0+Cu7tdm+UaHTAQJBAOwFs3ZwqQEqmv9vmgmIFwFpJm1aIw25gEOf3Hy45GP4bL/j0FQgwcXYRbLE5bPqhw/liLKc1GQ97bVm6zs8SvUCQQDnJZA6TFRMiDjezinE1J4e0v4RupyDniVjbE5ArTK5/FRVkjw4Ny0AqZUEyIIqlTeZlCq45pCJy4a2hymDGVJxAj9gzfXNnmezEsZ//kYvoqHM8lPQhifaeTsigW7tuOf0GPCBw+6uksDnZM0xhZCxOoArBPoMSEbU1pGo1Y2lvhUCQF6E5sBgHAybm53Ich4Rz4LNRqWbSIstrR5F2I3sBRU2kInZXZSjQ1zE+7HUCB4/nFfJ1dp8NdiTCEg1Zw072pECQQDnxyQALmWhQbBTl0tq6CwYf9rZDwBzxuY+CXB8Ky1gOmXwan96KZvV4rK8MQQs6HIiYC/j+5lX3A3zlXTFldaz";
    public static final String SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDVG8a7xGN6ZIkDbeecySygcDfsypjUMNPE4QJjis8B316CvsZQ0hcTTLUyiRpHlHZys2k3xEhHBHymFC1AONcvzZzpb40tAhLHO1qtAnut00khjAdjR3muLVdGkM/zMC7G5s9iIwBVhwOQhy+VsGnCH91EzkjZ4SVEr55KJoyQJQIDAQAB";
    public static final PrivateKey SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY_PK;
    public static final PublicKey SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY_PK;

    static {
        try {
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
    public static final String SAML_CLIENT_SALES_POST_SIG_EXPIRED_PRIVATE_KEY = "MIIEpQIBAAKCAQEA3SMEGYw330CS++XP0KqoFz2UezUxZAhkLv5C93hf5FPGw9QpPmimpGcsN8RCy4DDYOGrbuJLd8GkoBCkmp7xTqQMx/nrUvzDCAWAUSnxnBVgCsq9KbpI5sdacOHd0oEI9pQdRQ71Rj+tipeIt+Fy8S17bkpGBYjQk3xdusMX8E9LR04ksp0C9o2mvX+U0QCrF8HqVCCO9gMJJNOGaot7a3+QaTWnNrPguhMuHgJ6LlsyOUYNFQw5rdxs8Vz2mOsIGvWn1Em/c+KCcMltTIhOhDY3zW3ZrFL3Vwq4kTQ74ju9Qp1qyyQOOJmig6LLm31LQvQHPQWkY7rRcp9VBMRPcQIDAQABAoIBAQDPUpvuY9KiIYVsWvoqFUWAfIBvvuAue9uJX2JjZ1zn0U+Bm7CLTUwmyH/hTMSezHrgotK6I7lDbq4sT04zlJ6B7zX4aqwg4s7q/1VdQui9QCEKHSeaLodYrkBxoqD4UXeYziZe73YvRVYroIRSeTDtQon9Te82Ex4RmEC771rLNZ38rm2EsF2+GfNIavumo458TBmX0DI8w3QwlSMEeXaNZqch2adZSDxehrOFeqzZ9o8KtgCfrJ5P11vgXlKnVGFa7Pfndrc6XacfYhKAtTyX3Bgx9FFaOK+W5k5/XXc2UTbUV6aNmiQdNp5CrjoZ/DuttWFGwOWfg9zSG3i5wwLRAoGBAPR7IWPk1Ejf8+4vGvDED3ZDc94DINrFjszaVZBt2w/Hx0uePdeojulHhTBFMFUtV2Dn8vpG7D9TxDeZj7tmKSHE3/j1DXE6jpo72Z+iOR5byO/HmgiV0kblKxXnZfDy5/cq/Cy6GTJ2MU6k50SDgIIq86gWCXbRwveX9E66qlHdAoGBAOeOUEiuGC332m7N2wfUobBbczNviSWeAzIFP4t15u0QHRhMDeRmfE4xuWS4aL1vfsyTOrxaN5GJ2QeAIdkM42dSA0FqzzumRd9T8VdeJ+J2GGB+ALNmTHNuz8jWepLVD2F1GBhs+gkSh5yS1p+FUodQWkWC5YLI/y2rySpbiPylAoGBAJpV0LJbFpgaqMbH/d3YJ1qlIlQY7XiuFoPDoRhYAV5o46sc7jViNzWU7MOYKfbbdLm8M2tDsogXvVrMGixXRcgHnMxxBldge/1pouxfYGeF0cds3hRlYCVZLmXZekUtUrp57E/f+2AbtOzMtSJPUaTasI5/uuHDca0TxCqfND4RAoGAWS6Fm0h6BZJVLaHZPw3U7FB8cQ3/G17dSjGdRMA3HYy8N/Rq0VHrhE5AYhtoM7Wyd2YpFAwHJOWbkfj2kFsXZl6+5D4X7JhghuAUrpqT7/Od9ePxryayQS8nlemNMeofT2DC0/1822uokVQ4lx3JKFZ5PhZpANMa/OMRyl+QxgUCgYEA4D5YyD5wHz7fNFyaUrgJr4dFLG9vqRv8Pm9IozBAmNumi25Gi7gyi/WN8DrVbsRiq4ywiKiikui5TW3/RR51OYDnX3YCnWE5AGV4okci3PlclJ/UsPjlUOzNlXW7Wr0pFCcJc/WuQm1lgho/o6QGbMbS/BSwxBrUl/bUEp4IZKc=";
    public static final String SAML_CLIENT_SALES_POST_SIG_EXPIRED_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3SMEGYw330CS++XP0KqoFz2UezUxZAhkLv5C93hf5FPGw9QpPmimpGcsN8RCy4DDYOGrbuJLd8GkoBCkmp7xTqQMx/nrUvzDCAWAUSnxnBVgCsq9KbpI5sdacOHd0oEI9pQdRQ71Rj+tipeIt+Fy8S17bkpGBYjQk3xdusMX8E9LR04ksp0C9o2mvX+U0QCrF8HqVCCO9gMJJNOGaot7a3+QaTWnNrPguhMuHgJ6LlsyOUYNFQw5rdxs8Vz2mOsIGvWn1Em/c+KCcMltTIhOhDY3zW3ZrFL3Vwq4kTQ74ju9Qp1qyyQOOJmig6LLm31LQvQHPQWkY7rRcp9VBMRPcQIDAQAB";
    public static final String SAML_CLIENT_SALES_POST_SIG_EXPIRED_CERTIFICATE = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDQTCCAimgAwIBAgIUT8qwq3DECizGLB2tQAaaNSGAVLgwDQYJKoZIhvcNAQEL\n" +
            "BQAwMDEuMCwGA1UEAwwlaHR0cDovL2xvY2FsaG9zdDo4MDgwL3NhbGVzLXBvc3Qt\n" +
            "c2lnLzAeFw0yMzAxMjcxNjAwMDBaFw0yMzAxMjgxNjAwMDBaMDAxLjAsBgNVBAMM\n" +
            "JWh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9zYWxlcy1wb3N0LXNpZy8wggEiMA0GCSqG\n" +
            "SIb3DQEBAQUAA4IBDwAwggEKAoIBAQDdIwQZjDffQJL75c/QqqgXPZR7NTFkCGQu\n" +
            "/kL3eF/kU8bD1Ck+aKakZyw3xELLgMNg4atu4kt3waSgEKSanvFOpAzH+etS/MMI\n" +
            "BYBRKfGcFWAKyr0pukjmx1pw4d3SgQj2lB1FDvVGP62Kl4i34XLxLXtuSkYFiNCT\n" +
            "fF26wxfwT0tHTiSynQL2jaa9f5TRAKsXwepUII72Awkk04Zqi3trf5BpNac2s+C6\n" +
            "Ey4eAnouWzI5Rg0VDDmt3GzxXPaY6wga9afUSb9z4oJwyW1MiE6ENjfNbdmsUvdX\n" +
            "CriRNDviO71CnWrLJA44maKDosubfUtC9Ac9BaRjutFyn1UExE9xAgMBAAGjUzBR\n" +
            "MB0GA1UdDgQWBBR4R5i1kWMxzzdQ3TdgI/MuNLChSDAfBgNVHSMEGDAWgBR4R5i1\n" +
            "kWMxzzdQ3TdgI/MuNLChSDAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUA\n" +
            "A4IBAQAacI/f9YFVTUCGXfh/FCVBQI20bgOs9D6IpIhN8L5kEnY6Ox5t00b9G5Bz\n" +
            "64alK3WMR3DdhTEpufX8IMFpMlme/JnnOQXkfmIvzbev4iIKxcKFvS8qNXav8PVx\n" +
            "wDApuzgxEq/XZCtFXhDS3q1jGRmlOr+MtQdCNQuJmxy7kOoFPY+UYjhSXTZVrCyF\n" +
            "I0LYJQfcZ69bYXd+5h1U3UsN4ZvsBgnrz/IhhadaCtTZVtvyr/uzHiJpqT99VO9/\n" +
            "7lwh2zL8ihPyOUVDjdYxYyCi+BHLRB+udnVAfo7t3fbxMi1gV9xVcYaqTJgSArsY\n" +
            "M8mxv8p5mhTa8TJknzs4V3Dm+PHs\n" +
            "-----END CERTIFICATE-----";
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
