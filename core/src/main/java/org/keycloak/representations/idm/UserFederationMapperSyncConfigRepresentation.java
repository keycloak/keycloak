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

package org.keycloak.representations.idm;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserFederationMapperSyncConfigRepresentation {

    private Boolean fedToKeycloakSyncSupported;
    private String fedToKeycloakSyncMessage; // applicable just if fedToKeycloakSyncSupported is true

    private Boolean keycloakToFedSyncSupported;
    private String keycloakToFedSyncMessage; // applicable just if keycloakToFedSyncSupported is true

    public UserFederationMapperSyncConfigRepresentation() {
    }

    public UserFederationMapperSyncConfigRepresentation(boolean fedToKeycloakSyncSupported, String fedToKeycloakSyncMessage,
                                                        boolean keycloakToFedSyncSupported, String keycloakToFedSyncMessage) {
        this.fedToKeycloakSyncSupported = fedToKeycloakSyncSupported;
        this.fedToKeycloakSyncMessage = fedToKeycloakSyncMessage;
        this.keycloakToFedSyncSupported = keycloakToFedSyncSupported;
        this.keycloakToFedSyncMessage = keycloakToFedSyncMessage;
    }

    public Boolean isFedToKeycloakSyncSupported() {
        return fedToKeycloakSyncSupported;
    }

    public void setFedToKeycloakSyncSupported(Boolean fedToKeycloakSyncSupported) {
        this.fedToKeycloakSyncSupported = fedToKeycloakSyncSupported;
    }

    public String getFedToKeycloakSyncMessage() {
        return fedToKeycloakSyncMessage;
    }

    public void setFedToKeycloakSyncMessage(String fedToKeycloakSyncMessage) {
        this.fedToKeycloakSyncMessage = fedToKeycloakSyncMessage;
    }

    public Boolean isKeycloakToFedSyncSupported() {
        return keycloakToFedSyncSupported;
    }

    public void setKeycloakToFedSyncSupported(Boolean keycloakToFedSyncSupported) {
        this.keycloakToFedSyncSupported = keycloakToFedSyncSupported;
    }

    public String getKeycloakToFedSyncMessage() {
        return keycloakToFedSyncMessage;
    }

    public void setKeycloakToFedSyncMessage(String keycloakToFedSyncMessage) {
        this.keycloakToFedSyncMessage = keycloakToFedSyncMessage;
    }
}
