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

package org.keycloak.partialimport;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ServicesLogger;

/**
 * Base PartialImport for most resource types.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public abstract class AbstractPartialImport<T> implements PartialImport<T> {

    protected final Set<T> toOverwrite = new HashSet<>();
    protected final Set<T> toSkip = new HashSet<>();

    public abstract List<T> getRepList(PartialImportRepresentation partialImportRep);
    public abstract String getName(T resourceRep);
    public abstract String getModelId(RealmModel realm, KeycloakSession session, T resourceRep);
    public abstract boolean exists(RealmModel realm, KeycloakSession session, T resourceRep);
    public abstract String existsMessage(RealmModel realm, T resourceRep);
    public abstract ResourceType getResourceType();
    public abstract void remove(RealmModel realm, KeycloakSession session, T resourceRep);
    public abstract void create(RealmModel realm, KeycloakSession session, T resourceRep);

    @Override
    public void prepare(PartialImportRepresentation partialImportRep,
                         RealmModel realm,
                         KeycloakSession session) {
        List<T> repList = getRepList(partialImportRep);
        if ((repList == null) || repList.isEmpty()) return;

        for (T resourceRep : getRepList(partialImportRep)) {
            if (exists(realm, session, resourceRep)) {
                switch (partialImportRep.getPolicy()) {
                    case SKIP: toSkip.add(resourceRep); break;
                    case OVERWRITE: toOverwrite.add(resourceRep); break;
                    default: throw existsError(existsMessage(realm, resourceRep));
                }
            }
        }
    }

    protected ErrorResponseException existsError(String message) {
        throw ErrorResponse.exists(message);
    }

    protected PartialImportResult overwritten(String modelId, T resourceRep){
        return PartialImportResult.overwritten(getResourceType(), getName(resourceRep), modelId, resourceRep);
    }

    protected PartialImportResult skipped(String modelId, T resourceRep) {
        return PartialImportResult.skipped(getResourceType(), getName(resourceRep), modelId, resourceRep);
    }

    protected PartialImportResult added(String modelId, T resourceRep) {
        return PartialImportResult.added(getResourceType(), getName(resourceRep), modelId, resourceRep);
    }

    @Override
    public void removeOverwrites(RealmModel realm, KeycloakSession session) {
        for (T resourceRep : toOverwrite) {
            remove(realm, session, resourceRep);
        }
    }

    @Override
    public PartialImportResults doImport(PartialImportRepresentation partialImportRep, RealmModel realm, KeycloakSession session) {
        PartialImportResults results = new PartialImportResults();
        List<T> repList = getRepList(partialImportRep);
        if ((repList == null) || repList.isEmpty()) return results;

        for (T resourceRep : toOverwrite) {
            try {
                create(realm, session, resourceRep);
            } catch (Exception e) {
                ServicesLogger.LOGGER.overwriteError(e, getName(resourceRep));
                throw e;
            }

            String modelId = getModelId(realm, session, resourceRep);
            results.addResult(overwritten(modelId, resourceRep));
        }

        for (T resourceRep : toSkip) {
            String modelId = getModelId(realm, session, resourceRep);
            results.addResult(skipped(modelId, resourceRep));
        }

        for (T resourceRep : repList) {
            if (toOverwrite.contains(resourceRep)) continue;
            if (toSkip.contains(resourceRep)) continue;

            try {
                create(realm, session, resourceRep);
                String modelId = getModelId(realm, session, resourceRep);
                results.addResult(added(modelId, resourceRep));
            } catch (Exception e) {
                ServicesLogger.LOGGER.creationError(e, getName(resourceRep));
                throw e;
            }
        }

        return results;
    }

}
