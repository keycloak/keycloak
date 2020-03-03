/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.adapter.AbstractAdapterTest;
import org.keycloak.testsuite.adapter.page.SalesPostAssertionAndResponseSig;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.updaters.Creator;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.RolesBuilder;
import org.junit.Test;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response.Status;
import javax.xml.crypto.dsig.XMLSignature;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assume;
import org.junit.Before;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.ASSERTION_NSURI;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.PROTOCOL_NSURI;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.samlServletDeployment;
import static org.keycloak.testsuite.saml.AbstractSamlTest.REALM_NAME;
import static org.keycloak.testsuite.saml.AbstractSamlTest.REALM_PRIVATE_KEY;
import static org.keycloak.testsuite.saml.AbstractSamlTest.REALM_PUBLIC_KEY;
import static org.keycloak.testsuite.saml.AbstractSamlTest.REALM_SIGNING_CERTIFICATE;

/**
 *
 * @author hmlnarik
 */
@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY_DEPRECATED)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT7)
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT8)
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT9)
public class SamlSignatureTest extends AbstractAdapterTest {

    private static final String REQUIRED_ROLE_NAME = "manager";
    private static final RoleRepresentation REQUIRED_ROLE = RoleBuilder.create().name(REQUIRED_ROLE_NAME).build();

    private static final String BROKER = "broker";
    private static final String APP_CLIENT_ID = "http://localhost:8280/sales-post-assertion-and-response-sig/";

    // Based on https://github.com/SAMLRaider/SAMLRaider/blob/master/src/main/java/helpers/XSWHelpers.java
    public static class XSWHelpers {

        /*
         * Following are the 8 common XML Signature Wrapping attacks implemented, which were found
         * in a paper called "On Breaking SAML: Be Whoever You Want to Be"
         * */

        public static void applyXSW1(Document document){
            Element response = (Element) document.getElementsByTagNameNS(PROTOCOL_NSURI.get(), "Response").item(0);
            Element clonedResponse = (Element) response.cloneNode(true);
            Element clonedSignature = (Element) clonedResponse.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
            Assume.assumeThat("Response needs to be signed", clonedSignature, notNullValue());
            clonedResponse.removeChild(clonedSignature);
            Element signature = (Element) response.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
            signature.appendChild(clonedResponse);
            response.setAttribute("ID", "_evil_response_ID");
        }

        public static void applyXSW2(Document document){
            Element response = (Element) document.getElementsByTagNameNS(PROTOCOL_NSURI.get(), "Response").item(0);
            Element clonedResponse = (Element) response.cloneNode(true);
            Element clonedSignature = (Element) clonedResponse.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
            Assume.assumeThat("Response needs to be signed", clonedSignature, notNullValue());
            clonedResponse.removeChild(clonedSignature);
            Element signature = (Element) response.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
            response.insertBefore(clonedResponse, signature);
            response.setAttribute("ID", "_evil_response_ID");
        }

        public static void applyXSW3(Document document){
            Element assertion = (Element) document.getElementsByTagNameNS(ASSERTION_NSURI.get(), "Assertion").item(0);
            Element evilAssertion = (Element) assertion.cloneNode(true);
            Element copiedSignature = (Element) evilAssertion.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
            Assume.assumeThat("Assertion needs to be signed", copiedSignature, notNullValue());
            evilAssertion.setAttribute("ID", "_evil_assertion_ID");
            evilAssertion.removeChild(copiedSignature);
            document.getDocumentElement().insertBefore(evilAssertion, assertion);
        }

        public static void applyXSW4(Document document){
            Element assertion = (Element) document.getElementsByTagNameNS(ASSERTION_NSURI.get(), "Assertion").item(0);
            Element evilAssertion = (Element) assertion.cloneNode(true);
            Element copiedSignature = (Element) evilAssertion.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
            Assume.assumeThat("Assertion needs to be signed", copiedSignature, notNullValue());
            evilAssertion.setAttribute("ID", "_evil_assertion_ID");
            evilAssertion.removeChild(copiedSignature);
            document.getDocumentElement().appendChild(evilAssertion);
            evilAssertion.appendChild(assertion);
        }

        public static void applyXSW5(Document document){
            Element evilAssertion = (Element) document.getElementsByTagNameNS(ASSERTION_NSURI.get(), "Assertion").item(0);
            Element assertion = (Element) evilAssertion.cloneNode(true);
            Element copiedSignature = (Element) assertion.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
            Assume.assumeThat("Assertion needs to be signed", copiedSignature, notNullValue());
            assertion.removeChild(copiedSignature);
            document.getDocumentElement().appendChild(assertion);
            evilAssertion.setAttribute("ID", "_evil_assertion_ID");
        }

