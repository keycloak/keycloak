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

package org.keycloak.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Version {
    public static final String UNKNOWN = "UNKNOWN";
    public static String VERSION;
    public static String RESOURCES_VERSION;
    public static String BUILD_TIME;

    private static String name;

    static {
        try (InputStream is = Version.class.getResourceAsStream("/keycloak-version.properties")) {
            Properties props = new Properties();
            props.load(is);
            Version.VERSION = props.getProperty("version");
            Version.BUILD_TIME = props.getProperty("build-time");
            name = props.getProperty("name");
            Version.RESOURCES_VERSION = Version.VERSION.toLowerCase();

            if (Version.RESOURCES_VERSION.endsWith("-snapshot")) {
                Version.RESOURCES_VERSION = Version.RESOURCES_VERSION.replace("-snapshot", "-" + Version.BUILD_TIME.replace(" ", "").replace(":", "").replace("-", ""));
            }

        } catch (IOException e) {
            Version.VERSION = Version.UNKNOWN;
            Version.BUILD_TIME = Version.UNKNOWN;
            name = "Keycloak";
        }
    }

    public static final String NAME = name;
    public static final String NAME_HTML = "<div class=\"kc-logo-text\"><span>" + name + "</span></div>";

}
