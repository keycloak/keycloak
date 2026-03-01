/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.util.DPoPUtil;

import static jakarta.ws.rs.core.HttpHeaders.WWW_AUTHENTICATE;

import static org.keycloak.OAuth2Constants.ALGS_ATTRIBUTE;
import static org.keycloak.services.util.DPoPUtil.DPOP_SCHEME;

/**
 * @author <a href="mailto:dmitryt@backbase.com">Dmitry Telegin</a>
 */
public class OAuth2Error {

    private static final Map<Response.Status, Class<? extends WebApplicationException>> STATUS_MAP = new HashMap<>();

    private KeycloakSession session;
    private RealmModel realm;
    private String authScheme;
    private String error;
    private String errorDescription;
    private Optional<Cors> cors = Optional.empty();

    private Class<? extends WebApplicationException> clazz;
    private Response.Status status;
    private boolean json = true;

    static {
        STATUS_MAP.put(Response.Status.BAD_REQUEST, BadRequestException.class);
        STATUS_MAP.put(Response.Status.UNAUTHORIZED, NotAuthorizedException.class);
        STATUS_MAP.put(Response.Status.FORBIDDEN, ForbiddenException.class);
        STATUS_MAP.put(Response.Status.INTERNAL_SERVER_ERROR, InternalServerErrorException.class);
    }

    public OAuth2Error session(KeycloakSession session) {
        this.session = session;
        return this;
    }

    public OAuth2Error realm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    public OAuth2Error authScheme(String authScheme) {
        this.authScheme = authScheme;
        return this;
    }

    public OAuth2Error error(String error) {

        this.error = error;

        switch (error) {
            case OAuthErrorException.INVALID_GRANT:
            case OAuthErrorException.INVALID_REQUEST:
            case OAuthErrorException.UNAUTHORIZED_CLIENT:
            case OAuthErrorException.UNSUPPORTED_GRANT_TYPE:
            case OAuthErrorException.INVALID_SCOPE:
                status = Response.Status.BAD_REQUEST;
                break;
            case OAuthErrorException.INVALID_CLIENT:
            case OAuthErrorException.INVALID_TOKEN:
                status = Response.Status.UNAUTHORIZED;
                break;
            case OAuthErrorException.INSUFFICIENT_SCOPE:
                status = Response.Status.FORBIDDEN;
                break;
            case OAuthErrorException.SERVER_ERROR:
                status = Response.Status.INTERNAL_SERVER_ERROR;
                break;
            default:
                throw new IllegalArgumentException("Unrecognized OAuth 2.0 error: " + error);
        }

        return this;
    }