        public static void applyXSW6(Document document){
            Element evilAssertion = (Element) document.getElementsByTagNameNS(ASSERTION_NSURI.get(), "Assertion").item(0);
            Element originalSignature = (Element) evilAssertion.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
            Element assertion = (Element) evilAssertion.cloneNode(true);
            Element copiedSignature = (Element) assertion.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
            Assume.assumeThat("Assertion needs to be signed", copiedSignature, notNullValue());
            assertion.removeChild(copiedSignature);
            originalSignature.appendChild(assertion);
            evilAssertion.setAttribute("ID", "_evil_assertion_ID");
        }

        public static void applyXSW7(Document document){
            Element assertion = (Element) document.getElementsByTagNameNS(ASSERTION_NSURI.get(), "Assertion").item(0);
            Element extensions = document.createElement("Extensions");
            document.getDocumentElement().insertBefore(extensions, assertion);
            Element evilAssertion = (Element) assertion.cloneNode(true);
            Element copiedSignature = (Element) evilAssertion.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
            Assume.assumeThat("Assertion needs to be signed", copiedSignature, notNullValue());
            evilAssertion.removeChild(copiedSignature);
            extensions.appendChild(evilAssertion);
        }

        public static void applyXSW8(Document document){
            Element evilAssertion = (Element) document.getElementsByTagNameNS(ASSERTION_NSURI.get(), "Assertion").item(0);
            Element originalSignature = (Element) evilAssertion.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
            Element assertion = (Element) evilAssertion.cloneNode(true);
            Element copiedSignature = (Element) assertion.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
            Assume.assumeThat("Assertion needs to be signed", copiedSignature, notNullValue());
            assertion.removeChild(copiedSignature);
            Element object = document.createElement("Object");
            originalSignature.appendChild(object);
            object.appendChild(assertion);
        }
    }

    @Page
    private SalesPostAssertionAndResponseSig salesPostAssertionAndResponseSigPage;

    private UserRepresentation user;

    @Deployment(name = SalesPostAssertionAndResponseSig.DEPLOYMENT_NAME)
    protected static WebArchive salesPostAssertionAndResponseSig() {
        return samlServletDeployment(SalesPostAssertionAndResponseSig.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return false;
    }

    private static ClientBuilder signingSamlClient(String clientId) {
        return ClientBuilder.create()
          .protocol(SamlProtocol.LOGIN_PROTOCOL)
          .enabled(true)
          .attribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "true")
          .attribute(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, "username")
          .attribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true")
          .attribute(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM, "RSA_SHA256")
          .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, "true")
          .clientId(clientId);
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        final ClientBuilder salesPostClient = signingSamlClient(APP_CLIENT_ID)
          .baseUrl("http://localhost:8080/sales-post-assertion-and-response-sig")
          .redirectUris("http://localhost:8080/sales-post-assertion-and-response-sig/*");
        final String brokerBaseUrl = getAuthServerRoot() + "realms/" + BROKER;
        final ClientBuilder brokerRealmIdPClient = signingSamlClient(brokerBaseUrl)
          .baseUrl(brokerBaseUrl + "/broker/" + REALM_NAME + "/endpoint")
          .redirectUris(brokerBaseUrl + "/broker/" + REALM_NAME + "/endpoint");

        testRealms.add(RealmBuilder.create()
          .name(REALM_NAME)
          .publicKey(REALM_PUBLIC_KEY)
          .privateKey(REALM_PRIVATE_KEY)
          .client(salesPostClient)
          .client(brokerRealmIdPClient)
          .roles(RolesBuilder.create().realmRole(REQUIRED_ROLE))
          .build()
        );

