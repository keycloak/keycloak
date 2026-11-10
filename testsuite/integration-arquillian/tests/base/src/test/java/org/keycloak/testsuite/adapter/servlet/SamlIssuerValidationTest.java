/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.adapter.servlet;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RoleBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.adapter.AbstractAdapterTest;
import org.keycloak.testsuite.adapter.page.SalesPostSigServlet;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.updaters.Creator;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.ASSERTION_NSURI;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.samlServletDeployment;
import static org.keycloak.testsuite.saml.AbstractSamlTest.REALM_NAME;
import static org.keycloak.testsuite.saml.AbstractSamlTest.REALM_PRIVATE_KEY;
import static org.keycloak.testsuite.saml.AbstractSamlTest.REALM_PUBLIC_KEY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Tests that the SAML adapter rejects assertions and responses whose issuer
 * does not match the configured IdP entity ID.
 */
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP8)
public class SamlIssuerValidationTest extends AbstractAdapterTest {

    private static final String ATTACKER_ISSUER = "https://attacker-tenant.idp.example";
    private static final String APP_CLIENT_ID = "http://localhost:8280/sales-post-sig/";
    private static final String REQUIRED_ROLE_NAME = "manager";
    private static final RoleRepresentation REQUIRED_ROLE = RoleBuilder.create().name(REQUIRED_ROLE_NAME).build();

    @Page
    private SalesPostSigServlet salesPostSigPage;

    private UserRepresentation user;
    private final AtomicReference<NameIDType> nameIdRef = new AtomicReference<>();
    private final AtomicReference<String> sessionIndexRef = new AtomicReference<>();

    @Deployment(name = SalesPostSigServlet.DEPLOYMENT_NAME)
    protected static WebArchive salesPostSig() {
        return samlServletDeployment(SalesPostSigServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return false;
    }

    private static ClientBuilder signingSamlClient(String clientId) {
        return ClientBuilder.create()
          .protocol(SamlProtocol.LOGIN_PROTOCOL)
          .enabled(true)
          .attribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false")
          .attribute(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, "username")
          .attribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true")
          .attribute(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM, "RSA_SHA256")
          .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, "true")
          .clientId(clientId);
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        final ClientBuilder salesPostClient = signingSamlClient(APP_CLIENT_ID)
          .baseUrl("http://localhost:8080/sales-post-sig")
          .redirectUris("http://localhost:8080/sales-post-sig/*");

        testRealms.add(RealmBuilder.create()
          .name(REALM_NAME)
          .publicKey(REALM_PUBLIC_KEY)
          .privateKey(REALM_PRIVATE_KEY)
          .clients(salesPostClient)
          .realmRoles(REQUIRED_ROLE)
          .build()
        );
    }

    @Before
    public void addFreshUserToDemoRealm() {
        this.user = UserBuilder.update(createUserRepresentation(("U-" + UUID.randomUUID().toString()).toLowerCase(), "a@b.c", "A", "B", true))
          .password("password")
          .build();

        Creator<UserResource> c = Creator.create(adminClient.realm(REALM_NAME), user);
        getCleanup(REALM_NAME).addCleanup(c);
        List<RoleRepresentation> reqRoleToJoin = c.resource().roles().realmLevel().listAvailable().stream().filter(r -> r.getName().equals(REQUIRED_ROLE_NAME)).collect(Collectors.toList());
        c.resource().roles().realmLevel().add(reqRoleToJoin);
    }

    // ---- test methods ----

    @Test
    public void testCorrectIssuerIsAccepted() {
        testIssuerModification(d -> {}, true);
    }

    @Test
    public void testWrongAssertionIssuerIsRejected() {
        testIssuerModification(d -> changeAssertionIssuer(d, ATTACKER_ISSUER), false);
    }

    @Test
    public void testWrongResponseIssuerIsRejected() {
        testIssuerModification(d -> changeRootIssuer(d, ATTACKER_ISSUER), false);
    }

    @Test
    public void testWrongBothIssuersRejected() {
        testIssuerModification(d -> {
            changeRootIssuer(d, ATTACKER_ISSUER);
            changeAssertionIssuer(d, ATTACKER_ISSUER);
        }, false);
    }

    @Test
    public void testNullAssertionIssuerIsRejected() {
        testIssuerModification(SamlIssuerValidationTest::removeAssertionIssuer, false);
    }

    @Test
    public void testNullResponseIssuerWithCorrectAssertionIssuerIsAccepted() {
        testIssuerModification(SamlIssuerValidationTest::removeRootIssuer, true);
    }

    @Test
    public void testWrongAssertionIssuerWithNullResponseIssuerIsRejected() {
        testIssuerModification(d -> {
            removeRootIssuer(d);
            changeAssertionIssuer(d, ATTACKER_ISSUER);
        }, false);
    }

    // ---- logout issuer tests ----

    @Test
    public void testCorrectLogoutIssuerIsAccepted() {
        testLogoutIssuer(req -> {}, true);
    }

    @Test
    public void testWrongLogoutIssuerIsRejected() {
        testLogoutIssuer(d -> changeRootIssuer(d, ATTACKER_ISSUER), false);
    }

    @Test
    public void testNullLogoutIssuerIsRejected() {
        testLogoutIssuer(SamlIssuerValidationTest::removeRootIssuer, false);
    }

    // ---- helpers ----

