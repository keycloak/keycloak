package org.keycloak.services.resources.admin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.KeystoreUtil.KeystoreFormat;
import org.keycloak.common.util.PemUtils;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
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

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
// import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;

@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class ClientAttributeCertificateResource {

    protected final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    protected final ClientModel client;
    protected final KeycloakSession session;
    protected final AdminEventBuilder adminEvent;
    protected final String attributePrefix;

    public ClientAttributeCertificateResource(AdminPermissionEvaluator auth, ClientModel client,
                                             KeycloakSession session, String attributePrefix,
                                             AdminEventBuilder adminEvent) {
        this.realm = session.getContext().getRealm();
        this.auth = auth;
        this.client = client;
        this.session = session;
        this.attributePrefix = attributePrefix;
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT);
    }

    // ---------------- GET ----------------
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENT_ATTRIBUTE_CERTIFICATE)
    @Operation(summary = "Get key info")
    public CertificateRepresentation getKeyInfo() {
        auth.clients().requireView(client);

        CertificateRepresentation info =
                CertificateInfoHelper.getCertificateFromClient(client, attributePrefix);

        info.setPrivateKey(null); // always hide
        return info;
    }

    // ---------------- GENERATE ----------------
    @POST
    @NoCache
    @Path("generate")
    @Produces(MediaType.APPLICATION_JSON)
    public CertificateRepresentation generate() {
        auth.clients().requireConfigure(client);

        CertificateRepresentation info =
                KeycloakModelUtils.generateKeyPairCertificate(client.getClientId());

        CertificateInfoHelper.updateClientModelCertificateInfo(client, info, attributePrefix);

        // ✅ FIX: MUST null before event
        info.setPrivateKey(null);

        adminEvent.operation(OperationType.ACTION)
                .resourcePath(session.getContext().getUri())
                .representation(info)
                .success();

        return info;
    }

    // ---------------- UPLOAD (JKS) ----------------
    @POST
    @Path("upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public CertificateRepresentation uploadJks() throws IOException {
        auth.clients().requireConfigure(client);

        try {
            CertificateRepresentation info =
                    CertificateInfoHelper.getCertificateFromRequest(session);

            info.setPrivateKey(null); // FIX BEFORE ANY SIDE EFFECT

            updateCertFromRequest(info);

            return info;
        } catch (IllegalStateException ise) {
            throw new ErrorResponseException(
                    "certificate-not-found",
                    "Certificate or key with given alias not found in the keystore",
                    Response.Status.BAD_REQUEST
            );
        }
    }

    // ---------------- UPLOAD CERT ONLY ----------------
    @POST
    @Path("upload-certificate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public CertificateRepresentation uploadJksCertificate() throws IOException {
        auth.clients().requireManage(client);

        try {
            CertificateRepresentation info =
                    CertificateInfoHelper.getCertificateFromRequest(session);

            info.setPrivateKey(null); // FIX

            updateCertFromRequest(info);

            return info;
        } catch (IllegalStateException ise) {
            throw new ErrorResponseException(
                    "certificate-not-found",
                    "Certificate or key with given alias not found in the keystore",
                    Response.Status.BAD_REQUEST
            );
        }
    }

    // ---------------- COMMON UPDATE ----------------
    private void updateCertFromRequest(CertificateRepresentation info) {
        if (OIDCLoginProtocol.LOGIN_PROTOCOL.equals(client.getProtocol())
                && info.getJwks() != null) {

            CertificateInfoHelper.updateClientModelJwksString(
                    client, attributePrefix, info.getJwks());

        } else {
            CertificateInfoHelper.updateClientModelCertificateInfo(
                    client, info, attributePrefix);
        }

        adminEvent.operation(OperationType.ACTION)
                .resourcePath(session.getContext().getUri())
                .representation(info)
                .success();
    }

    // ---------------- DOWNLOAD ----------------
    @POST
    @NoCache
    @Path("/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.APPLICATION_JSON)
    public byte[] getKeystore(final KeyStoreConfig config) {
        auth.clients().requireConfigure(client);

        checkKeystoreFormat(config);

        CertificateRepresentation info =
                CertificateInfoHelper.getCertificateFromClient(client, attributePrefix);

        String privatePem = info.getPrivateKey();
        String certPem = info.getCertificate();

        if (privatePem == null && certPem == null) {
            throw new NotFoundException("keypair not generated for client");
        }

        if (privatePem != null && config.getKeyPassword() == null) {
            throw new ErrorResponseException("password-missing",
                    "Need to specify a key password for jks download",
                    Response.Status.BAD_REQUEST);
        }

        if (config.getStorePassword() == null) {
            throw new ErrorResponseException("password-missing",
                    "Need to specify a store password for jks download",
                    Response.Status.BAD_REQUEST);
        }

        return getKeystore(config, privatePem, certPem);
    }

    // ---------------- GENERATE + DOWNLOAD ----------------
    @POST
    @NoCache
    @Path("/generate-and-download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.APPLICATION_JSON)
    public byte[] generateAndGetKeystore(final KeyStoreConfig config) {

        auth.clients().requireConfigure(client);

        checkKeystoreFormat(config);

        if (config.getKeyPassword() == null) {
            throw new ErrorResponseException("password-missing",
                    "Need key password",
                    Response.Status.BAD_REQUEST);
        }

        if (config.getStorePassword() == null) {
            throw new ErrorResponseException("password-missing",
                    "Need store password",
                    Response.Status.BAD_REQUEST);
        }

        int keySize = config.getKeySize() != null && config.getKeySize() > 0
                ? config.getKeySize()
                : KeycloakModelUtils.DEFAULT_RSA_KEY_SIZE;

        int validity = config.getValidity() != null && config.getValidity() > 0
                ? config.getValidity()
                : KeycloakModelUtils.DEFAULT_CERTIFICATE_VALIDITY_YEARS;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, validity);

        CertificateRepresentation info =
                KeycloakModelUtils.generateKeyPairCertificate(
                        client.getClientId(), keySize, calendar);

        byte[] rtn = getKeystore(config, info.getPrivateKey(), info.getCertificate());

        info.setPrivateKey(null); // FIX

        CertificateInfoHelper.updateClientModelCertificateInfo(
                client, info, attributePrefix);

        adminEvent.operation(OperationType.ACTION)
                .resourcePath(session.getContext().getUri())
                .representation(info)
                .success();

        return rtn;
    }

    // ---------------- KEYSTORE ----------------
    private byte[] getKeystore(KeyStoreConfig config, String privatePem, String certPem) {
        try {
            String format = config.getFormat();

            KeyStore keyStore =
                    CryptoIntegration.getProvider()
                            .getKeyStore(KeystoreFormat.valueOf(format));

            keyStore.load(null, null);

            String keyAlias = config.getKeyAlias();
            if (keyAlias == null) keyAlias = client.getClientId();

            if (privatePem != null) {
                PrivateKey privateKey = PemUtils.decodePrivateKey(privatePem);
                X509Certificate cert = PemUtils.decodeCertificate(certPem);

                Certificate[] chain = {cert};

                keyStore.setKeyEntry(
                        keyAlias,
                        privateKey,
                        config.getKeyPassword().trim().toCharArray(),
                        chain
                );
            } else {
                X509Certificate cert = PemUtils.decodeCertificate(certPem);
                keyStore.setCertificateEntry(keyAlias, cert);
            }

            if (config.isRealmCertificate() == null || config.isRealmCertificate()) {
                KeyManager keys = session.keys();
                String kid = keys.getActiveRsaKey(realm).getKid();
                Certificate certificate = keys.getRsaCertificate(realm, kid);

                String alias = config.getRealmAlias();
                if (alias == null) alias = realm.getName();

                keyStore.setCertificateEntry(alias, certificate);
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            keyStore.store(stream, config.getStorePassword().trim().toCharArray());

            return stream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------- FORMAT CHECK ----------------
    private void checkKeystoreFormat(KeyStoreConfig config) {
        if (config.getFormat() != null) {
            Set<KeystoreFormat> supported =
                    CryptoIntegration.getProvider()
                            .getSupportedKeyStoreTypes()
                            .collect(Collectors.toSet());

            try {
                KeystoreFormat format =
                        Enum.valueOf(KeystoreFormat.class, config.getFormat().toUpperCase());

                if (!supported.contains(format)) {
                    throw new NotAcceptableException(
                            "Not supported keystore format: " + supported);
                }

            } catch (IllegalArgumentException iae) {
                throw new NotAcceptableException(
                        "Not supported keystore format: " + supported);
            }
        }
    }
}
