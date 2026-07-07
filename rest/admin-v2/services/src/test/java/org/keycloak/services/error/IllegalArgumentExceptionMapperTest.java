package org.keycloak.services.error;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.DefaultKeycloakContext;
import org.keycloak.services.DefaultKeycloakSession;
import org.keycloak.services.DefaultKeycloakSessionFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IllegalArgumentExceptionMapperTest {

    private DefaultKeycloakSessionFactory sessionFactory;

    @AfterEach
    void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    void adminApiV2PathMapsToBadRequest() {
        IllegalArgumentExceptionMapper mapper = new IllegalArgumentExceptionMapper();
        try (KeycloakSession session = createSession("/admin/realms/test/admin/api/test/clients")) {
            mapper.session = session;

            Response response = mapper.toResponse(new IllegalArgumentException("bad sort"));

            assertEquals(400, response.getStatus());
            OAuth2ErrorRepresentation error = (OAuth2ErrorRepresentation) response.getEntity();
            assertEquals("bad sort", error.getError());
        }
    }

    @Test
    void nonAdminApiPathMapsToServerError() {
        IllegalArgumentExceptionMapper mapper = new IllegalArgumentExceptionMapper();
        try (KeycloakSession session = createSession("/realms/test/protocol/openid-connect/token")) {
            mapper.session = session;

            Response response = mapper.toResponse(new IllegalArgumentException("internal error"));

            assertEquals(500, response.getStatus());
        }
    }

    @Test
    void isAdminApiV2RequestDetectsAdminApiPath() {
        try (KeycloakSession session = createSession("/admin/realms/master/admin/api/master/clients")) {
            assertTrue(IllegalArgumentExceptionMapper.isAdminApiV2Request(session));
        }
    }

    @Test
    void isAdminApiV2RequestRejectsNonAdminApiPath() {
        try (KeycloakSession session = createSession("/realms/test/protocol/openid-connect/token")) {
            assertFalse(IllegalArgumentExceptionMapper.isAdminApiV2Request(session));
        }
    }

    private KeycloakSession createSession(String requestPath) {
        sessionFactory = new DefaultKeycloakSessionFactory() {
            @Override
            public KeycloakSession create() {
                return new DefaultKeycloakSession(this) {
                    @Override
                    protected DefaultKeycloakContext createKeycloakContext(KeycloakSession session) {
                        return new DefaultKeycloakContext(session) {
                            @Override
                            protected Optional<HttpRequest> createHttpRequest() {
                                return Optional.of(new TestHttpRequest(requestPath));
                            }

                            @Override
                            protected Optional<org.keycloak.http.HttpResponse> createHttpResponse() {
                                return Optional.empty();
                            }
                        };
                    }
                };
            }
        };

        return sessionFactory.create();
    }

    private static final class TestHttpRequest implements HttpRequest {

        private final UriInfo uriInfo;

        private TestHttpRequest(String requestPath) {
            this.uriInfo = new TestUriInfo(requestPath);
        }

        @Override
        public String getHttpMethod() {
            return "GET";
        }

        @Override
        public MultivaluedMap<String, String> getDecodedFormParameters() {
            return new MultivaluedHashMap<>();
        }

        @Override
        public MultivaluedMap<String, org.keycloak.http.FormPartValue> getMultiPartFormParameters() {
            return new MultivaluedHashMap<>();
        }

        @Override
        public HttpHeaders getHttpHeaders() {
            return new HttpHeaders() {
                @Override
                public List<String> getRequestHeader(String name) {
                    return List.of();
                }

                @Override
                public String getHeaderString(String name) {
                    return null;
                }

                @Override
                public MultivaluedMap<String, String> getRequestHeaders() {
                    return new MultivaluedHashMap<>();
                }

                @Override
                public List<MediaType> getAcceptableMediaTypes() {
                    return List.of(MediaType.APPLICATION_JSON_TYPE);
                }

                @Override
                public List<Locale> getAcceptableLanguages() {
                    return List.of();
                }

                @Override
                public MediaType getMediaType() {
                    return null;
                }

                @Override
                public Locale getLanguage() {
                    return null;
                }

                @Override
                public Map<String, jakarta.ws.rs.core.Cookie> getCookies() {
                    return Map.of();
                }

                @Override
                public java.util.Date getDate() {
                    return null;
                }

                @Override
                public int getLength() {
                    return -1;
                }
            };
        }

        @Override
        public java.security.cert.X509Certificate[] getClientCertificateChain() {
            return null;
        }

        @Override
        public UriInfo getUri() {
            return uriInfo;
        }

        @Override
        public boolean isProxyTrusted() {
            return true;
        }
    }

    private static final class TestUriInfo implements UriInfo {

        private final String path;

        private TestUriInfo(String path) {
            this.path = path;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public String getPath(boolean decode) {
            return path;
        }

        @Override
        public List<PathSegment> getPathSegments() {
            return List.of();
        }

        @Override
        public List<PathSegment> getPathSegments(boolean decode) {
            return List.of();
        }

        @Override
        public URI getRequestUri() {
            return URI.create("https://localhost:8080" + path);
        }

        @Override
        public UriBuilder getRequestUriBuilder() {
            return UriBuilder.fromUri(getRequestUri());
        }

        @Override
        public URI getAbsolutePath() {
            return getRequestUri();
        }

        @Override
        public UriBuilder getAbsolutePathBuilder() {
            return UriBuilder.fromUri(getAbsolutePath());
        }

        @Override
        public URI getBaseUri() {
            return URI.create("https://localhost:8080/");
        }

        @Override
        public UriBuilder getBaseUriBuilder() {
            return UriBuilder.fromUri(getBaseUri());
        }

        @Override
        public MultivaluedMap<String, String> getPathParameters() {
            return new MultivaluedHashMap<>();
        }

        @Override
        public MultivaluedMap<String, String> getPathParameters(boolean decode) {
            return new MultivaluedHashMap<>();
        }

        @Override
        public MultivaluedMap<String, String> getQueryParameters() {
            return new MultivaluedHashMap<>();
        }

        @Override
        public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
            return new MultivaluedHashMap<>();
        }

        @Override
        public List<String> getMatchedURIs() {
            return List.of();
        }

        @Override
        public List<String> getMatchedURIs(boolean decode) {
            return List.of();
        }

        @Override
        public List<Object> getMatchedResources() {
            return List.of();
        }

        @Override
        public URI resolve(URI uri) {
            return getBaseUri().resolve(uri);
        }

        @Override
        public URI relativize(URI uri) {
            return getBaseUri().relativize(uri);
        }
    }
}
