package org.keycloak.testsuite.saml;

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.adapter.page.SAMLServlet;
import org.keycloak.testsuite.util.SamlClient;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import java.net.URI;
import java.util.List;

import static org.keycloak.testsuite.util.IOUtil.loadRealm;

/**
 * @author mhajas
 */
public class AbstractSamlTest extends AbstractAuthTest {

    protected static final String REALM_NAME = "demo";

    protected static final String SAML_ASSERTION_CONSUMER_URL_SALES_POST = "http://localhost:8080/sales-post/";
    protected static final String SAML_CLIENT_ID_SALES_POST = "http://localhost:8081/sales-post/";

    protected static final String SAML_ASSERTION_CONSUMER_URL_SALES_POST_ENC = "http://localhost:8080/sales-post-enc/";
    protected static final String SAML_CLIENT_ID_SALES_POST_ENC = "http://localhost:8081/sales-post-enc/";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/adapter-test/keycloak-saml/testsaml.json"));
    }

    protected AuthnRequestType createLoginRequestDocument(String issuer, String assertionConsumerURL, String realmName) {
        return SamlClient.createLoginRequestDocument(issuer, assertionConsumerURL, getAuthServerSamlEndpoint(realmName));
    }

    protected URI getAuthServerSamlEndpoint(String realm) throws IllegalArgumentException, UriBuilderException {
        return RealmsResource
                .protocolUrl(UriBuilder.fromUri(getAuthServerRoot()))
                .build(realm, SamlProtocol.LOGIN_PROTOCOL);
    }
}
