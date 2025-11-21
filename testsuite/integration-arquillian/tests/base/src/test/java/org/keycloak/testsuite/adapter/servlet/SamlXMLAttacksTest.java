package org.keycloak.testsuite.adapter.servlet;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.processing.web.util.PostBindingUtil;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.Matcher;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

import static org.keycloak.testsuite.util.Matchers.bodyHC;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP8)
public class SamlXMLAttacksTest extends AbstractSamlTest {

    @Test
    public void testXMLBombAttackResistance() throws Exception {
        runTestWithTimeout(4000, () -> {
            String bombDoctype = "<!DOCTYPE AuthnRequest [" +
            " <!ENTITY lol \"lol\">" +
                    "<!ELEMENT AuthnRequest (#PCDATA)>" +
                    "<!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">" +
                    "<!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">" +
                    "<!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">" +
                    "<!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">" +
                    "<!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">" +
                    "<!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">" +
                    "<!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">" +
                    "<!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">" +
                    "<!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\">" +
                    "]>";

            String samlAuthnRequest = "<samlp:AuthnRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"a123\" Version=\"2.0\" IssueInstant=\"2014-07-16T23:52:45Z\" >" +
                    "<saml:Issuer>" + SAML_CLIENT_ID_SALES_POST + "&lol9;</saml:Issuer>" +
                    "</samlp:AuthnRequest>";

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                HttpPost post = new HttpPost(getAuthServerSamlEndpoint(REALM_NAME));

                List<NameValuePair> parameters = new LinkedList<>();
                String encoded = PostBindingUtil.base64Encode(bombDoctype + samlAuthnRequest);
                parameters.add(new BasicNameValuePair(GeneralConstants.SAML_REQUEST_KEY, encoded));

                UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);

                post.setEntity(formEntity);

                try (CloseableHttpResponse response = client.execute(post)) {
                    assertThat(response, bodyHC(containsString("Invalid Request")));
                }
            }
        });
    }

    @Deployment(name = "DTD")
    protected static WebArchive employee() {
        String attackerDtd = "<!ENTITY % file SYSTEM \"file:///etc/passwd\">\n" +
                     "<!ENTITY % eval \"<!ENTITY &#x25; error SYSTEM 'file:///nonexistent/%file;'>\">\n" +
                     "%eval;\n" +
                     "%error;";

        return ShrinkWrap.create(WebArchive.class, "dtd.war")
          .add(new StringAsset(attackerDtd), "/attacker.dtd");
    }

    private void assertBlackboxInputValidation(String s, Matcher<? super CloseableHttpResponse> matcher) throws IOException, RuntimeException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(getAuthServerSamlEndpoint(REALM_NAME));

            List<NameValuePair> parameters = new LinkedList<>();
            String encoded = PostBindingUtil.base64Encode(s);
            parameters.add(new BasicNameValuePair(GeneralConstants.SAML_REQUEST_KEY, encoded));

            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
            post.setEntity(formEntity);

            try (CloseableHttpResponse response = client.execute(post)) {
                assertThat(response, matcher);
            }
        }
    }

    @Test
    public void testValidator(@ArquillianResource @OperateOnDeployment("DTD") URL attackerDtdUrl) throws Exception {
        String preamble = "<!DOCTYPE AuthnRequest [" +
                "<!ELEMENT AuthnRequest (#PCDATA)>" +
                "<!ENTITY % sp SYSTEM \"" + attackerDtdUrl + "/attacker.dtd\" >%sp;" +
                "<!ENTITY heh SYSTEM \"file:///etc/passwd\">" +
                "]>".replaceAll("//attacker", "/attacker");

        assertBlackboxInputValidation(REQUEST_VALID, statusCodeIsHC(Response.Status.FOUND));

        assertBlackboxInputValidation(REQUEST_INVALID, bodyHC(containsString("Invalid Request")));
        assertBlackboxInputValidation(preamble + REQUEST_VALID, bodyHC(containsString("Invalid Request")));
        assertBlackboxInputValidation(preamble + REQUEST_FLAWED, bodyHC(containsString("Invalid Request")));
        assertBlackboxInputValidation(preamble + REQUEST_FLAWED_LOCAL, bodyHC(containsString("Invalid Request")));
        assertBlackboxInputValidation(preamble + "<AuthnRequest></AuthnRequest>", bodyHC(containsString("Invalid Request")));
    }

    private static final String REQUEST_VALID = "<samlp:AuthnRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"a123\" Version=\"2.0\" IssueInstant=\"2014-07-16T23:52:45Z\" >" +
            "<saml:Issuer>" + SAML_CLIENT_ID_SALES_POST + "</saml:Issuer>" +
            "</samlp:AuthnRequest>";

    private static final String REQUEST_FLAWED = "<samlp:AuthnRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"&sp;\" Version=\"2.0\" IssueInstant=\"2014-07-16T23:52:45Z\" >" +
            "<saml:Issuer>" + SAML_CLIENT_ID_SALES_POST + "</saml:Issuer>" +
            "</samlp:AuthnRequest>";

    private static final String REQUEST_FLAWED_LOCAL = "<samlp:AuthnRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"&heh;\" Version=\"2.0\" IssueInstant=\"2014-07-16T23:52:45Z\" >" +
            "<saml:Issuer>" + SAML_CLIENT_ID_SALES_POST + "</saml:Issuer>" +
            "</samlp:AuthnRequest>";

    private static final String REQUEST_INVALID = "<samlp:InvalidAuthnRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"a123\" Version=\"2.0\" IssueInstant=\"2014-07-16T23:52:45Z\" >" +
            "<saml:Issuer>" + SAML_CLIENT_ID_SALES_POST + "</saml:Issuer>" +
            "</samlp:AuthnRequest>";

}
