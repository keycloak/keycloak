/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.test.broker.saml;


import org.junit.Assert;
import org.junit.Test;
import org.keycloak.broker.saml.SAMLDataMarshaller;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SAMLDataMarshallerTest {

    private static final String TEST_RESPONSE = "<samlp:Response xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"ID_4804cf50-cd96-4b92-823e-89adaa0c78ba\" Version=\"2.0\" IssueInstant=\"2015-11-06T11:00:33.920Z\" Destination=\"http://localhost:8081/auth/realms/realm-with-broker/broker/kc-saml-idp-basic/endpoint\" InResponseTo=\"ID_c6b90123-f0bb-4c5c-bf9d-388d5bbe467a\"><saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">http://localhost:8082/auth/realms/realm-with-saml-idp-basic</saml:Issuer><samlp:Status><samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"></samlp:StatusCode></samlp:Status><saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"ID_29b196c2-d641-45c8-a423-8ed8e54d4cf9\" Version=\"2.0\" IssueInstant=\"2015-11-06T11:00:33.911Z\"><saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">http://localhost:8082/auth/realms/realm-with-saml-idp-basic</saml:Issuer><saml:Subject><saml:NameID xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">test-user</saml:NameID><saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml:SubjectConfirmationData InResponseTo=\"ID_c6b90123-f0bb-4c5c-bf9d-388d5bbe467a\" NotOnOrAfter=\"2015-11-06T11:05:31.911Z\" Recipient=\"http://localhost:8081/auth/realms/realm-with-broker/broker/kc-saml-idp-basic/endpoint\"></saml:SubjectConfirmationData></saml:SubjectConfirmation></saml:Subject><saml:Conditions NotBefore=\"2015-11-06T11:00:31.911Z\" NotOnOrAfter=\"2015-11-06T11:01:31.911Z\"><saml:AudienceRestriction><saml:Audience>http://localhost:8081/auth/realms/realm-with-broker</saml:Audience></saml:AudienceRestriction></saml:Conditions><saml:AuthnStatement AuthnInstant=\"2015-11-06T11:00:33.923Z\" SessionIndex=\"fa0f4fd3-8a11-44f4-9acb-ee30c5bb8fe5\"><saml:AuthnContext><saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</saml:AuthnContextClassRef></saml:AuthnContext></saml:AuthnStatement><saml:AttributeStatement><saml:Attribute Name=\"mobile\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"><saml:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:string\">617-666-7777</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"urn:oid:1.2.840.113549.1.9.1\" FriendlyName=\"email\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"><saml:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:string\">test-user@localhost</saml:AttributeValue></saml:Attribute></saml:AttributeStatement><saml:AttributeStatement><saml:Attribute Name=\"Role\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"><saml:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:string\">manager</saml:AttributeValue></saml:Attribute></saml:AttributeStatement></saml:Assertion></samlp:Response>";

    private static final String TEST_ASSERTION = "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"ID_29b196c2-d641-45c8-a423-8ed8e54d4cf9\" Version=\"2.0\" IssueInstant=\"2015-11-06T11:00:33.911Z\"><saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">http://localhost:8082/auth/realms/realm-with-saml-idp-basic</saml:Issuer><saml:Subject><saml:NameID xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">test-user</saml:NameID><saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml:SubjectConfirmationData InResponseTo=\"ID_c6b90123-f0bb-4c5c-bf9d-388d5bbe467a\" NotOnOrAfter=\"2015-11-06T11:05:31.911Z\" Recipient=\"http://localhost:8081/auth/realms/realm-with-broker/broker/kc-saml-idp-basic/endpoint\"></saml:SubjectConfirmationData></saml:SubjectConfirmation></saml:Subject><saml:Conditions NotBefore=\"2015-11-06T11:00:31.911Z\" NotOnOrAfter=\"2015-11-06T11:01:31.911Z\"><saml:AudienceRestriction><saml:Audience>http://localhost:8081/auth/realms/realm-with-broker</saml:Audience></saml:AudienceRestriction></saml:Conditions><saml:AuthnStatement AuthnInstant=\"2015-11-06T11:00:33.923Z\" SessionIndex=\"fa0f4fd3-8a11-44f4-9acb-ee30c5bb8fe5\"><saml:AuthnContext><saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</saml:AuthnContextClassRef></saml:AuthnContext></saml:AuthnStatement><saml:AttributeStatement><saml:Attribute Name=\"mobile\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"><saml:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:string\">617-666-7777</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"urn:oid:1.2.840.113549.1.9.1\" FriendlyName=\"email\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"><saml:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:string\">test-user@localhost</saml:AttributeValue></saml:Attribute></saml:AttributeStatement><saml:AttributeStatement><saml:Attribute Name=\"Role\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"><saml:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:string\">manager</saml:AttributeValue></saml:Attribute></saml:AttributeStatement></saml:Assertion>";

    private static final String TEST_ASSERTION_WITH_NAME_ID = "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"ID_29b196c2-d641-45c8-a423-8ed8e54d4cf9\" Version=\"2.0\" IssueInstant=\"2015-11-06T11:00:33.911Z\"><saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">http://localhost:8082/auth/realms/realm-with-saml-idp-basic</saml:Issuer><saml:Subject><saml:NameID xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">test-user</saml:NameID><saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml:SubjectConfirmationData InResponseTo=\"ID_c6b90123-f0bb-4c5c-bf9d-388d5bbe467a\" NotOnOrAfter=\"2015-11-06T11:05:31.911Z\" Recipient=\"http://localhost:8081/auth/realms/realm-with-broker/broker/kc-saml-idp-basic/endpoint\"></saml:SubjectConfirmationData></saml:SubjectConfirmation></saml:Subject><saml:Conditions NotBefore=\"2015-11-06T11:00:31.911Z\" NotOnOrAfter=\"2015-11-06T11:01:31.911Z\"><saml:AudienceRestriction><saml:Audience>http://localhost:8081/auth/realms/realm-with-broker</saml:Audience></saml:AudienceRestriction></saml:Conditions><saml:AuthnStatement AuthnInstant=\"2015-11-06T11:00:33.923Z\" SessionIndex=\"fa0f4fd3-8a11-44f4-9acb-ee30c5bb8fe5\"><saml:AuthnContext><saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</saml:AuthnContextClassRef></saml:AuthnContext></saml:AuthnStatement><saml:AttributeStatement><saml:Attribute Name=\"mobile\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"><saml:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:string\">617-666-7777</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"urn:oid:1.2.840.113549.1.9.1\" FriendlyName=\"email\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"><saml:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:string\">test-user@localhost</saml:AttributeValue></saml:Attribute></saml:AttributeStatement><saml:AttributeStatement><saml:Attribute Name=\"Role\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"><saml:AttributeValue><saml:NameID xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\">b2c6275838784dba219c92f53ea5493c8ef4da09</saml:NameID></saml:AttributeValue></saml:Attribute></saml:AttributeStatement></saml:Assertion>";
    
    private static final String TEST_AUTHN_TYPE = "<saml:AuthnStatement xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns=\"urn:oasis:names:tc:SAML:2.0:assertion\" AuthnInstant=\"2015-11-06T11:00:33.923Z\" SessionIndex=\"fa0f4fd3-8a11-44f4-9acb-ee30c5bb8fe5\"><saml:AuthnContext><saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</saml:AuthnContextClassRef></saml:AuthnContext></saml:AuthnStatement>";

    @Test
    public void testParseResponse() throws Exception {
        SAMLDataMarshaller serializer = new SAMLDataMarshaller();
        ResponseType responseType = serializer.deserialize(TEST_RESPONSE, ResponseType.class);

        // test ResponseType
        Assert.assertEquals(responseType.getID(), "ID_4804cf50-cd96-4b92-823e-89adaa0c78ba");
        Assert.assertEquals(responseType.getDestination(), "http://localhost:8081/auth/realms/realm-with-broker/broker/kc-saml-idp-basic/endpoint");
        Assert.assertEquals(responseType.getIssuer().getValue(), "http://localhost:8082/auth/realms/realm-with-saml-idp-basic");
        Assert.assertEquals(responseType.getAssertions().get(0).getID(), "ID_29b196c2-d641-45c8-a423-8ed8e54d4cf9");

        // back to String
        String serialized = serializer.serialize(responseType);
        Assert.assertEquals(TEST_RESPONSE, serialized);
    }

    @Test
    public void testParseAssertion() throws Exception {
        SAMLDataMarshaller serializer = new SAMLDataMarshaller();
        AssertionType assertion = serializer.deserialize(TEST_ASSERTION, AssertionType.class);

        // test assertion
        Assert.assertEquals(assertion.getID(), "ID_29b196c2-d641-45c8-a423-8ed8e54d4cf9");
        Assert.assertEquals(((NameIDType) assertion.getSubject().getSubType().getBaseID()).getValue(), "test-user");

        // back to String
        String serialized = serializer.serialize(assertion);
        Assert.assertEquals(TEST_ASSERTION, serialized);
    }

    @Test
    public void testParseAssertionWitNameId() throws Exception {
        SAMLDataMarshaller serializer = new SAMLDataMarshaller();
        AssertionType assertion = serializer.deserialize(TEST_ASSERTION_WITH_NAME_ID, AssertionType.class);

        // test assertion
        Assert.assertEquals(assertion.getID(), "ID_29b196c2-d641-45c8-a423-8ed8e54d4cf9");
        Assert.assertEquals(((NameIDType) assertion.getSubject().getSubType().getBaseID()).getValue(), "test-user");

        // back to String
        String serialized = serializer.serialize(assertion);
        Assert.assertEquals(TEST_ASSERTION_WITH_NAME_ID, serialized);
    }
    
    @Test
    public void testParseAuthnType() throws Exception {
        SAMLDataMarshaller serializer = new SAMLDataMarshaller();
        AuthnStatementType authnStatement =  serializer.deserialize(TEST_AUTHN_TYPE, AuthnStatementType.class);

        // test authnStatement
        Assert.assertEquals(authnStatement.getSessionIndex(), "fa0f4fd3-8a11-44f4-9acb-ee30c5bb8fe5");

        // back to String
        String serialized = serializer.serialize(authnStatement);
        Assert.assertEquals(TEST_AUTHN_TYPE, serialized);
    }
}
