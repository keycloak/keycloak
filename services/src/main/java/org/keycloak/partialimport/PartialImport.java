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

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.PartialImportRepresentation;

/**
 * Main interface for PartialImport handlers.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public interface PartialImport<T> {

    /**
     * Find which resources will need to be skipped or overwritten.  Also,
     * do a preliminary check for errors.
     *
     * @param rep Everything in the PartialImport request.
     * @param realm Realm to be imported into.
     * @param session The KeycloakSession.
     * @ If the PartialImport can not be performed,
     *                                throw this exception.
     */
    void prepare(PartialImportRepresentation rep,
                 RealmModel realm,
                 KeycloakSession session);

    /**
     * Delete resources that will be overwritten.  This is done separately so
     * that it can be called for all resource types before calling all the doImports.
     *
     * It was found that doing delete/add per resource causes errors because of
     * cascading deletes.
     *
     * @param realm Realm to be imported into.
     * @param session The KeycloakSession
     */
    void removeOverwrites(RealmModel realm, KeycloakSession session);

    /**
     * Create (or re-create) all the imported resources.
     *
     * @param rep Everything in the PartialImport request.
     * @param realm Realm to be imported into.
     * @param session The KeycloakSession.
     * @return The final results of the PartialImport request.
     * @ if an error was detected trying to doImport a resource.
     */
    PartialImportResults doImport(PartialImportRepresentation rep,
                                  RealmModel realm,
                                  KeycloakSession session);
}
