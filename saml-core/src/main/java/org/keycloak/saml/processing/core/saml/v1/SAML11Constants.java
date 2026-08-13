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
package org.keycloak.saml.processing.core.saml.v1;

/**
 * Constants for the SAML v1.1 Specifications
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 22, 2011
 */
public interface SAML11Constants {

    String ACTION = "Action";

    String ASSERTIONID = "AssertionID";

    String ASSERTION_11_NSURI = "urn:oasis:names:tc:SAML:1.0:assertion";

    String ASSERTION_ARTIFACT = "AssertionArtifact";

    String ASSERTION_ID_REF = "AssertionIDReference";

    String ATTRIBUTE_QUERY = "AttributeQuery";

    String ATTRIBUTE_NAME = "AttributeName";

    String ATTRIBUTE_NAMESPACE = "AttributeNamespace";

    String ATTRIBUTE_STATEMENT = "AttributeStatement";

    String AUDIENCE_RESTRICTION_CONDITION = "AudienceRestrictionCondition";

    String AUTHENTICATION_INSTANT = "AuthenticationInstant";

    String AUTHENTICATION_METHOD = "AuthenticationMethod";

    String AUTHENTICATION_QUERY = "AuthenticationQuery";

    String AUTHENTICATION_STATEMENT = "AuthenticationStatement";

    String AUTHORITY_BINDING = "AuthorityBinding";

    String AUTHORITY_KIND = "AuthorityKind";

    String AUTHORIZATION_DECISION_QUERY = "AuthorizationDecisionQuery";

    String AUTHORIZATION_DECISION_STATEMENT = "AuthorizationDecisionStatement";

    String BINDING = "Binding";

    String CONFIRMATION_METHOD = "ConfirmationMethod";

    String DECISION = "Decision";

    String DNS_ADDRESS = "DNSAddress";

    String EVIDENCE = "Evidence";

    String FORMAT = "Format";

    String IN_RESPONSE_TO = "InResponseTo";

    String IP_ADDRESS = "IPAddress";

    String ISSUER = "Issuer";

    String ISSUE_INSTANT = "IssueInstant";

    String LOCATION = "Location";

    String MAJOR_VERSION = "MajorVersion";

    String MINOR_VERSION = "MinorVersion";

    String NAME_IDENTIFIER = "NameIdentifier";

    String NAME_QUALIFIER = "NameQualifier";

    String NAMESPACE = "Namespace";

    String PROTOCOL_11_NSURI = "urn:oasis:names:tc:SAML:1.0:protocol";

    String RECIPIENT = "Recipient";

    String REQUEST = "Request";

    String REQUEST_ID = "RequestID";

    String RESOURCE = "Resource";

    String RESPONSE = "Response";

    String RESPONSE_ID = "ResponseID";

    String STATUS = "Status";

    String STATUS_CODE = "StatusCode";

    String STATUS_DETAIL = "StatusDetail";

    String STATUS_MSG = "StatusMessage";

    String VALUE = "Value";
}