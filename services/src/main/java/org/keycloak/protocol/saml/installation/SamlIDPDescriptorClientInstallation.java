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
import org.keycloak.services.resources.RealmsResource;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlIDPDescriptorClientInstallation implements ClientInstallationProvider {
    public static String getIDPDescriptorForClient(RealmModel realm, ClientModel client, URI serverBaseUri) {
        SamlClient samlClient = new SamlClient(client);
        String idpEntityId = RealmsResource.realmBaseUrl(UriBuilder.fromUri(serverBaseUri)).build(realm.getName()).toString();
        String idp = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                 "<EntityDescriptor entityID=\"" + idpEntityId + "\"\n" +
                "                   xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\"\n" +
                "                   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "   <IDPSSODescriptor WantAuthnRequestsSigned=\"" + Boolean.toString(samlClient.requiresClientSignature()) + "\"\n" +
                "      protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n";
        if (samlClient.forceNameIDFormat() && samlClient.getNameIDFormat() != null) {
            idp +=  "   <NameIDFormat>" + samlClient.getNameIDFormat() + "</NameIDFormat>\n";
        } else {
            idp +=  "   <NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:persistent</NameIDFormat>\n" +
                    "   <NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:transient</NameIDFormat>\n" +
                    "   <NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified</NameIDFormat>\n" +
                    "   <NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress</NameIDFormat>\n";
        }
        String bindUrl = RealmsResource.protocolUrl(UriBuilder.fromUri(serverBaseUri)).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL).toString();
        idp +=  "\n" +
                "      <SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"\n" +
                "         Location=\"" + bindUrl + "\" />\n";
        if (!samlClient.forcePostBinding()) {
           idp +=   "      <SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\"\n" +
                    "         Location=\"" + bindUrl + "\" />\n";

        }
        idp +=  "      <SingleLogoutService\n" +
                "         Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"\n" +
                "         Location=\"" + bindUrl + "\" />\n";
        if (!samlClient.forcePostBinding()) {
            idp +=  "      <SingleLogoutService\n" +
                    "         Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\"\n" +
                    "         Location=\"" + bindUrl + "\" />\n";
        }
        idp +=  "      <KeyDescriptor use=\"signing\">\n" +
                "          <dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "              <dsig:X509Data>\n" +
                "                  <dsig:X509Certificate>\n" +
                "                      " + realm.getCertificatePem() + "\n" +
                "                  </dsig:X509Certificate>\n" +
                "              </dsig:X509Data>\n" +
                "          </dsig:KeyInfo>\n" +
                "      </KeyDescriptor>\n" +
                "   </IDPSSODescriptor>\n" +
                "</EntityDescriptor>\n";
        return idp;
    }

    @Override
    public Response generateInstallation(KeycloakSession session, RealmModel realm, ClientModel client, URI serverBaseUri) {
        String descriptor = getIDPDescriptorForClient(realm, client, serverBaseUri);
        return Response.ok(descriptor, MediaType.TEXT_PLAIN_TYPE).build();
    }

    @Override
    public String getProtocol() {
        return SamlProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public String getDisplayType() {
        return "SAML Metadata IDPSSODescriptor";
    }

    @Override
    public String getHelpText() {
        return "SAML Metadata IDSSODescriptor tailored for the client.  This is special because not every client may require things like digital signatures";
    }

    @Override
    public String getFilename() {
        return "client-tailored-saml-idp-metadata.xml";
    }

    public String getMediaType() {
        return MediaType.APPLICATION_XML;
    }

    @Override
    public boolean isDownloadOnly() {
        return false;
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
        return "saml-idp-descriptor";
    }
}
