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

package org.keycloak.services.resources.admin;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.URI;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.net.ssl.SSLException;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.oidc.ClientCertificates;
import org.keycloak.broker.oidc.ClientCertificates.ClientCertificateKey;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ClientCertificateRepresentation;
import org.keycloak.representations.idm.ClientCertificateTestRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class IdentityProviderMtlsResource {

    private static final Logger logger = Logger.getLogger(IdentityProviderMtlsResource.class);

    private static final String EKU_CLIENT_AUTH = "1.3.6.1.5.5.7.3.2";
    private static final String EKU_ANY = "2.5.29.37.0";
    // the index of digitalSignature in X509Certificate#getKeyUsage()
    private static final int KU_DIGITAL_SIGNATURE = 0;

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;

    public IdentityProviderMtlsResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
    }

    /**
     * List the client certificates an identity provider can use
     */
    @GET
    @Path("client-certificates")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation(summary = "List the client certificates that an identity provider can present")
    public Stream<ClientCertificateRepresentation> getClientCertificates() {
        auth.realm().requireViewIdentityProviders();

        return ClientCertificates.eligibleCertificates(session, realm)
                .map(this::toRepresentation)
                .sorted(Comparator.comparing(ClientCertificateRepresentation::getProviderName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    }

    /**
     * Test the client certificate by sending a token request with an invalid code to the token endpoint
     *
     * The identity provider authenticates the client before it checks the code, so an invalid_grant error means
     * that the client certificate was accepted. If the same request without the certificate also gets invalid_grant
     * then the certificate wasn't required for authentication.
     * Responds with 204 when it was accepted, 400 when it was rejected or not required and 200 when the response
     * did not tell.
     */
    @POST
    @Path("test")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation(summary = "Test the client certificate with the token endpoint of an identity provider")
    public Response testClientCertificate(ClientCertificateTestRepresentation request) {
        auth.realm().requireManageIdentityProviders();
        if (request == null) {
            throw new BadRequestException();
        }

        String url = StringUtil.isNotBlank(request.getMtlsTokenUrl()) ? request.getMtlsTokenUrl() : request.getTokenUrl();
        if (!isHttps(url)) {
            throw ErrorResponse.error("The token endpoint URL must use HTTPS. Check the Token URL, or the mutual TLS token URL if it is set.", Response.Status.BAD_REQUEST);
        }
        if (StringUtil.isBlank(request.getClientId())) {
            throw ErrorResponse.error("The client ID is required.", Response.Status.BAD_REQUEST);
        }

        ClientCertificateKey certificate;
        try {
            certificate = ClientCertificates.findEnabled(session, realm, request.getKeyProviderId());
        } catch (IllegalArgumentException e) {
            throw ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }

        TokenResponse response;
        try {
            response = sendTokenRequest(SimpleHttp.create(session, certificate), url, request.getClientId());
        } catch (IOException | RuntimeException e) {
            // the cause is not returned as it can reveal internal addresses
            logger.debug("Failed to test the client certificate", e);
            throw ErrorResponse.error(failureMessage(e, certificate), Response.Status.BAD_REQUEST);
        }

        if (OAuthErrorException.INVALID_GRANT.equals(response.error())) {
            return checkCertificateRequired(url, request.getClientId());
        }
        if (isClientRejected(response)) {
            throw ErrorResponse.error(withProblems("The identity provider rejected the client certificate. Check that it trusts the certificate and that the client ID and certificate settings match.", certificate), Response.Status.BAD_REQUEST);
        }
        return inconclusive();
    }

    // a public client also gets invalid_grant so the same request must be rejected without the certificate
    private Response checkCertificateRequired(String url, String clientId) {
        TokenResponse response;
        try {
            response = sendTokenRequest(SimpleHttp.create(session), url, clientId);
        } catch (IOException | RuntimeException e) {
            // an endpoint that requires a client certificate can fail the handshake
            return hasCause(e, SSLException.class) ? Response.noContent().build() : inconclusive();
        }
        if (OAuthErrorException.INVALID_GRANT.equals(response.error())) {
            throw ErrorResponse.error("The identity provider accepted the request without the client certificate so it does not check it. Check that its client is confidential and uses tls_client_auth.", Response.Status.BAD_REQUEST);
        }
        return isClientRejected(response) ? Response.noContent().build() : inconclusive();
    }

    private static boolean isClientRejected(TokenResponse response) {
        return OAuthErrorException.INVALID_CLIENT.equals(response.error()) || response.status() == Response.Status.UNAUTHORIZED.getStatusCode();
    }

    private static Response inconclusive() {
        return Response.ok(Map.of("authenticated", false)).build();
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    private record TokenResponse(int status, String error) {
    }

    private static TokenResponse sendTokenRequest(SimpleHttp http, String url, String clientId) throws IOException {
        try (SimpleHttpResponse response = http.doPost(url)
                .param(OAuth2Constants.CLIENT_ID, clientId)
                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE)
                .param(OAuth2Constants.CODE, KeycloakModelUtils.generateId())
                .acceptJson()
                .asResponse()) {
            return new TokenResponse(response.getStatus(), readError(response));
        }
    }

    private static String readError(SimpleHttpResponse response) {
        try {
            JsonNode error = response.asJson().get(OAuth2Constants.ERROR);
            return error == null ? null : error.asText();
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static boolean isHttps(String url) {
        if (StringUtil.isBlank(url)) {
            return false;
        }
        try {
            URI uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && StringUtil.isNotBlank(uri.getHost());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String withProblems(String message, ClientCertificateKey certificate) {
        return Stream.concat(Stream.of(message), problems(certificate.getCertificate()).stream()).collect(Collectors.joining(" "));
    }

    private static String failureMessage(Throwable failure, ClientCertificateKey certificate) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            // TimeoutException is thrown by the Vert.x client
            if (t instanceof InterruptedIOException || t instanceof TimeoutException) {
                return "The token endpoint timed out.";
            }
            if (t instanceof SSLException) {
                return withProblems("The TLS handshake failed. The identity provider may not trust the client certificate.", certificate);
            }
            // a connection dropped during the handshake is a plain SocketException, not an SSLException
            if (t.getClass() == SocketException.class) {
                return withProblems("The identity provider closed the connection during the TLS handshake. It may not trust the client certificate.", certificate);
            }
        }
        return "Could not connect to the token endpoint.";
    }

    // an unchanged reference is not checked so a deleted key provider does not block other changes
    static void validateOnSave(KeycloakSession session, RealmModel realm, IdentityProviderModel previous, IdentityProviderModel updated) {
        if (!usesClientCertificate(updated)) {
            return;
        }
        String keyProviderId = keyProviderId(updated);
        if (previous != null && usesClientCertificate(previous) && Objects.equals(keyProviderId, keyProviderId(previous))) {
            return;
        }
        ClientCertificates.find(session, realm, keyProviderId);
    }

    private static boolean usesClientCertificate(IdentityProviderModel model) {
        return model.getConfig() != null && new OAuth2IdentityProviderConfig(model).isTlsClientAuthentication();
    }

    private static String keyProviderId(IdentityProviderModel model) {
        return new OAuth2IdentityProviderConfig(model).getClientCertificateKeyProviderId();
    }

    private ClientCertificateRepresentation toRepresentation(ClientCertificateKey clientCertificate) {
        KeyWrapper key = clientCertificate.getKey();
        X509Certificate certificate = clientCertificate.getCertificate();
        ClientCertificateRepresentation rep = new ClientCertificateRepresentation();
        rep.setProviderId(key.getProviderId());
        ComponentModel component = realm.getComponent(key.getProviderId());
        rep.setProviderName(component != null ? component.getName() : null);
        rep.setKid(key.getKid());
        rep.setStatus(key.getStatus().name());
        rep.setType(key.getType());
        rep.setAlgorithm(key.getAlgorithm());
        rep.setUse(key.getUse());
        rep.setSubjectDn(certificate.getSubjectX500Principal().getName());
        rep.setIssuerDn(certificate.getIssuerX500Principal().getName());
        rep.setSerialNumber(certificate.getSerialNumber().toString(16).toUpperCase());
        rep.setValidFrom(certificate.getNotBefore() != null ? certificate.getNotBefore().getTime() : null);
        rep.setValidTo(certificate.getNotAfter() != null ? certificate.getNotAfter().getTime() : null);
        rep.setFingerprintSha256(fingerprintSha256(certificate));
        return rep;
    }

    /**
     * @return uppercase hex with colons like {@code openssl x509 -fingerprint -sha256}
     */
    static String fingerprintSha256(X509Certificate certificate) {
        try {
            return HexFormat.ofDelimiter(":").withUpperCase().formatHex(HashUtils.hash(JavaAlgorithm.SHA256, certificate.getEncoded()));
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException("Unable to calculate the SHA-256 fingerprint of the certificate", e);
        }
    }

    static List<String> problems(X509Certificate certificate) {
        long now = Time.currentTimeMillis();
        List<String> problems = new ArrayList<>();
        if (certificate.getNotAfter().getTime() < now) {
            problems.add("The certificate has expired.");
        } else if (certificate.getNotBefore().getTime() > now) {
            problems.add("The certificate is not valid yet.");
        }
        try {
            problems.addAll(usageProblems(certificate.getKeyUsage(), certificate.getExtendedKeyUsage()));
        } catch (CertificateParsingException ignored) {
        }
        return problems;
    }

    /**
     * A missing key usage or extended key usage extension allows any purpose.
     */
    static List<String> usageProblems(boolean[] keyUsage, List<String> extendedKeyUsage) {
        List<String> problems = new ArrayList<>();
        if (extendedKeyUsage != null && !extendedKeyUsage.contains(EKU_CLIENT_AUTH) && !extendedKeyUsage.contains(EKU_ANY)) {
            problems.add("The extended key usage of the certificate does not include client authentication.");
        }
        if (keyUsage != null && (keyUsage.length <= KU_DIGITAL_SIGNATURE || !keyUsage[KU_DIGITAL_SIGNATURE])) {
            problems.add("The key usage of the certificate does not include digital signature.");
        }
        return problems;
    }
}
