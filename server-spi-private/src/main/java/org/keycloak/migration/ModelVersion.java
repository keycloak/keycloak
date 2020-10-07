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

package org.keycloak.migration;

import org.jboss.logging.Logger;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class ModelVersion {
    private static Logger logger = Logger.getLogger(ModelVersion.class);

    int major;
    int minor;
    int micro;
    String qualifier;

    public ModelVersion(int major, int minor, int micro) {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
    }

    public ModelVersion(String version) {
        version = version.split("-")[0];

        String[] split = version.split("\\.");
        try {
            if (split.length > 0) {
                major = Integer.parseInt(split[0]);
            }
            if (split.length > 1) {
                minor = Integer.parseInt(split[1]);
            }
            if (split.length > 2) {
                micro = Integer.parseInt(split[2]);
            }
            if (split.length > 3) {
                qualifier = split[3];

                if (qualifier.startsWith("redhat")) {
                    qualifier = null;
                }
            }
        } catch (NumberFormatException e) {
            logger.warn("failed to parse version: " + version, e);
        }
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getMicro() {
        return micro;
    }

    public String getQualifier() {
        return qualifier;
    }

    public boolean lessThan(ModelVersion version) {
        if (major < version.major) {
            return true;
        } else if (major > version.major) {
            return false;
        }

        if (minor < version.minor) {
            return true;
        } else if (minor > version.minor) {
            return false;
        }

        if (micro < version.micro) {
            return true;
        } else if (minor > version.minor) {
            return false;
        }

        if (qualifier != null && qualifier.equals(version.qualifier)) return false;
        if (qualifier == null) return false;
        if (version.qualifier == null) return true;
        int comp = qualifier.compareTo(version.qualifier);
        if (comp < 0) {
            return true;
        } else if (comp > 0){
            return false;
        }

        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ModelVersion)) {
            return false;
        }

        ModelVersion v = (ModelVersion) obj;
        return v.getMajor() == major && v.getMinor() == minor && v.getMicro() == micro;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + micro;
    }
}
