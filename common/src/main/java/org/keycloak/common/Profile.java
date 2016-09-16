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

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Profile {

    private enum ProfileValue {
        PRODUCT, PREVIEW, COMMUNITY
    }

    private static ProfileValue value = load();

    static ProfileValue load() {
        String profile = null;
        try {
            profile = System.getProperty("keycloak.profile");
            if (profile == null) {
                String jbossServerConfigDir = System.getProperty("jboss.server.config.dir");
                if (jbossServerConfigDir != null) {
                    File file = new File(jbossServerConfigDir, "profile.properties");
                    if (file.isFile()) {
                        Properties props = new Properties();
                        props.load(new FileInputStream(file));
                        profile = props.getProperty("profile");
                    }
                }
            }
        } catch (Exception e) {
        }

        if (profile == null) {
            return ProfileValue.valueOf(Version.DEFAULT_PROFILE.toUpperCase());
        } else {
            return ProfileValue.valueOf(profile.toUpperCase());
        }
    }

    public static String getName() {
        return value.name().toLowerCase();
    }

    public static boolean isPreviewEnabled() {
        return value.ordinal() >= ProfileValue.PREVIEW.ordinal();
    }

}
