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

package org.keycloak.services.resources.admin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.KeystoreUtil.KeystoreFormat;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.http.FormPartValue;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.KeyStoreConfig;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.util.CertificateInfoHelper;

import com.google.common.base.Strings;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

/**
 * @resource Client Attribute Certificate
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class ClientAttributeCertificateResource {

    public static final String CERTIFICATE_PEM = "Certificate PEM";
    public static final String PUBLIC_KEY_PEM = "Public Key PEM";
    public static final String JSON_WEB_KEY_SET = "JSON Web Key Set";

    private static final Logger logger = Logger.getLogger(ClientAttributeCertificateResource.class);

    protected final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    protected final ClientModel client;
    protected final KeycloakSession session;
    protected final AdminEventBuilder adminEvent;
    protected final String attributePrefix;

    public ClientAttributeCertificateResource(AdminPermissionEvaluator auth, ClientModel client, KeycloakSession session, String attributePrefix, AdminEventBuilder adminEvent) {
        this.realm = session.getContext().getRealm();
        this.auth = auth;
        this.client = client;
        this.session = session;
        this.attributePrefix = attributePrefix;
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT);
    }

    /**
     * Get key info
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENT_ATTRIBUTE_CERTIFICATE)
    @Operation( summary = "Get key info")
    public CertificateRepresentation getKeyInfo() {
        auth.clients().requireView(client);

        CertificateRepresentation info = CertificateInfoHelper.getCertificateFromClient(client, attributePrefix);
        return info;
    }

    /**
     * Generate a new certificate with new key pair
     *
     * @return
     */
    @POST
    @NoCache
    @Path("generate")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENT_ATTRIBUTE_CERTIFICATE)
    @Operation( summary = "Generate a new certificate with new key pair")
    public CertificateRepresentation generate() {
        auth.clients().requireConfigure(client);

        CertificateRepresentation info = KeycloakModelUtils.generateKeyPairCertificate(client.getClientId());

        CertificateInfoHelper.updateClientModelCertificateInfo(client, info, attributePrefix);

        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(info).success();

        return info;
    }

    /**
     * Upload certificate and eventually private key
     *
     * @return
     * @throws IOException
     */
    @POST
    @Path("upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENT_ATTRIBUTE_CERTIFICATE)
    @Operation( summary = "Upload certificate and eventually private key")
    public CertificateRepresentation uploadJks() throws IOException {
        try {
            CertificateRepresentation info = updateCertFromRequest();
            adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(info).success();
            return info;
        } catch (IllegalStateException ise) {
            throw new ErrorResponseException("certificate-not-found", "Certificate or key with given alias not found in the keystore", Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Upload only certificate, not private key
     *
     * @return information extracted from uploaded certificate - not necessarily the new state of certificate on the server
     * @throws IOException
     */
    @POST
    @Path("upload-certificate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENT_ATTRIBUTE_CERTIFICATE)
    @Operation( summary = "Upload only certificate, not private key")
    public CertificateRepresentation uploadJksCertificate() throws IOException {
        try {
            CertificateRepresentation info = updateCertFromRequest();
            adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(info).success();
            return info;
        } catch (IllegalStateException ise) {
            throw new ErrorResponseException("certificate-not-found", "Certificate or key with given alias not found in the keystore", Response.Status.BAD_REQUEST);
        }
    }

    private CertificateRepresentation updateCertFromRequest() throws IOException {
        auth.clients().requireManage(client);
        CertificateRepresentation info = new CertificateRepresentation();
        MultivaluedMap<String, FormPartValue> uploadForm = session.getContext().getHttpRequest().getMultiPartFormParameters();
        FormPartValue keystoreFormatPart = uploadForm.getFirst("keystoreFormat");
        if (keystoreFormatPart == null) {
            throw new BadRequestException("keystoreFormat cannot be null");
        }
        String keystoreFormat = keystoreFormatPart.asString();
        FormPartValue inputParts = uploadForm.getFirst("file");

        boolean fileEmpty = false;
        try {
            fileEmpty = inputParts == null || Strings.isNullOrEmpty(inputParts.asString());
        } catch (Exception e) {
            // ignore
        }

        if (fileEmpty) {
            throw new BadRequestException("file cannot be empty");
        }

        if (keystoreFormat.equals(CERTIFICATE_PEM)) {
            String pem = StreamUtil.readString(inputParts.asInputStream(), StandardCharsets.UTF_8);
            pem = PemUtils.removeBeginEnd(pem);

            // Validate format
            KeycloakModelUtils.getCertificate(pem);
            info.setCertificate(pem);
            CertificateInfoHelper.updateClientModelCertificateInfo(client, info, attributePrefix);
            return info;
        } else if (keystoreFormat.equals(PUBLIC_KEY_PEM)) {
            String pem = StreamUtil.readString(inputParts.asInputStream(), StandardCharsets.UTF_8);

            // Validate format
            KeycloakModelUtils.getPublicKey(pem);
            info.setPublicKey(pem);
            CertificateInfoHelper.updateClientModelCertificateInfo(client, info, attributePrefix);
            return info;
        } else if (keystoreFormat.equals(JSON_WEB_KEY_SET)) {
            String jwks = StreamUtil.readString(inputParts.asInputStream(), StandardCharsets.UTF_8);

            info = CertificateInfoHelper.jwksStringToSigCertificateRepresentation(jwks);
            // jwks is only valid for OIDC clients
            if (OIDCLoginProtocol.LOGIN_PROTOCOL.equals(client.getProtocol())) {
                CertificateInfoHelper.updateClientModelJwksString(client, attributePrefix, jwks);
            } else {
                CertificateInfoHelper.updateClientModelCertificateInfo(client, info, attributePrefix);
            }
            return info;
        }

        String keyAlias = uploadForm.getFirst("keyAlias").asString();
        FormPartValue keyPasswordPart = uploadForm.getFirst("keyPassword");
        char[] keyPassword = keyPasswordPart != null ? keyPasswordPart.asString().toCharArray() : null;

        FormPartValue storePasswordPart = uploadForm.getFirst("storePassword");
        char[] storePassword = storePasswordPart != null ? storePasswordPart.asString().toCharArray() : null;
        PrivateKey privateKey = null;
        X509Certificate certificate = null;
        try {
            KeyStore keyStore = CryptoIntegration.getProvider().getKeyStore(KeystoreFormat.valueOf(keystoreFormat));
            keyStore.load(inputParts.asInputStream(), storePassword);
            try {
                privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword);
            } catch (Exception e) {
                // ignore
            }
            certificate = (X509Certificate) keyStore.getCertificate(keyAlias);
        } catch (Exception e) {
            logger.error("Error loading keystore", e);
            if (e.getCause() instanceof UnrecoverableKeyException keyException) {
                throw new BadRequestException(keyException.getMessage());
            } else {
                throw new BadRequestException("error loading keystore");
            }
        }

        if (privateKey != null) {
            String privateKeyPem = KeycloakModelUtils.getPemFromKey(privateKey);
            info.setPrivateKey(privateKeyPem);
        }

        if (certificate != null) {
            String certPem = KeycloakModelUtils.getPemFromCertificate(certificate);
            info.setCertificate(certPem);
        }

        CertificateInfoHelper.updateClientModelCertificateInfo(client, info, attributePrefix);
        return info;
    }

    /**
     * Get a keystore file for the client, containing private key and public certificate
     *
     * @param config Keystore configuration as JSON
     * @return
     */
    @POST
    @NoCache
    @Path("/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENT_ATTRIBUTE_CERTIFICATE)
    @Operation( summary = "Get a keystore file for the client, containing private key and public certificate")
    public byte[] getKeystore(@Parameter(description = "Keystore configuration as JSON") final KeyStoreConfig config) {
        auth.clients().requireView(client);

        checkKeystoreFormat(config);

        CertificateRepresentation info = CertificateInfoHelper.getCertificateFromClient(client, attributePrefix);
        String privatePem = info.getPrivateKey();
        String certPem = info.getCertificate();

        if (privatePem == null && certPem == null) {
            throw new NotFoundException("keypair not generated for client");
        }
        if (privatePem != null && config.getKeyPassword() == null) {
            throw new ErrorResponseException("password-missing", "Need to specify a key password for jks download", Response.Status.BAD_REQUEST);
        }
        if (config.getStorePassword() == null) {
            throw new ErrorResponseException("password-missing", "Need to specify a store password for jks download", Response.Status.BAD_REQUEST);
        }

        byte[] rtn = getKeystore(config, privatePem, certPem);
        return rtn;
    }

    /**
     * Generate a new keypair and certificate, and get the private key file
     *
     * Generates a keypair and certificate and serves the private key in a specified keystore format.
     * Only generated public certificate is saved in Keycloak DB - the private key is not.
     *
     * @param config Keystore configuration as JSON
     * @return
     */
    @POST
    @NoCache
    @Path("/generate-and-download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENT_ATTRIBUTE_CERTIFICATE)
    @Operation( summary =
            "Generate a new keypair and certificate, and get the private key file\n" +
                    "\n" +
                    "Generates a keypair and certificate and serves the private key in a specified keystore format.\n" +
                    "Only generated public certificate is saved in Keycloak DB - the private key is not.")
    public byte[] generateAndGetKeystore(@Parameter(description = "Keystore configuration as JSON") final KeyStoreConfig config) {
        auth.clients().requireConfigure(client);

        checkKeystoreFormat(config);
        if (config.getKeyPassword() == null) {
            throw new ErrorResponseException("password-missing", "Need to specify a key password for jks generation and download", Response.Status.BAD_REQUEST);
        }
        if (config.getStorePassword() == null) {
            throw new ErrorResponseException("password-missing", "Need to specify a store password for jks generation and download", Response.Status.BAD_REQUEST);
        }

        int keySize = config.getKeySize() != null && config.getKeySize() > 0
                ? config.getKeySize()
                : KeycloakModelUtils.DEFAULT_RSA_KEY_SIZE;
        int validity = config.getValidity() != null && config.getValidity() > 0
                ? config.getValidity()
                : KeycloakModelUtils.DEFAULT_CERTIFICATE_VALIDITY_YEARS;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, validity);
        CertificateRepresentation info = KeycloakModelUtils.generateKeyPairCertificate(client.getClientId(), keySize, calendar);
        byte[] rtn = getKeystore(config, info.getPrivateKey(), info.getCertificate());

        info.setPrivateKey(null);

        CertificateInfoHelper.updateClientModelCertificateInfo(client, info, attributePrefix);

        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(info).success();
        return rtn;
    }

    private byte[] getKeystore(KeyStoreConfig config, String privatePem, String certPem) {
        try {
            String format = config.getFormat();
            KeyStore keyStore = CryptoIntegration.getProvider().getKeyStore(KeystoreFormat.valueOf(format));
            keyStore.load(null, null);
            String keyAlias = config.getKeyAlias();
            if (keyAlias == null) keyAlias = client.getClientId();
            if (privatePem != null) {
                PrivateKey privateKey = PemUtils.decodePrivateKey(privatePem);
                X509Certificate clientCert = PemUtils.decodeCertificate(certPem);


                Certificate[] chain =  {clientCert};

                keyStore.setKeyEntry(keyAlias, privateKey, config.getKeyPassword().trim().toCharArray(), chain);
            } else {
                X509Certificate clientCert = PemUtils.decodeCertificate(certPem);
                keyStore.setCertificateEntry(keyAlias, clientCert);
            }


            if (config.isRealmCertificate() == null || config.isRealmCertificate().booleanValue()) {
                KeyManager keys = session.keys();
                String kid = keys.getActiveRsaKey(realm).getKid();
                Certificate certificate = keys.getRsaCertificate(realm, kid);
                String certificateAlias = config.getRealmAlias();
                if (certificateAlias == null) certificateAlias = realm.getName();
                keyStore.setCertificateEntry(certificateAlias, certificate);

            }
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            keyStore.store(stream, config.getStorePassword().trim().toCharArray());
            stream.flush();
            stream.close();
            byte[] rtn = stream.toByteArray();
            return rtn;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void checkKeystoreFormat(KeyStoreConfig config) throws NotAcceptableException {
        if (config.getFormat() != null) {
            Set<KeystoreFormat> supportedKeystoreFormats = CryptoIntegration.getProvider().getSupportedKeyStoreTypes()
                    .collect(Collectors.toSet());
            try {
                KeystoreFormat format = Enum.valueOf(KeystoreFormat.class, config.getFormat().toUpperCase());
                if (config.getFormat() != null && !supportedKeystoreFormats.contains(format)) {
                    throw new NotAcceptableException("Not supported keystore format. Supported keystore formats: " + supportedKeystoreFormats);
                }
            } catch (IllegalArgumentException iae) {
                throw new NotAcceptableException("Not supported keystore format. Supported keystore formats: " + supportedKeystoreFormats);
            }
        }
    }


}
