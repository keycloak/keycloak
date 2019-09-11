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

import org.keycloak.Config;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.SamlProtocol;

import org.keycloak.protocol.saml.SamlService;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ModAuthMellonClientInstallation implements ClientInstallationProvider {
    @Override
    public Response generateInstallation(KeycloakSession session, RealmModel realm, ClientModel client, URI serverBaseUri) {
        SamlClient samlClient = new SamlClient(client);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(baos);
        String idpDescriptor = SamlService.getIDPMetadataDescriptor(session.getContext().getUri(), session, realm);
        String spDescriptor = SamlSPDescriptorClientInstallation.getSPDescriptorForClient(client);
        String clientDirName = client.getClientId()
                .replace('/', '_')
                .replace(' ', '_');
        try {
            zip.putNextEntry(new ZipEntry(clientDirName + "/idp-metadata.xml"));
            zip.write(idpDescriptor.getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(clientDirName + "/sp-metadata.xml"));
            zip.write(spDescriptor.getBytes());
            zip.closeEntry();
            if (samlClient.requiresClientSignature()) {
                if (samlClient.getClientSigningPrivateKey() != null) {
                    zip.putNextEntry(new ZipEntry(clientDirName + "/client-private-key.pem"));
                    zip.write(samlClient.getClientSigningPrivateKey().getBytes());
                    zip.closeEntry();
                }
                if (samlClient.getClientSigningCertificate() != null) {
                    zip.putNextEntry(new ZipEntry(clientDirName + "/client-cert.pem"));
                    zip.write(samlClient.getClientSigningCertificate().getBytes());
                    zip.closeEntry();
                }
            }
            zip.close();
            baos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        return Response.ok(baos.toByteArray(), getMediaType()).build();
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
}
