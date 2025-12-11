/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.operator.controllers;

import io.quarkiverse.operatorsdk.annotations.CSVMetadata;
import io.quarkiverse.operatorsdk.annotations.SharedCSVMetadata;

@CSVMetadata(
    version = "KCOP_NEXT",
    name = "keycloak-operator",
    replaces = "keycloak-operator.KCOP_PREVIOUS",
    displayName = "Keycloak Operator",
    provider = @CSVMetadata.Provider(
        name = "Red Hat"
    ),
    maturity = "stable",
    keywords = {
        "Keycloak",
        "Identity",
        "Access"
    },
    maintainers = {
        @CSVMetadata.Maintainer(
            email = "keycloak-dev@googlegroups.com",
            name = "Keycloak DEV mailing list"
        )
    },
    links = {
        @CSVMetadata.Link(
            url = "https://www.keycloak.org/guides#operator",
            name = "Documentation"
        ),
        @CSVMetadata.Link(
            url = "https://www.keycloak.org/",
            name = "Keycloak"
        ),
        @CSVMetadata.Link(
            url = "https://keycloak.discourse.group/",
            name = "Keycloak Discourse"
        )
    },
    installModes = {
        @CSVMetadata.InstallMode(
            type = "OwnNamespace",
            supported = true
        ),
        @CSVMetadata.InstallMode(
            type = "SingleNamespace",
            supported = true
        ),
        @CSVMetadata.InstallMode(
            type = "MultiNamespace",
            supported = false
        ),
        @CSVMetadata.InstallMode(
            type = "AllNamespaces",
            supported = false
        )
    },
    annotations = @CSVMetadata.Annotations(
        containerImage = "KCOP_IMAGE_PULL_URL:KCOP_NEXT",
        repository = "https://github.com/keycloak/keycloak",
        capabilities = "Deep Insights",
        categories = "Security",
        certified = false,
        almExamples =
            // language=JSON
            """
                [
                  {
                    "apiVersion": "k8s.keycloak.org/v2alpha1",
                    "kind": "Keycloak",
                    "metadata": {
                      "name": "example-keycloak",
                      "labels": {
                        "app": "sso"
                      }
                    },
                    "spec": {
                      "instances": 1,
                      "hostname": {
                        "hostname": "example.org"
                      },
                      "http": {
                        "tlsSecret": "my-tls-secret"
                      }
                    }
                  },
                  {
                    "apiVersion": "k8s.keycloak.org/v2alpha1",
                    "kind": "KeycloakRealmImport",
                    "metadata": {
                      "name": "example-keycloak-realm-import",
                      "labels": {
                        "app": "sso"
                      }
                    },
                    "spec": {
                      "keycloakCRName": "example-keycloak",
                      "realm": {}
                    }
                  }
                ]""",
        others = {
            @CSVMetadata.Annotations.Annotation(
                name = "support",
                value = "Red Hat"
            ),
            @CSVMetadata.Annotations.Annotation(
                name = "description",
                value = "An Operator for installing and managing Keycloak"
            )
        }
    ),
    description =
        """
            A Kubernetes Operator based on the Operator SDK for installing and managing Keycloak.

            Keycloak lets you add authentication to applications and secure services with minimum fuss. No need to deal with storing users or authenticating users. It's all available out of the box.

            The operator can deploy and manage Keycloak instances on Kubernetes and OpenShift.
            The following features are supported:

            * Install Keycloak to a namespace
            * Import Keycloak Realms
            """,
    icon = @CSVMetadata.Icon(
        fileName = "KeycloakController.icon.png",
        mediatype = "image/png"
    ),
    labels = {
        @CSVMetadata.Label(name = "operatorframework.io/arch.amd64", value = "supported"),
        @CSVMetadata.Label(name = "operatorframework.io/arch.arm64", value = "supported"),
        @CSVMetadata.Label(name = "operatorframework.io/arch.ppc64le", value = "supported")
    }
)
public class KeycloakSharedCsvMetadata implements SharedCSVMetadata {
}