    private void testIssuerModification(Consumer<Document> modifier, boolean shouldPass) {
        Consumer<CloseableHttpResponse> assertions = shouldPass
                ? this::assertCorrectUserLoggedIn
                : SamlIssuerValidationTest::assertUserAccessDenied;

        new SamlClientBuilder()
          .navigateTo(salesPostSigPage)
          .processSamlResponse(Binding.POST).build()
          .login().user(user).build()
          .processSamlResponse(Binding.POST)
            .transformDocument(modifier)
            .signWith(REALM_PRIVATE_KEY, REALM_PUBLIC_KEY)
            .build()
          .executeAndTransform(r -> { assertions.accept(r); return null; });
    }

    private void assertCorrectUserLoggedIn(CloseableHttpResponse response) {
        assertThat(response, Matchers.statusCodeHC(is(Status.OK.getStatusCode())));
        assertThat(response, Matchers.bodyHC(containsString(user.getUsername())));
    }

    private static void assertUserAccessDenied(CloseableHttpResponse response) {
        // SendUsernameServlet renders the original error status in a Response.ok() page.
        assertThat(response, Matchers.bodyHC(allOf(
                containsString("HTTP status code: 403"),
                containsString("EXTRACTION_FAILURE"))));
    }

    private void testLogoutIssuer(Consumer<Document> modifier, boolean shouldPass) {
        URI clientSamlEndpoint = salesPostSigPage.getUriBuilder().clone().path("saml").build();
        String correctIssuer = getAuthServerRoot() + "realms/" + REALM_NAME;

        SamlClientBuilder builder = new SamlClientBuilder()
          // Step 1: Login to establish a session
          .navigateTo(salesPostSigPage)
          .processSamlResponse(Binding.POST).build()
          .login().user(user).build()
          .processSamlResponse(Binding.POST)
            .targetAttributeSamlResponse()
            .transformObject(this::extractNameIdAndSessionIndex)
            .signWith(REALM_PRIVATE_KEY, REALM_PUBLIC_KEY)
            .build()
          .assertResponse(this::assertCorrectUserLoggedIn)
          // Step 2: Send logout request to the SP with the given issuer
          .logoutRequest(clientSamlEndpoint, correctIssuer, Binding.POST)
            .nameId(nameIdRef::get)
            .sessionIndex(sessionIndexRef::get)
            .signWith(REALM_PRIVATE_KEY, REALM_PUBLIC_KEY)
            // Modify the serialized request so a missing issuer reaches the adapter.
            .transformDocument(modifier)
            .build()
          .doNotFollowRedirects()
          .assertResponse(shouldPass
              ? SamlIssuerValidationTest::assertLogoutSuccess
              : SamlIssuerValidationTest::assertLogoutRejected);

        if (!shouldPass) {
            // A rejected logout must leave the existing application session intact.
            builder.navigateTo(salesPostSigPage)
              .doNotFollowRedirects()
              .assertResponse(this::assertCorrectUserLoggedIn);
        }
        builder.execute();
    }

    private SAML2Object extractNameIdAndSessionIndex(SAML2Object so) {
        assertThat(so, Matchers.isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        ResponseType loginResp = (ResponseType) so;
        AssertionType assertion = loginResp.getAssertions().get(0).getAssertion();
        assertThat(assertion.getSubject().getSubType().getBaseID(), instanceOf(NameIDType.class));
        nameIdRef.set((NameIDType) assertion.getSubject().getSubType().getBaseID());
        sessionIndexRef.set(((AuthnStatementType) assertion.getStatements().iterator().next()).getSessionIndex());
        return so;
    }

    private static void assertLogoutSuccess(CloseableHttpResponse response) {
        try {
            assertThat(Binding.POST.extractResponse(response).getSamlObject(),
                    Matchers.isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void assertLogoutRejected(HttpResponse response) {
        // Terminal failures return no challenge or SAML LogoutResponse.
        assertThat(response, Matchers.statusCodeIsHC(Status.OK));
        assertThat(response, Matchers.bodyHC(is("")));
    }

    // ---- DOM manipulation helpers ----

    private static void changeAssertionIssuer(Document doc, String newIssuer) {
        Element assertion = (Element) doc.getElementsByTagNameNS(ASSERTION_NSURI.get(), "Assertion").item(0);
        NodeList issuers = assertion.getElementsByTagNameNS(ASSERTION_NSURI.get(), "Issuer");
        if (issuers.getLength() > 0) {
            issuers.item(0).setTextContent(newIssuer);
        }
    }

    private static void changeRootIssuer(Document doc, String newIssuer) {
        Element response = doc.getDocumentElement();
        NodeList children = response.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && "Issuer".equals(child.getLocalName())
                    && ASSERTION_NSURI.get().equals(child.getNamespaceURI())) {
                child.setTextContent(newIssuer);
                return;
            }
        }
    }

    private static void removeAssertionIssuer(Document doc) {
        Element assertion = (Element) doc.getElementsByTagNameNS(ASSERTION_NSURI.get(), "Assertion").item(0);
        NodeList issuers = assertion.getElementsByTagNameNS(ASSERTION_NSURI.get(), "Issuer");
        if (issuers.getLength() > 0) {
            issuers.item(0).getParentNode().removeChild(issuers.item(0));
        }
    }

    private static void removeRootIssuer(Document doc) {
        Element response = doc.getDocumentElement();
        NodeList children = response.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && "Issuer".equals(child.getLocalName())
                    && ASSERTION_NSURI.get().equals(child.getNamespaceURI())) {
                response.removeChild(child);
                return;
            }
        }
    }
}
