/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
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

package org.keycloak.partialimport;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.services.ErrorResponse;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public abstract class AbstractPartialImport<T> implements PartialImport {

    public abstract List<T> getRepList(PartialImportRepresentation partialImportRep);
    public abstract String getName(T resourceRep);
    public abstract boolean exists(RealmModel realm, KeycloakSession session, T resourceRep);
    public abstract String existsMessage(T resourceRep);
    public abstract ResourceType getResourceType();
    public abstract void overwrite(RealmModel realm, KeycloakSession session, T resourceRep);
    public abstract void create(RealmModel realm, KeycloakSession session, T resourceRep);

    protected void prepare(PartialImportRepresentation partialImportRep,
                         RealmModel realm,
                         KeycloakSession session,
                         Set<T> resourcesToOverwrite,
                         Set<T> resourcesToSkip) throws ErrorResponseException {
        for (T resourceRep : getRepList(partialImportRep)) {
            if (exists(realm, session, resourceRep)) {
                switch (partialImportRep.getPolicy()) {
                    case SKIP: resourcesToSkip.add(resourceRep); break;
                    case OVERWRITE: resourcesToOverwrite.add(resourceRep); break;
                    default: throw exists(existsMessage(resourceRep));
                }
            }
        }
    }

    protected ErrorResponseException exists(String message) {
        Response error = ErrorResponse.exists(message);
        return new ErrorResponseException(error);
    }

    protected PartialImportResult overwritten(T resourceRep){
        return PartialImportResult.overwritten(getResourceType(), getName(resourceRep), resourceRep);
    }

    protected PartialImportResult skipped(T resourceRep) {
        return PartialImportResult.skipped(getResourceType(), getName(resourceRep), resourceRep);
    }

    protected PartialImportResult added(T resourceRep) {
        return PartialImportResult.added(getResourceType(), getName(resourceRep), resourceRep);
    }

    @Override
    public PartialImportResults doImport(PartialImportRepresentation partialImportRep, RealmModel realm, KeycloakSession session) throws ErrorResponseException {
        PartialImportResults results = new PartialImportResults();
        List<T> repList = getRepList(partialImportRep);
        if ((repList == null) || repList.isEmpty()) return results;

        final Set<T> toOverwrite = new HashSet<>();
        final Set<T> toSkip = new HashSet<>();
        prepare(partialImportRep, realm, session, toOverwrite, toSkip);

        for (T resourceRep: toOverwrite) {
            System.out.println("overwriting " + getResourceType() + " " + getName(resourceRep));
            try {
                overwrite(realm, session, resourceRep);
            } catch (Exception e) {
                throw new ErrorResponseException(ErrorResponse.error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR));
            }

            results.addResult(overwritten(resourceRep));
        }

        for (T resourceRep : toSkip) {
            System.out.println("skipping " + getResourceType() + " " + getName(resourceRep));
            results.addResult(skipped(resourceRep));
        }

        for (T resourceRep : repList) {
            if (toOverwrite.contains(resourceRep)) continue;
            if (toSkip.contains(resourceRep)) continue;

            try {
                System.out.println("adding " + getResourceType() + " " + getName(resourceRep));
                create(realm, session, resourceRep);
                results.addResult(added(resourceRep));
            } catch (Exception e) {
                //e.printStackTrace();
                throw new ErrorResponseException(ErrorResponse.error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR));
            }
        }

        return results;
    }

}
