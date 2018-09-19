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

package org.keycloak.testsuite.client.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.keycloak.exportimport.Strategy;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface TestingExportImportResource {

    @GET
    @Path("/run-import")
    @Produces(MediaType.APPLICATION_JSON)
    void runImport();

    @GET
    @Path("/run-export")
    @Produces(MediaType.APPLICATION_JSON)
    void runExport();

    @GET
    @Path("/get-users-per-file")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Integer getUsersPerFile();

    @PUT
    @Path("/set-users-per-file")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setUsersPerFile(@QueryParam("usersPerFile") Integer usersPerFile);

    @GET
    @Path("/get-dir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getDir();

    @PUT
    @Path("/set-dir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String setDir(@QueryParam("dir") String dir);

    @PUT
    @Path("/set-import-strategy")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setStrategy(@QueryParam("importStrategy") Strategy strategy);

    @PUT
    @Path("/export-import-provider")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setProvider(@QueryParam("exportImportProvider") String exportImportProvider);

    @PUT
    @Path("/export-import-file")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setFile(@QueryParam("file") String file);

    @PUT
    @Path("/export-import-action")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setAction(@QueryParam("exportImportAction") String exportImportAction);

    @PUT
    @Path("/set-realm-name")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setRealmName(@QueryParam("realmName") String realmName);

    @GET
    @Path("/get-test-dir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getExportImportTestDirectory();

    @GET
    @Path("/clear")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clear();

}
