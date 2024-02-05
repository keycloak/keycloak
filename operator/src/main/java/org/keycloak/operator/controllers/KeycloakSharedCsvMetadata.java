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
            "[\n" +
            "  {\n" +
            "    \"apiVersion\": \"k8s.keycloak.org/v2alpha1\",\n" +
            "    \"kind\": \"Keycloak\",\n" +
            "    \"metadata\": {\n" +
            "      \"name\": \"example-keycloak\",\n" +
            "      \"labels\": {\n" +
            "        \"app\": \"sso\"\n" +
            "      }\n" +
            "    },\n" +
            "    \"spec\": {\n" +
            "      \"instances\": 1,\n" +
            "      \"hostname\": \n {" +
            "        \"hostname\": \"example.org\" } \n" +
            "      \"http\":\n {" +
            "        \"tlsSecret\": \"my-tls-secret\" } \n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"apiVersion\": \"k8s.keycloak.org/v2alpha1\",\n" +
            "    \"kind\": \"KeycloakRealmImport\",\n" +
            "    \"metadata\": {\n" +
            "      \"name\": \"example-keycloak-realm-import\",\n" +
            "      \"labels\": {\n" +
            "        \"app\": \"sso\"\n" +
            "      }\n" +
            "    },\n" +
            "    \"spec\": {\n" +
            "      \"keycloakCRName\": \"example-keycloak\",\n" +
            "      \"realm\": {}\n" +
            "    }\n" +
            "  }\n" +
            "]",
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
        "A Kubernetes Operator based on the Operator SDK for installing and managing Keycloak.\n" +
        "\n" +
        "Keycloak lets you add authentication to applications and secure services with minimum fuss. No need to deal with storing users or authenticating users. It's all available out of the box.\n" +
        "\n" +
        "The operator can deploy and manage Keycloak instances on Kubernetes and OpenShift.\n" +
        "The following features are supported:\n" +
        "\n" +
        "* Install Keycloak to a namespace\n" +
        "* Import Keycloak Realms\n",
    icon = @CSVMetadata.Icon(
        fileName = "KeycloakController.icon.png",
        mediatype = "image/png"
    )
)
public class KeycloakSharedCsvMetadata implements SharedCSVMetadata {
}
