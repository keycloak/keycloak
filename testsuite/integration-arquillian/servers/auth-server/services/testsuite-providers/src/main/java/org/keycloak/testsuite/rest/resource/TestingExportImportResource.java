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

package org.keycloak.testsuite.rest.resource;

import java.nio.file.Files;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.exportimport.Strategy;
import org.keycloak.models.KeycloakSession;

import static org.keycloak.exportimport.ExportImportConfig.ACTION;
import static org.keycloak.exportimport.ExportImportConfig.DEFAULT_USERS_PER_FILE;
import static org.keycloak.exportimport.ExportImportConfig.DIR;
import static org.keycloak.exportimport.ExportImportConfig.FILE;
import static org.keycloak.exportimport.ExportImportConfig.PROVIDER;
import static org.keycloak.exportimport.ExportImportConfig.REALM_NAME;
import static org.keycloak.exportimport.ExportImportConfig.STRATEGY;
import static org.keycloak.exportimport.ExportImportConfig.USERS_PER_FILE;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TestingExportImportResource {

    private final KeycloakSession session;
    private static String tempDir;

    public TestingExportImportResource(KeycloakSession session) {
        this.session = session;
    }

    @GET
    @Path("/run-import")
    @Produces(MediaType.APPLICATION_JSON)
    public Response runImport() {
        new ExportImportManager(session).runImport();
        return Response.noContent().build();
    }

    @GET
    @Path("/run-export")
    @Produces(MediaType.APPLICATION_JSON)
    public Response runExport() {
        new ExportImportManager(session).runExport();
        return Response.noContent().build();
    }

    @GET
    @Path("/get-users-per-file")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Integer getUsersPerFile() {
        String usersPerFile = System.getProperty(USERS_PER_FILE, String.valueOf(DEFAULT_USERS_PER_FILE));
        return Integer.parseInt(usersPerFile.trim());
    }

    @PUT
    @Path("/set-users-per-file")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setUsersPerFile(@QueryParam("usersPerFile") Integer usersPerFile) {
        System.setProperty(USERS_PER_FILE, String.valueOf(usersPerFile));
    }

    @GET
    @Path("/get-dir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getDir() {
        return System.getProperty(DIR);
    }

    @PUT
    @Path("/set-dir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String setDir(@QueryParam("dir") String dir) {
        return System.setProperty(DIR, dir);
    }

    @PUT
    @Path("/set-import-strategy")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setStrategy(@QueryParam("importStrategy") Strategy strategy) {
        System.setProperty(STRATEGY, strategy.name());
    }

    @PUT
    @Path("/export-import-provider")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setProvider(@QueryParam("exportImportProvider") String exportImportProvider) {
        System.setProperty(PROVIDER, exportImportProvider);
    }

    @PUT
    @Path("/export-import-file")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setFile(@QueryParam("file") String file) {
        System.setProperty(FILE, file);
    }

    @PUT
    @Path("/export-import-action")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setAction(@QueryParam("exportImportAction") String exportImportAction) {
        System.setProperty(ACTION, exportImportAction);
    }

    @PUT
    @Path("/set-realm-name")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setRealmName(@QueryParam("realmName") String realmName) {
        if (realmName != null && !realmName.isEmpty()) {
            System.setProperty(REALM_NAME, realmName);
        } else {
            System.getProperties().remove(REALM_NAME);
        }
    }

    @GET
    @Path("/get-test-dir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getExportImportTestDirectory() throws Exception {
        if (tempDir == null) {
            tempDir = Files.createTempDirectory("kc-tests").toAbsolutePath().toString();
        }
        return tempDir;
    }

    @GET
    @Path("/clear")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clear() {
        System.clearProperty(REALM_NAME);
        System.clearProperty(PROVIDER);
        System.clearProperty(ACTION);
        System.clearProperty(FILE);

        return Response.noContent().build();
    }
}