    public OAuth2Error errorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
        return this;
    }

    public OAuth2Error cors(Cors cors) {
        this.cors = Optional.ofNullable(cors);
        return this;
    }

    public OAuth2Error status(Response.Status status) {
        this.status = status;
        return this;
    }

    public OAuth2Error json(boolean json) {
        this.json = json;
        return this;
    }

    public WebApplicationException build() {
        clazz = STATUS_MAP.getOrDefault(status, WebApplicationException.class);
        Response.ResponseBuilder builder = Response.status(status);

        try {
            Constructor<? extends WebApplicationException> constructor = clazz.getConstructor(new Class[] { Response.class });

            if (json) {
                OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(error, errorDescription);
                builder.entity(errorRep).type(MediaType.APPLICATION_JSON_TYPE);
            } else {
                WWWAuthenticate.BearerChallenge bearer = DPOP_SCHEME.equals(this.authScheme) ? new WWWAuthenticate.DPoPChallenge(session) : new WWWAuthenticate.BearerChallenge();
                bearer.setRealm(realm.getName());
                bearer.setError(error);
                bearer.setErrorDescription(errorDescription);
                WWWAuthenticate wwwAuthenticate = new WWWAuthenticate(bearer);
                wwwAuthenticate.build(builder::header);
                cors.ifPresent(_cors -> _cors.exposedHeaders(WWW_AUTHENTICATE));
                builder.entity("").type(MediaType.TEXT_PLAIN_UTF_8_TYPE);
            }
            cors.ifPresent(Cors::add);

            return constructor.newInstance(builder.build());
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new InternalServerErrorException(ex);
        }
    }

    public WebApplicationException insufficientScope(String errorDescription) {
        return this.error(OAuthErrorException.INSUFFICIENT_SCOPE).errorDescription(errorDescription).build();
    }

    public WebApplicationException invalidToken(String errorDescription) {
        return this.error(OAuthErrorException.INVALID_TOKEN).errorDescription(errorDescription).build();
    }

    public WebApplicationException invalidRequest(String errorDescription) {
        return this.error(OAuthErrorException.INVALID_REQUEST).errorDescription(errorDescription).build();
    }

    public WebApplicationException unauthorized() {
        return this.status(Response.Status.UNAUTHORIZED).build();
    }

    private static class WWWAuthenticate {

        private final List<Challenge> challenges;
        private Challenge master;
        private boolean singleHeader = true;

        public WWWAuthenticate(Challenge challenge, Challenge... moreChallenges) {
            challenges = new ArrayList<>(1 + ((moreChallenges == null) ? 0 : moreChallenges.length));
            challenges.add(challenge);
            if (moreChallenges != null) {
                challenges.addAll(Arrays.asList(moreChallenges));
            }
            master = challenge;
        }

        public void addChallenge(Challenge challenge) {
            challenges.add(challenge);
        }

        public void setMasterChallenge(Challenge challenge) {
            if (challenges.contains(challenge)) {
                master = challenge;
            } else {
                throw new IllegalArgumentException("Unknown challenge: " + challenge);
            }
        }

        public void setMasterChallenge(String scheme) {
            master = challenges.stream()
                .filter(c -> c.getScheme().equals(scheme))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown challenge: " + scheme));
        }

        public Challenge getMasterChallenge() {
            return master;
        }

        public boolean isSingleHeader() {
            return singleHeader;
        }

        public void setSingleHeader(boolean singleHeader) {
            this.singleHeader = singleHeader;
        }

        public void setAttribute(String attribute, String value) {
            challenges.forEach(c -> c.setAttribute(attribute, value));
        }

        public void build(BiConsumer<String, Object> addHeader) {
            if (singleHeader) {
                String header = challenges.stream()
                    .map(Challenge::toString)
                    .collect(Collectors.joining(", "));
                addHeader.accept(WWW_AUTHENTICATE, header);
            } else {
                challenges.forEach(c -> addHeader.accept(WWW_AUTHENTICATE, c));
            }
        }

        public static abstract class Challenge {

            private final Map<String, String> attributes = new LinkedHashMap<>();

            public void setAttribute(String attribute, String value) {
                if (value != null) {
                    attributes.put(attribute, value);
                }
            }

            public abstract String getScheme();

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder(getScheme());

                if (!attributes.isEmpty()) {
                    sb.append(" ").append(
                        attributes.entrySet().stream()
                            .map(e -> String.format("%s=\"%s\"", e.getKey(), e.getValue()))
                            .collect(Collectors.joining(", "))
                    );
                }

                return sb.toString();
            }

        }

        public static class BasicChallenge extends Challenge {

            private static final String BASIC_SCHEME = "Basic";
            private static final String REALM_ATTRIBUTE = "realm";

            public void setRealm(String realm) {
                setAttribute(REALM_ATTRIBUTE, realm);
            }

            @Override
            public String getScheme() {
                return BASIC_SCHEME;
            }

        }

        public static class BearerChallenge extends BasicChallenge {

            private static final String BEARER_SCHEME = "Bearer";

            private static final String ERROR_ATTRIBUTE = "error";
            private static final String ERROR_DESCRIPTION_ATTRIBUTE = "error_description";
            private static final String ERROR_URI_ATTRIBUTE = "error_uri";
            private static final String SCOPE_ATTRIBUTE = "scope";

            public void setError(String error) {
                setAttribute(ERROR_ATTRIBUTE, error);
            }

            public void setErrorDescription(String errorDescription) {
                setAttribute(ERROR_DESCRIPTION_ATTRIBUTE, errorDescription);
            }

            public void setErrorUri(String errorUri) {
                setAttribute(ERROR_URI_ATTRIBUTE, errorUri);
            }

            public void setScope(String scope) {
                setAttribute(SCOPE_ATTRIBUTE, scope);
            }

            @Override
            public String getScheme() {
                return BEARER_SCHEME;
            }

        }

        public static class DPoPChallenge extends BearerChallenge {

            public DPoPChallenge(KeycloakSession session) {
                List<String> dpopAlgs = DPoPUtil.getDPoPSupportedAlgorithms(session);
                dpopAlgs.stream()
                        .reduce((str1, current) -> str1 + " " + current)
                        .ifPresent(algs -> setAttribute(ALGS_ATTRIBUTE, algs));
            }

            @Override
            public String getScheme() {
                return DPOP_SCHEME;
            }

        }

    }

}
