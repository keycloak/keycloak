/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.config;

public class ImportOptions {

    public static final Option<String> FILE = new OptionBuilder<>("file", String.class)
            .category(OptionCategory.IMPORT)
            .description("Set the path to a file that will be read.")
            .buildTime(false)
            .build();

    public static final Option<String> DIR = new OptionBuilder<>("dir", String.class)
            .category(OptionCategory.IMPORT)
            .description("Set the path to a directory where files will be read from.")
            .buildTime(false)
            .build();

    public static final Option<Boolean> OVERRIDE = new OptionBuilder<>("override", Boolean.class)
            .category(OptionCategory.IMPORT)
            .defaultValue(Boolean.TRUE)
            .description("Set if existing data should be overwritten. If set to false, data will be ignored.")
            .buildTime(false)
            .build();

}
