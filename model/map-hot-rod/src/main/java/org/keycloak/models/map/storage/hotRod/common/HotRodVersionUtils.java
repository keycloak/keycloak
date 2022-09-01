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

package org.keycloak.models.map.storage.hotRod.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HotRodVersionUtils {

    private static final Pattern schemaVersionPattern = Pattern.compile("schema-version: (\\d+)$", Pattern.MULTILINE);

    /**
     * Decides whether {@code version1} is older than {@code version2}
     *
     * @param version1 first version
     * @param version2 second version
     * @return returns true when {@code version1} is older than {@code version2} and false when versions are equal
     * or {@code version2} is older than {@code version1}
     */
    public static boolean isVersion2NewerThanVersion1(Integer version1, Integer version2) {
        return version1 < version2;
    }

    /**
     * Decides whether {@code version1} and {@code version2} are adjacent values (there are no versions between these)
     *
     * @param version1 first version
     * @param version2 second version
     * @return returns true when {@code version1} and {@code version2} are adjacent, false otherwise
     */
    public static boolean adjacentVersions(Integer version1, Integer version2) {
        return Math.abs(version1 - version2) == 1;
    }

    /**
     * Searches given {@code protoFile} string for occurrences of string schema-version: X, where X is version of current
     * schema in the {@code protoFile} string
     *
     * @param protoFile schema to search
     * @return Integer object representing version of schema within {@code protoFile} or {@code null} if not found
     * @throws IllegalStateException if file contains more than one version definitions
     */
    public static Integer parseSchemaVersionFromProtoFile(String protoFile) {
        Matcher matcher = schemaVersionPattern.matcher(protoFile);

        if (matcher.find()) {
            if (matcher.groupCount() > 1) {
                throw new IllegalStateException("More than one occurrence of schema-version definitions within one proto file " + protoFile);
            }
            return Integer.parseInt(matcher.group(1));
        }

        return null;
    }
}
