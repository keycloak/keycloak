/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.admin.client.resource;

import org.keycloak.representations.KeyStoreConfig;
import org.keycloak.representations.idm.CertificateRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public interface ClientAttributeCertificateResource {

    /**
     * Get key info
     *
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    CertificateRepresentation getKeyInfo();

    /**
     * Generate a new certificate with new key pair
     *
     * @return
     */
    @POST
    @Path("generate")
    @Produces(MediaType.APPLICATION_JSON)
    CertificateRepresentation generate();

    /**
     * Upload certificate and eventually private key
     *
     * @param output
     * @return
     */
    @POST
    @Path("upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    CertificateRepresentation uploadJks(Object output);

    /**
     * Upload only certificate, not private key
     *
     * @param output
     * @return
     */
    @POST
    @Path("upload-certificate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    CertificateRepresentation uploadJksCertificate(Object output);

    /**
     * Get a keystore file for the client, containing private key and public certificate
     *
     * @param config Keystore configuration as JSON
     * @return
     */
    @POST
    @Path("/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.APPLICATION_JSON)
    byte[] getKeystore(final KeyStoreConfig config);

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
    @Path("/generate-and-download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.APPLICATION_JSON)
    byte[] generateAndGetKeystore(final KeyStoreConfig config);
}
