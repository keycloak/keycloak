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
package org.keycloak.tests.utils.matchers;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;

import org.apache.http.HttpResponse;
import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Additional hamcrest matchers
 * @author hmlnarik
 */
public class Matchers {

    /**
     * Matcher on HTTP status code of a {@link Response} instance.
     * @param matcher
     * @return
     */
    public static Matcher<Response> body(Matcher<String> matcher) {
        return new ResponseBodyMatcher(matcher);
    }

    /**
     * Matcher on HTTP body of a {@link Response} instance.
     * @param matcher
     * @return
     */
    public static Matcher<HttpResponse> bodyHC(Matcher<String> matcher) {
        return new HttpResponseBodyMatcher(matcher);
    }

    /**
     * Matcher on HTTP status code of a {@link Response} instance.
     * @param matcher
     * @return
     */
    public static Matcher<Response> statusCode(Matcher<? extends Number> matcher) {
        return new ResponseStatusCodeMatcher(matcher);
    }

    /**
     * Matcher on HTTP status code of a {@link Response} instance (HttpClient variant).
     * @param matcher
     * @return
     */
    public static Matcher<HttpResponse> statusCodeHC(Matcher<? extends Number> matcher) {
        return new HttpResponseStatusCodeMatcher(matcher);
    }

    /**
     * Matches when the HTTP status code of a {@link Response} instance is equal to the given code.
     * @param expectedStatusCode
     * @return
     */
    public static Matcher<Response> statusCodeIs(Response.Status expectedStatusCode) {
        return new ResponseStatusCodeMatcher(org.hamcrest.Matchers.is(expectedStatusCode.getStatusCode()));
    }

    /**
     * Matches when the HTTP status code of a {@link Response} instance is equal to the given code (HttpClient variant).
     * @param expectedStatusCode
     * @return
     */
    public static Matcher<HttpResponse> statusCodeIsHC(Response.Status expectedStatusCode) {
        return new HttpResponseStatusCodeMatcher(org.hamcrest.Matchers.is(expectedStatusCode.getStatusCode()));
    }

    /**
     * Matches when the HTTP status code of a {@link Response} instance is equal to the given code.
     * @param expectedStatusCode
     * @return
     */
    public static Matcher<Response> statusCodeIs(int expectedStatusCode) {
        return new ResponseStatusCodeMatcher(org.hamcrest.Matchers.is(expectedStatusCode));
    }

    /**
     * Matches when the HTTP status code of a {@link Response} instance is equal to the given code (HttpClient variant).
     * @param expectedStatusCode
     * @return
     */
    public static Matcher<HttpResponse> statusCodeIsHC(int expectedStatusCode) {
        return new HttpResponseStatusCodeMatcher(org.hamcrest.Matchers.is(expectedStatusCode));
    }

    /**
     * Matches when the HTTP status code of a {@link Response} instance is equal to the given code.
     * @param matcher
     * @return
     */
    public static <T> Matcher<Response> header(Matcher<Map<String, T>> matcher) {
        return new ResponseHeaderMatcher<>(matcher);
    }

    /**
     * Matches when the SAML status code of a {@link ResponseType} instance is equal to the given code.
     * @param expectedStatus
     * @return
     */
    public static <T> Matcher<SAML2Object> isSamlResponse(JBossSAMLURIConstants expectedStatus) {
        return allOf(
          instanceOf(ResponseType.class),
          new SamlResponseTypeMatcher(is(expectedStatus.getUri()))
        );
    }

    /**
     * Matches when the destination of a SAML {@link LogoutRequestType} instance is equal to the given destination.
     * @param destination
     * @return
     */
    public static <T> Matcher<SAML2Object> isSamlLogoutRequest(String destination) {
        return allOf(
          instanceOf(LogoutRequestType.class),
          new SamlLogoutRequestTypeMatcher(URI.create(destination))
        );
    }
    /**
     * Matches when the type of a SAML object is instance of {@link AuthnRequestType}.
     * @return
     */
    public static <T> Matcher<SAML2Object> isSamlAuthnRequest() {
        return instanceOf(AuthnRequestType.class);
    }

    /**
     * Matches when the SAML status of a {@link StatusResponseType} instance is equal to the given code.
     * @param expectedStatus
     * @return
     */
    public static <T> Matcher<SAML2Object> isSamlStatusResponse(JBossSAMLURIConstants... expectedStatus) {
        return allOf(
          instanceOf(StatusResponseType.class),
          new SamlStatusResponseTypeMatcher(Arrays.stream(expectedStatus).map(JBossSAMLURIConstants::getUri).toArray(i -> new URI[i]))
        );
    }
}
