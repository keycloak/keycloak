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

import java.io.IOException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.util.CertificateInfoHelper;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;

/**
 * @resource Client Attribute Certificate
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class ClientAttributeCertificateResource {

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
     * Upload certificate
     *
     * @return
     * @throws IOException
     */
    @POST
    @Path("upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENT_ATTRIBUTE_CERTIFICATE)
    @Operation( summary = "Upload certificate")
    public CertificateRepresentation uploadJks() throws IOException {
        auth.clients().requireConfigure(client);
        try {
            CertificateRepresentation info = CertificateInfoHelper.getCertificateFromRequest(session);
            updateCertFromRequest(info);
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
        auth.clients().requireManage(client);
        try {
            CertificateRepresentation info = CertificateInfoHelper.getCertificateFromRequest(session);
            updateCertFromRequest(info);
            return info;
        } catch (IllegalStateException ise) {
            throw new ErrorResponseException("certificate-not-found", "Certificate or key with given alias not found in the keystore", Response.Status.BAD_REQUEST);
        }
    }

    private void updateCertFromRequest(CertificateRepresentation info) {
        if (OIDCLoginProtocol.LOGIN_PROTOCOL.equals(client.getProtocol()) && info.getJwks() != null) {
            CertificateInfoHelper.updateClientModelJwksString(client, attributePrefix, info.getJwks());
        } else {
            CertificateInfoHelper.updateClientModelCertificateInfo(client, info, attributePrefix);
        }
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(info).success();
    }
}
