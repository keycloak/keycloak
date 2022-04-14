/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator;

import io.smallrye.config.ConfigMapping;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Optional;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@ConfigMapping(prefix = "operator")
public interface Config {
    Keycloak keycloak();

    interface Keycloak {
        Image image();
        String imagePullPolicy();

        interface Image {
            String name();
            Optional<String> tag();

            default String getFinalTag() {
                String applicationVersion =
                        ConfigProvider
                                .getConfig()
                                .getConfigValue("quarkus.application.version")
                                .getValue();

                if (tag().isPresent()) {
                    return tag().get();
                } else if (applicationVersion != null && applicationVersion.endsWith("SNAPSHOT")) {
                    return Constants.DEFAULT_KEYCLOAK_SNAPSHOT_IMAGE_TAG;
                } else {
                    return applicationVersion;
                }
            }

            default String getFullImage() {
                String tag = getFinalTag();
                if (tag.startsWith("sha")) {
                    return name() + "@" + tag;
                } else {
                    return name() + ":" + tag;
                }
            }
        }
    }
}
