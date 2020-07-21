package org.keycloak.testsuite.saml;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Test;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.processing.web.util.PostBindingUtil;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.Matchers.bodyHC;

public class SamlXMLAttacksTest extends AbstractSamlTest {

    @Test(timeout = 4000)
    public void testXMLBombAttackResistance() throws Exception {

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

            UrlEncodedFormEntity formEntity;
            try {
                formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            post.setEntity(formEntity);
            
            try (CloseableHttpResponse response = client.execute(post)) {
                assertThat(response, bodyHC(containsString("Invalid Request")));
            }
        }
    }

}
