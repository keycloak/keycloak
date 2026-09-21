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

package org.keycloak.protocol.saml.installation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.common.util.PemUtils;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlService;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.TransformerUtil;

import org.jboss.logging.Logger;
import org.w3c.dom.Document;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ModAuthMellonClientInstallation implements ClientInstallationProvider {

    private static final Logger logger = Logger.getLogger(ModAuthMellonClientInstallation.class);

    @Override
    public Response generateInstallation(KeycloakSession session, RealmModel realm, ClientModel client, URI serverBaseUri) {
        SamlClient samlClient = new SamlClient(client);
        String idpDescriptor = SamlService.getIDPMetadataDescriptor(session.getContext().getUri(), session, realm);
        String spDescriptor = SamlSPDescriptorClientInstallation.getSPDescriptorForClient(client);
        String clientDirName = client.getClientId()
                .replace('/', '_')
                .replace(' ', '_');
        String clientSigningPrivateKey = null;
        String clientSigningCertificate = null;
        if (samlClient.requiresClientSignature()) {
            clientSigningPrivateKey = samlClient.getClientSigningPrivateKey();
            clientSigningCertificate = samlClient.getClientSigningCertificate();
        }
        byte[] zip = createZip(clientDirName, idpDescriptor, spDescriptor, clientSigningPrivateKey, clientSigningCertificate);

        return Response.ok(zip, getMediaType()).build();
    }

    static byte[] createZip(String clientDirName, String idpDescriptor, String spDescriptor,
            String clientSigningPrivateKey, String clientSigningCertificate) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            zip.putNextEntry(new ZipEntry(clientDirName + "/idp-metadata.xml"));
            zip.write(prettyPrintXml(idpDescriptor).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(clientDirName + "/sp-metadata.xml"));
            zip.write(prettyPrintXml(spDescriptor).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            if (clientSigningPrivateKey != null) {
                zip.putNextEntry(new ZipEntry(clientDirName + "/client-private-key.pem"));
                zip.write(createClientSigningPrivateKeyRfc7468Representation(clientSigningPrivateKey));
                zip.closeEntry();
            }
            if (clientSigningCertificate != null) {
                zip.putNextEntry(new ZipEntry(clientDirName + "/client-cert.pem"));
                zip.write(createClientSigningCertificateRfc7468Representation(clientSigningCertificate));
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    /**
     * Indents the given XML document to make it human-readable. Signed documents are returned unchanged,
     * as adding whitespace would invalidate the signature.
     */
    static String prettyPrintXml(String xml) {
        if (xml == null || xml.isEmpty()) {
            return xml;
        }
        try {
            Document document = DocumentUtil.getDocument(xml);
            if (document.getElementsByTagNameNS(JBossSAMLURIConstants.XMLDSIG_NSURI.get(), "Signature").getLength() > 0) {
                return xml;
            }
            Transformer transformer = TransformerUtil.getTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            StringWriter out = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(out));
            return out.toString();
        } catch (Exception e) {
            logger.warn("Cannot pretty-print XML, using it unformatted", e);
            return xml;
        }
    }

    @Override
    public String getProtocol() {
        return SamlProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public String getDisplayType() {
        return "Mod Auth Mellon files";
    }

    @Override
    public String getHelpText() {
        return "This is a zip file.  It contains a SAML SP descriptor, SAML IDP descriptor,  private key pem, and certificate pem that you will use to configure mod_auth_mellon for Apache.  You'll use these files when crafting the main Apache configuration file.  See mod_auth_mellon website for more details.";
    }

    @Override
    public String getFilename() {
        return "keycloak-mod-auth-mellon-sp-config.zip";
    }

    @Override
    public String getMediaType() {
        return "application/zip";
    }

    @Override
    public boolean isDownloadOnly() {
        return true;
    }

    @Override
    public void close() {

    }

    @Override
    public ClientInstallationProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return "mod-auth-mellon";
    }

    private static byte[] createClientSigningPrivateKeyRfc7468Representation(String clientSigningPrivateKey) {
        String resultAsString = PemUtils.addPrivateKeyBeginEnd(wrapAt64Chars(clientSigningPrivateKey));
        return resultAsString.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] createClientSigningCertificateRfc7468Representation(String clientSigningCertificate) {
        String resultAsString = PemUtils.addCertificateBeginEnd(wrapAt64Chars(clientSigningCertificate));
        return resultAsString.getBytes(StandardCharsets.US_ASCII);
    }

    private static String wrapAt64Chars(String text) {
        return Pattern.compile(".{1,64}")
                .matcher(text)
                .results()
                .map(MatchResult::group)
                .collect(Collectors.joining("\n"));
    }
}
