/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.client.policies;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.Constants;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation.KeyMetadataRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.services.clientpolicy.condition.ClientProtocolCondition;
import org.keycloak.services.clientpolicy.condition.ClientProtocolConditionFactory;
import org.keycloak.services.clientpolicy.executor.SamlAvoidRedirectBindingExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SamlSecureClientUrisExecutor;
import org.keycloak.services.clientpolicy.executor.SamlSecureClientUrisExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SamlSignatureEnforcerExecutorFactory;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.util.ClientPoliciesUtil;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.apache.http.util.EntityUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author rmartinc
 */
public class SamlClientPoliciesExecutorTest extends AbstractTestRealmKeycloakTest {

    private static final String PROFILE_POLICY_NAME = "test";
    private static final String PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7qZGaTn+o0pWr"
            + "MS09ZVWiOPY1tKzqC+Zuvj9j4C46oNQOi1iSM0CRhhk9UUimYltnNsKoJzduk5hS"
            + "02/0rYhPGwH1AENUQzpmHyBii1u3Ywi6rwb+wPY7EsDzSF7lmwiDAlVJtVhuif+J"
            + "UFNlmP29wWFVaaM1JAK7wTgiOSoxeFqQPBwL4d+kjKm4S9LV6pOs798WEwvBJKtB"
            + "bve/ZvQHmpbt3ftbBegaXQJUzsxr7xqo+DKI6RPAprn0v0W8pGHiMQ/WoTtan5ka"
            + "R3LFSShtx3lX1WVNnT+0wBZcLEkQzaB1R6prwYlSQ6JaydjUqstInrgU2WUVwXJi"
            + "MNR0ulB7AgMBAAECggEANfV/5DqGAmjqmBq/w1OL1+VBBhg5T+K0E0uotnMTV9A+"
            + "qR/wC7mo6y7/ut3QYecOGRNpzSfZjHXr6oTZQCVVeElvup6kvWnHNO3mRe+EI6ra"
            + "K7N/82hQZJPz3wAEKUj2nZTiKRt3nfEYBMeP8zqWWyVrcz+4qeL81jesiEqfkzFl"
            + "gefsbBwYz7odNHyvYOYklrudKpfQQDff9zKEpPh9ou6TP5cHyPXNxN2HxSKjAsxN"
            + "OO/zxrtJeuP1pLMhLJQZuQyAuckdkAy4Cv8K8x+r6/8PxIFjz1+6+I4uolnsSpS7"
            + "+upOK+866XUIE6h6hlLTEp+XXds+/LFZaT+B/vBTwQKBgQDKCxNuwrRAD+JifB0t"
            + "hvnew8kwW48dT0ZM2m3Fw85Vey2hVStZoR6tYuxtoXLBpJCjGpeFhxm1BPV38SNr"
            + "78W98NduS57uU5iueByKY/twDws/AFSHpsLyMiWJBBwH8VRm0U4GmFxBlNCrU3TP"
            + "ddJxAgy5XYSP+LG7LXr0jZ1oSQKBgQDtx1OhAR14urfnuQgsmH9G6bEXFmR71uL8"
            + "OJBU0n/AB8bPQbisDRPcSkdF7KgQI5hJE+5+8aERmAZ1B//5wbZlmR/lmd+V/c/6"
            + "BAxJkQicjVG5EhgliM73z4jm/85pYfkN9IgbZlB7vCVKfKWKSIwJ/pY039WN+2t4"
            + "BsenhJ2aowKBgBnJCBXeq3pxjIbdKCwjSchwXEDbrowjDenJBrFyp+ao7c3lPL8X"
            + "nP6r3ViwfiDQi9UFE8lq0JEVrO49zDN+SlJPZm8hH4tzB81cbugKkpBemyTTOfaG"
            + "BeM7Gyc9awZoekkU9UxKLZwBDhCPehzwAIeDp3QQx1ZIewZUa5jCahBhAoGAd/Yg"
            + "UxJk9Av/zIClhxpI3FX6alN5zqDTU7yV1LV+jjteKiJWMTdH1dQDsVt8TugmZHgR"
            + "0ynEwUOZvmGS20bH5uoiFYxUKTAsRU7VhCgP2CvUFzLxy74B7TRfNWvJj5FGPawp"
            + "Hum3oTWC+tl4CxQe0swGrBZhf4hg5+VDxVg6y1ECgYEAns+tbdeBJWV/r3Rh3e0C"
            + "LetywsgNG02aIuZpIlT4VWKV5cIIq6d20C6I/EKhAZ0E56D8xX6xVmUBcY6Qv1zd"
            + "7yOVwITM3P64G+ZtPkm3m3w7XRnaIEUHuwFWpdMPjfdipjelq0ltbRQMOiqYAQYR"
            + "jfH4O0lZc8bo2TVGeQHpyg4=";
    private static final String CERTIFICATE = "MIICyDCCAbCgAwIBAgIJAIiYjotcPTEkMA0GCSqGSIb3DQEBCwUAMBExDzANBgNV"
            + "BAMTBmNsaWVudDAgFw0yNDAxMjYxMzM5MjNaGA8yMDc4MTAyOTEzMzkyM1owETEP"
            + "MA0GA1UEAxMGY2xpZW50MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA"
            + "u6mRmk5/qNKVqzEtPWVVojj2NbSs6gvmbr4/Y+AuOqDUDotYkjNAkYYZPVFIpmJb"
            + "ZzbCqCc3bpOYUtNv9K2ITxsB9QBDVEM6Zh8gYotbt2MIuq8G/sD2OxLA80he5ZsI"
            + "gwJVSbVYbon/iVBTZZj9vcFhVWmjNSQCu8E4IjkqMXhakDwcC+HfpIypuEvS1eqT"
            + "rO/fFhMLwSSrQW73v2b0B5qW7d37WwXoGl0CVM7Ma+8aqPgyiOkTwKa59L9FvKRh"
            + "4jEP1qE7Wp+ZGkdyxUkobcd5V9VlTZ0/tMAWXCxJEM2gdUeqa8GJUkOiWsnY1KrL"
            + "SJ64FNllFcFyYjDUdLpQewIDAQABoyEwHzAdBgNVHQ4EFgQUplMyjmtmloAy8sTA"
            + "CENFZugti98wDQYJKoZIhvcNAQELBQADggEBACXxwe1HJ0j56SgGueNSzfoUXwI4"
            + "a0XUN73I3zuXOwBoSqJr7X17B0ZDrHAb+k1WOz1iIz6OA2Bi1p8rtYqn/rLAdCbQ"
            + "fatlSzVrVkxc689LEOFiN9eGlfBpqX/VllY9DPzmMoPLa1v0Ya/AXIQlyURbe3Ve"
            + "PHdhS8lScQi239FtSq1pKlRRzBsfTNwD7MbgY2kGPSKBqe9TuYqYTjc4r0XmjVO2"
            + "ZI3mUuNOSpBrH2YY5umutjH4ZTJstzf82kp1m+/wsNM46ZvV4DCHxNUESONzZteW"
            + "+9OgpVwAt9ltqlX6qFxq04S0pAA2AyLnDvMuIUgtdNn7jFCwqYCePnDWJfY=";

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        // no-op
    }

    @Test
    public void testSamlSecureClientUrisExecutor() throws Exception {
        createClientProfileAndPolicyToTest(SamlSecureClientUrisExecutorFactory.PROVIDER_ID, null);
        final RealmResource realm = testRealm();
        String clientId = null;
        try {
            final ClientRepresentation client = createSecureClient("test-saml-client");
            // test creation fails for non-https urls
            testSamlSecureClient(client, c -> realm.clients().create(c));
            // create it
            testClientOperation(client, null, c -> realm.clients().create(c));
            clientId = realm.clients().findByClientId(client.getClientId()).iterator().next().getId();
            client.setId(clientId);
            // test update fails for non-https urls
            testSamlSecureClient(client, c -> updateClient(realm, c));
            // test wildcard redirects are not valid
            testRedirectUrisWildcard(client, c -> updateClient(realm, c));
            // test a login with https is valid
            testAuthenticationPostSuccess(client);
            // test a login with http is invalid
            testAuthenticationPostError(client, "http://client.keycloak.org/saml/");
        } finally {
            removeClientProfileAndPolicyToTest();
            if (clientId != null) {
                realm.clients().get(clientId).remove();
            }
        }
    }

    @Test
    public void testSamlAvoidRedirectBindingExecutor() throws Exception {
        String clientId = null;
        final RealmResource realm = testRealm();
        try {
            final ClientRepresentation client = createSecureClient("test-saml-client");
            client.getAttributes().put(SamlConfigAttributes.SAML_FORCE_POST_BINDING, Boolean.FALSE.toString());

            // create a client without the executor enabled
            testClientOperation(client, null, c -> realm.clients().create(c));
            clientId = realm.clients().findByClientId(client.getClientId()).iterator().next().getId();

            // enable the policy and check redirect is not allowed
            createClientProfileAndPolicyToTest(SamlAvoidRedirectBindingExecutorFactory.PROVIDER_ID, null);
            testAuthenticationRedirectPostError(client);
            realm.clients().get(clientId).remove();

            // test creation without signature fails
            testClientOperation(client, "Force POST binding is not enabled", c -> realm.clients().create(c));
            // put force post binding
            client.getAttributes().put(SamlConfigAttributes.SAML_FORCE_POST_BINDING, Boolean.TRUE.toString());
            testClientOperation(client, null, c -> realm.clients().create(c));
            // update without post binding fails
            clientId = realm.clients().findByClientId(client.getClientId()).iterator().next().getId();
            client.setId(clientId);
            client.getAttributes().put(SamlConfigAttributes.SAML_FORCE_POST_BINDING, Boolean.FALSE.toString());
            testClientOperation(client, "Force POST binding is not enabled", c -> updateClient(realm, client));
            // with force works OK
            client.getAttributes().put(SamlConfigAttributes.SAML_FORCE_POST_BINDING, Boolean.TRUE.toString());
            testClientOperation(client, null, c -> updateClient(realm, client));
            // test a REDIRECT is forced to POST OK
            testAuthenticationRedirectPostSuccess(client);
        } finally {
            removeClientProfileAndPolicyToTest();
            if (clientId != null) {
                realm.clients().get(clientId).remove();
            }
        }
    }

    @Test
    public void testSamlSignatureEnforcerExecutor() throws Exception {
        String clientId = null;
        final RealmResource realm = testRealm();
        try {
            final ClientRepresentation client = createSecureClient("test-saml-client");
            client.getAttributes().put(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, Boolean.FALSE.toString());

            // create a client without client signature
            testClientOperation(client, null, c -> realm.clients().create(c));
            clientId = realm.clients().findByClientId(client.getClientId()).iterator().next().getId();

            // enable the policy and check login without signature is not allowed
            createClientProfileAndPolicyToTest(SamlSignatureEnforcerExecutorFactory.PROVIDER_ID, null);
            testAuthenticationPostError(client, client.getAdminUrl());
            realm.clients().get(clientId).remove();

            // test creation
            testSamlAttributeOperation(client, "Signatures not ensured for the client.",
                    SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE,
                    Boolean.FALSE.toString(), Boolean.TRUE.toString(), c -> realm.clients().create(c));
            testSamlAttributeOperation(client, "Signatures not ensured for the client.",
                    SamlConfigAttributes.SAML_SERVER_SIGNATURE,
                    Boolean.FALSE.toString(), Boolean.FALSE.toString(), c -> realm.clients().create(c));
            testSamlAttributeOperation(client, "Signatures not ensured for the client.",
                    SamlConfigAttributes.SAML_ASSERTION_SIGNATURE,
                    Boolean.FALSE.toString(), Boolean.FALSE.toString(), c -> realm.clients().create(c));
            testSamlAttributeOperation(client, null,
                    SamlConfigAttributes.SAML_SERVER_SIGNATURE,
                    Boolean.TRUE.toString(), Boolean.TRUE.toString(), c -> realm.clients().create(c));
            clientId = realm.clients().findByClientId(client.getClientId()).iterator().next().getId();
            client.setId(clientId);

            // test update
            testSamlAttributeOperation(client, "Signatures not ensured for the client.",
                    SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE,
                    Boolean.FALSE.toString(), Boolean.TRUE.toString(), c -> updateClient(realm, c));
            testSamlAttributeOperation(client, "Signatures not ensured for the client.",
                    SamlConfigAttributes.SAML_SERVER_SIGNATURE,
                    Boolean.FALSE.toString(), Boolean.FALSE.toString(), c -> updateClient(realm, c));
            testSamlAttributeOperation(client, "Signatures not ensured for the client.",
                    SamlConfigAttributes.SAML_ASSERTION_SIGNATURE,
                    Boolean.FALSE.toString(), Boolean.FALSE.toString(), c -> updateClient(realm, c));

            // test login is OK with signatures
            client.getAttributes().put(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, Boolean.TRUE.toString());
            client.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, Boolean.TRUE.toString());
            client.getAttributes().put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, Boolean.FALSE.toString());
            client.getAttributes().put(SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE, CERTIFICATE);
            testClientOperation(client, null, c -> updateClient(realm, c));

            testAuthenticationPostSignatureSuccess(realm, client, PRIVATE_KEY, CERTIFICATE);
        } finally {
            removeClientProfileAndPolicyToTest();
            if (clientId != null) {
                realm.clients().get(clientId).remove();
            }
        }
    }

    private Response updateClient(RealmResource realm, ClientRepresentation client) {
        try {
            realm.clients().get(client.getId()).update(client);
            return null;
        } catch (ClientErrorException e) {
            return e.getResponse();
        }
    }

    private void testAuthenticationPostSuccess(ClientRepresentation client) {
        new SamlClientBuilder()
                .authnRequest(RealmsResource.protocolUrl(UriBuilder.fromUri(getAuthServerRoot())).build(TEST_REALM_NAME, SamlProtocol.LOGIN_PROTOCOL),
                        client.getClientId(), client.getAdminUrl(), SamlClient.Binding.POST)
                .build()
                .login().user("test-user@localhost", "password").build()
                .execute(hr -> {
                    try {
                        SAMLDocumentHolder doc = SamlClient.Binding.POST.extractResponse(hr);
                        MatcherAssert.assertThat(doc.getSamlObject(),
                                org.keycloak.testsuite.util.Matchers.isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
                    } catch (IOException ex) {
                        throw new IllegalStateException(ex);
                    }
                });
    }

    private void testAuthenticationPostSignatureSuccess(RealmResource realm, ClientRepresentation client, String privateKey, String certificate) {
        KeysMetadataRepresentation keysMetadata = realm.keys().getKeyMetadata();
        String kid = keysMetadata.getActive().get(Constants.DEFAULT_SIGNATURE_ALGORITHM);
        KeyMetadataRepresentation keyMetadata = keysMetadata.getKeys().stream()
                .filter(k -> kid.equals(k.getKid())).findAny().orElse(null);
        Assert.assertNotNull(keyMetadata);

        new SamlClientBuilder()
                .authnRequest(RealmsResource.protocolUrl(UriBuilder.fromUri(getAuthServerRoot())).build(TEST_REALM_NAME, SamlProtocol.LOGIN_PROTOCOL),
                        client.getClientId(), client.getAdminUrl(), SamlClient.Binding.POST)
                .signWith(privateKey, null, certificate)
                .build()
                .login().user("test-user@localhost", "password").build()
                .execute(hr -> {
                    try {
                        SAMLDocumentHolder doc = SamlClient.Binding.POST.extractResponse(hr, keyMetadata.getPublicKey());
                        MatcherAssert.assertThat(doc.getSamlObject(),
                                org.keycloak.testsuite.util.Matchers.isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
                    } catch (IOException ex) {
                        throw new IllegalStateException(ex);
                    }
                });
    }

    private void testAuthenticationPostError(ClientRepresentation client, String assertionUrl) {
        new SamlClientBuilder()
                .authnRequest(RealmsResource.protocolUrl(UriBuilder.fromUri(getAuthServerRoot())).build(TEST_REALM_NAME, SamlProtocol.LOGIN_PROTOCOL),
                        client.getClientId(), assertionUrl, SamlClient.Binding.POST)
                .build()
                .executeAndTransform(response -> {
                    Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());
                    MatcherAssert.assertThat(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8),
                            Matchers.containsString("Invalid Request"));
                    return null;
                });
    }

    private void testAuthenticationRedirectPostSuccess(ClientRepresentation client) {
        new SamlClientBuilder()
                .authnRequest(RealmsResource.protocolUrl(UriBuilder.fromUri(getAuthServerRoot())).build(TEST_REALM_NAME, SamlProtocol.LOGIN_PROTOCOL),
                        client.getClientId(), client.getAdminUrl(), SamlClient.Binding.REDIRECT)
                .build()
                .login().user("test-user@localhost", "password").build()
                .execute(hr -> {
                    try {
                        SAMLDocumentHolder doc = SamlClient.Binding.POST.extractResponse(hr);
                        MatcherAssert.assertThat(doc.getSamlObject(),
                                org.keycloak.testsuite.util.Matchers.isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
                    } catch (IOException ex) {
                        throw new IllegalStateException(ex);
                    }
                });
    }

    private void testAuthenticationRedirectPostError(ClientRepresentation client) {
        new SamlClientBuilder()
                .authnRequest(RealmsResource.protocolUrl(UriBuilder.fromUri(getAuthServerRoot())).build(TEST_REALM_NAME, SamlProtocol.LOGIN_PROTOCOL),
                        client.getClientId(), client.getAdminUrl(), SamlClient.Binding.REDIRECT)
                .build()
                .executeAndTransform(response -> {
                    Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());
                    MatcherAssert.assertThat(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8),
                            Matchers.containsString("Invalid Request"));
                    return null;
                });
    }

    private void testClientOperation(ClientRepresentation client, String errorPrefix, Function<ClientRepresentation, Response> operation) {
        try (Response response = operation.apply(client)) {
            if (errorPrefix == null) {
                if (response != null) {
                    // create returns 201, update returns null
                    Assert.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
                }
            } else {
                Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
                OAuth2ErrorRepresentation error = response.readEntity(OAuth2ErrorRepresentation.class);
                Assert.assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, error.getError());
                MatcherAssert.assertThat(error.getErrorDescription(), Matchers.startsWith(errorPrefix));
            }
        }
    }

    private void testSamlAttributeOperation(ClientRepresentation client, String error, String attr,
            String beforeValue, String afterValue, Function<ClientRepresentation, Response> operation) {
        client.getAttributes().put(attr, beforeValue);
        testClientOperation(client, error, operation);
        client.getAttributes().put(attr, afterValue);
    }

    private void testSamlSecureClientAttribute(ClientRepresentation client,
            String attr, Function<ClientRepresentation, Response> operation) {
        testSamlAttributeOperation(client, "Non secure scheme for ", attr, "http://client.keycloak.org/saml/",
                "https://client.keycloak.org/saml/", operation);
    }

    private void testRedirectUrisWildcard(ClientRepresentation client, Function<ClientRepresentation, Response> operation)
            throws Exception {
        // wildcards allowed
        createClientProfileAndPolicyToTest(SamlSecureClientUrisExecutorFactory.PROVIDER_ID,
                new SamlSecureClientUrisExecutor.Configuration(true));
        client.getRedirectUris().add("https://client.keycloak.org/saml/*");
        testClientOperation(client, null, operation);

        // wildcards disallowed
        createClientProfileAndPolicyToTest(SamlSecureClientUrisExecutorFactory.PROVIDER_ID,
                new SamlSecureClientUrisExecutor.Configuration(false));
        testClientOperation(client, "Unsecure wildcard redirect ", operation);
        client.getRedirectUris().remove("https://client.keycloak.org/saml/*");
        testClientOperation(client, null, operation);
    }

    private void testSamlSecureClient(ClientRepresentation client, Function<ClientRepresentation, Response> operation) {
        // rootUrl
        client.setRootUrl("http://client.keycloak.org/saml/");
        testClientOperation(client, "Non secure scheme for ", operation);
        client.setRootUrl("https://client.keycloak.org/saml/");

        // baseUrl
        client.setBaseUrl("http://client.keycloak.org/saml/");
        testClientOperation(client, "Non secure scheme for ", operation);
        client.setBaseUrl("https://client.keycloak.org/saml/");

        // adminUrl
        client.setAdminUrl("http://client.keycloak.org/saml/");
        testClientOperation(client, "Non secure scheme for ", operation);
        client.setAdminUrl("https://client.keycloak.org/saml/");

        // redirect URIs
        client.getRedirectUris().add("http://client.keycloak.org/saml/");
        testClientOperation(client, "Non secure scheme for ", operation);
        client.getRedirectUris().remove("http://client.keycloak.org/saml/");

        // test saml specific protocol urls
        testSamlSecureClientAttribute(client, SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, operation);
        testSamlSecureClientAttribute(client, SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE, operation);
        testSamlSecureClientAttribute(client, SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE, operation);
        testSamlSecureClientAttribute(client, SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, operation);
        testSamlSecureClientAttribute(client, SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE, operation);
        testSamlSecureClientAttribute(client, SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, operation);
        testSamlSecureClientAttribute(client, SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_ATTRIBUTE, operation);
        testSamlSecureClientAttribute(client, SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE, operation);
    }

    private void createClientProfileAndPolicyToTest(String executorId, ClientPolicyExecutorConfigurationRepresentation config) throws Exception {
        RealmResource realm = testRealm();
        ClientProfileRepresentation profile = new ClientPoliciesUtil.ClientProfileBuilder()
                .createProfile(PROFILE_POLICY_NAME, "The profile to test")
                .addExecutor(executorId, config)
                .toRepresentation();
        ClientProfilesRepresentation profiles = new ClientPoliciesUtil.ClientProfilesBuilder()
                .addProfile(profile)
                .toRepresentation();
        realm.clientPoliciesProfilesResource().updateProfiles(profiles);

        ClientPolicyRepresentation policy = new ClientPoliciesUtil.ClientPolicyBuilder()
                .createPolicy(PROFILE_POLICY_NAME, "The policy to test.", Boolean.TRUE)
                .addCondition(ClientProtocolConditionFactory.PROVIDER_ID, new ClientProtocolCondition.Configuration(SamlProtocol.LOGIN_PROTOCOL))
                .addProfile(PROFILE_POLICY_NAME)
                .toRepresentation();
        ClientPoliciesRepresentation policies = new ClientPoliciesUtil.ClientPoliciesBuilder()
                .addPolicy(policy)
                .toRepresentation();
        realm.clientPoliciesPoliciesResource().updatePolicies(policies);
    }

    private void removeClientProfileAndPolicyToTest() {
        ClientProfilesRepresentation profiles = new ClientPoliciesUtil.ClientProfilesBuilder().toRepresentation();
        adminClient.realm(TEST_REALM_NAME).clientPoliciesProfilesResource().updateProfiles(profiles);
        ClientPoliciesRepresentation policies = new ClientPoliciesUtil.ClientPoliciesBuilder().toRepresentation();
        adminClient.realm(TEST_REALM_NAME).clientPoliciesPoliciesResource().updatePolicies(policies);
    }

    private ClientRepresentation createSecureClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();
        client.setName(clientId);
        client.setClientId(clientId);
        client.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        client.setRootUrl("https://client.keycloak.org/saml/");
        client.setAdminUrl("https://client.keycloak.org/saml/");
        client.setBaseUrl("https://client.keycloak.org/saml/");
        List<String> redirectUris = new ArrayList<>();
        redirectUris.add("https://client.keycloak.org/saml/");
        client.setRedirectUris(redirectUris);
        client.setAttributes(new HashMap<>());
        client.getAttributes().put(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, Boolean.FALSE.toString());
        return client;
    }
}
