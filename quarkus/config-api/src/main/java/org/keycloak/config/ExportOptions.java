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

public class ExportOptions {

    public static final Option<String> FILE = new OptionBuilder<>("file", String.class)
            .category(OptionCategory.EXPORT)
            .description("Set the path to a file that will be created with the exported data. To export more than 50000 users, export to a directory with different files instead.")
            .buildTime(false)
            .build();

    public static final Option<String> DIR = new OptionBuilder<>("dir", String.class)
            .category(OptionCategory.EXPORT)
            .description("Set the path to a directory where files will be created with the exported data.")
            .buildTime(false)
            .build();

    public static final Option<String> REALM = new OptionBuilder<>("realm", String.class)
            .category(OptionCategory.EXPORT)
            .description("Set the name of the realm to export. If not set, all realms are going to be exported.")
            .buildTime(false)
            .build();

    public static final Option<Integer> USERS_PER_FILE = new OptionBuilder<>("users-per-file", Integer.class)
            .category(OptionCategory.EXPORT)
            .defaultValue(50)
            .description("Set the number of users per file. It is used only if 'users' is set to 'different_files'.")
            .buildTime(false)
            .build();

    public static final Option<String> USERS = new OptionBuilder<>("users", String.class)
            .category(OptionCategory.EXPORT)
            .defaultValue("different_files")
            .description("Set how users should be exported.")
            // see UsersExportStrategy
            .expectedValues("skip", "realm_file", "same_file", "different_files")
            .buildTime(false)
            .build();

}