        testRealms.add(RealmBuilder.create()
          .name(BROKER)
          .publicKey(REALM_PUBLIC_KEY)
          .privateKey(REALM_PRIVATE_KEY)
          .client(salesPostClient)
          .identityProvider(IdentityProviderBuilder.create()
            .alias(REALM_NAME)
            .providerId(SAMLIdentityProviderFactory.PROVIDER_ID)
            .setAttribute(SAMLIdentityProviderConfig.SINGLE_SIGN_ON_SERVICE_URL, getAuthServerRoot() + "realms/" + REALM_NAME + "/protocol/saml")
            .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST, "true")
            .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_RESPONSE, "true")
            .setAttribute(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, REALM_SIGNING_CERTIFICATE)
            .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, "true")
            .setAttribute(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true")
          )
          .roles(RolesBuilder.create().realmRole(REQUIRED_ROLE))
          .build()
        );
    }

    @Before
    public void addFreshUserToDemoRealm() {
        this.user = UserBuilder.edit(createUserRepresentation(("U-" + UUID.randomUUID().toString()).toLowerCase(), "a@b.c", "A", "B", true))
          .password("password")
          .build();

        Creator<UserResource> c = Creator.create(adminClient.realm(REALM_NAME), user);
        getCleanup(REALM_NAME).addCleanup(c);
        List<RoleRepresentation> reqRoleToJoin = c.resource().roles().realmLevel().listAvailable().stream().filter(r -> r.getName().equals(REQUIRED_ROLE_NAME)).collect(Collectors.toList());
        c.resource().roles().realmLevel().add(reqRoleToJoin);
    }

    private void testSamlResponseModifications(Consumer<Document> samlResponseModifier, boolean shouldPass) throws Exception {
        final Consumer<CloseableHttpResponse> clientAssertions = shouldPass ? this::assertCorrectUserLoggedIn : SamlSignatureTest::assertUserAccessDenied;
        final Consumer<CloseableHttpResponse> brokerAssertions = shouldPass ? SamlSignatureTest::assertUpdateProfilePage   : SamlSignatureTest::assertNotUpdateProfilePage;
        testSamlResponseModificationsClient(samlResponseModifier, clientAssertions);
        testSamlResponseModificationsBroker(samlResponseModifier, brokerAssertions);
    }

    private void testSamlResponseModificationsBroker(Consumer<Document> samlResponseModifier, Consumer<CloseableHttpResponse> assertions) throws Exception {
        new SamlClientBuilder()
          .authnRequest(new URI(getAuthServerRoot() + "realms/" + BROKER + "/protocol/saml"), APP_CLIENT_ID, salesPostAssertionAndResponseSigPage.toString(), Binding.POST).build()
          .login().idp(REALM_NAME).build()
              .processSamlResponse(Binding.POST).build()
              .login().user(user).build()
              .processSamlResponse(Binding.POST).transformDocument(d -> { samlResponseModifier.accept(d); return d; }).build()
          .executeAndTransform(r -> { assertions.accept(r); return null; });
    }

    private void testSamlResponseModificationsClient(Consumer<Document> samlResponseModifier, Consumer<CloseableHttpResponse> assertions) {
        new SamlClientBuilder()
          .navigateTo(salesPostAssertionAndResponseSigPage)
          .processSamlResponse(Binding.POST).build()
          .login().user(user).build()
          .processSamlResponse(Binding.POST).transformDocument(d -> { samlResponseModifier.accept(d); return d; }).build()
          .executeAndTransform(r -> { assertions.accept(r); return null; });
    }

    private void assertCorrectUserLoggedIn(CloseableHttpResponse response) {
        assertThat(response, Matchers.statusCodeHC(is(Status.OK.getStatusCode())));
        assertThat(response, Matchers.bodyHC(containsString(user.getUsername())));
    }

    private static void assertUpdateProfilePage(CloseableHttpResponse response) {
        assertThat(response, Matchers.statusCodeIsHC(Status.OK));
        assertThat(response, Matchers.bodyHC(containsString("Update Account Information")));
    }

    private static void assertNotUpdateProfilePage(CloseableHttpResponse response) {
        assertThat(response, Matchers.statusCodeHC(greaterThanOrEqualTo(400)));
        assertThat(response, Matchers.bodyHC(not(containsString("Update Account Information"))));
    }

    private static void assertUserAccessDenied(CloseableHttpResponse response) {
        assertThat(response, Matchers.bodyHC(
          anyOf(
            containsString("INVALID_SIGNATURE"),
            containsString("EXTRACTION_FAILURE"),
            containsString("There was an error")
          )
        ));
    }

    private static void removeAllSignatures(Document doc) throws DOMException {
        NodeList signatures;
        while ((signatures = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature")).getLength() > 0) {
            Node s = signatures.item(0);
            s.getParentNode().removeChild(s);
        }
    }

    @Test
    public void testNoChange() throws Exception {
        testSamlResponseModifications(r -> {}, true);
    }

    @Test
    public void testRemoveSignatures() throws Exception {
        testSamlResponseModifications(SamlSignatureTest::removeAllSignatures, false);
    }

    @Test
    public void testXSW1() throws Exception {
        testSamlResponseModifications(XSWHelpers::applyXSW1, false);
    }

    @Test
    public void testXSW2() throws Exception {
        testSamlResponseModifications(XSWHelpers::applyXSW2, false);
    }

    @Test
    public void testXSW3() throws Exception {
        testSamlResponseModifications(XSWHelpers::applyXSW3, false);
    }

    @Test
    public void testXSW4() throws Exception {
        testSamlResponseModifications(XSWHelpers::applyXSW4, false);
    }

    @Test
    public void testXSW5() throws Exception {
        testSamlResponseModifications(XSWHelpers::applyXSW5, false);
    }

    @Test
    public void testXSW6() throws Exception {
        testSamlResponseModifications(XSWHelpers::applyXSW6, false);
    }

    @Test
    public void testXSW7() throws Exception {
        testSamlResponseModifications(XSWHelpers::applyXSW7, false);
    }

    @Test
    public void testXSW8() throws Exception {
        testSamlResponseModifications(XSWHelpers::applyXSW8, false);
    }

}
